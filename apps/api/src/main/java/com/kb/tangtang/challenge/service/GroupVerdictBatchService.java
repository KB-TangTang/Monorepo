package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.client.TangiVerdictClient;
import com.kb.tangtang.challenge.domain.IndictmentStatus;
import com.kb.tangtang.challenge.domain.VerdictDecision;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 개표 배치 (이슈 #172). {@code VOTING} 인 기소를 모아 판결을 확정한다.
 *
 * <p>대상은 「투표 마감이 지났거나, 투표할 사람이 전부 던졌거나」 둘 중 하나다. 마감만 보면
 * 전원이 일찍 투표를 끝낸 재판도 30시간을 채워야 결과가 나오고, 그 사이 화면은 「투표 완료」인
 * 채로 멈춰 있어 사용자는 서비스가 멈춘 줄 안다(팀 결정 2026-08-18).
 *
 * <p><b>여기에 {@code @Transactional} 을 붙이지 않는다.</b> 이유가 둘이다.
 * <ol>
 *   <li>동률이면 판사 탕이(LLM)를 HTTP 로 부른다. 트랜잭션 안에서 부르면 응답을 기다리는 동안
 *       DB 커넥션과 행 잠금을 잡고 있게 된다 — 배치 하나가 커넥션 풀을 말릴 수 있다</li>
 *   <li>재판 하나의 실패가 나머지를 막으면 안 된다. 트랜잭션 경계는
 *       {@link GroupVerdictTransitionService} 가 기소 단위로 잡는다</li>
 * </ol>
 * ({@link ChallengeGroupStatusBatchService} 와 같은 형태)
 *
 * <p>멱등성은 {@code confirmVerdict} 의 {@code WHERE ... AND status = 'VOTING'} 이 보장한다.
 * 몇 번을 돌려도 목숨은 한 번만 깎인다.
 */
@Service
@Log4j2
public class GroupVerdictBatchService {

    /**
     * 판사 탕이를 못 불렀을 때의 사유. 무죄를 기본값으로 두는 이유는 0표 무죄 추정
     * ({@code NO_VOTE})과 일관되기 때문이다(요구사항정의서 6.5).
     *
     * <p><b>재시도하지 않는다.</b> 다음 틱이 다시 집게 두면 같은 재판이 LLM 을 계속 두드리고,
     * 그동안 재판은 끝나지 않은 채 남는다 — 「끝나지 않는 재판」이 잘못된 무죄보다 나쁘다.
     */
    static final String AI_FAILURE_REASON =
            "판사 탕이가 판결하지 못해, 무죄 추정 원칙에 따라 무죄로 확정했어요.";

    private final IndictmentMapper indictmentMapper;
    private final GroupVerdictTransitionService transitionService;
    private final TangiVerdictClient tangiVerdictClient;
    private final int defenseHours;
    private final int voteHours;

    public GroupVerdictBatchService(IndictmentMapper indictmentMapper,
                                    GroupVerdictTransitionService transitionService,
                                    TangiVerdictClient tangiVerdictClient,
                                    @Value("${challenge.trial.defense-hours}") int defenseHours,
                                    @Value("${challenge.trial.vote-hours}") int voteHours) {
        this.indictmentMapper = indictmentMapper;
        this.transitionService = transitionService;
        this.tangiVerdictClient = tangiVerdictClient;
        this.defenseHours = defenseHours;
        this.voteHours = voteHours;
    }

    /**
     * 개표할 차례가 된 재판을 전부 확정한다.
     *
     * @return 실제로 확정된 재판 수. 이미 다른 실행이 확정한 건은 세지 않는다
     */
    public int confirmDueVerdicts() {
        List<VerdictTallyRow> due = indictmentMapper.findVotingToTally(defenseHours, voteHours);
        if (due.isEmpty()) {
            return 0;
        }

        int confirmed = 0;
        for (VerdictTallyRow row : due) {
            try {
                VerdictDecision decision = VerdictDecider.decide(row);
                if (decision.needsAi()) {
                    decision = judgeByAi(row);
                }
                if (transitionService.confirm(row, decision)) {
                    confirmed++;
                }
            } catch (RuntimeException e) {
                /* 한 재판의 실패가 나머지를 막지 않는다. 다음 틱이 다시 집어 간다 */
                log.error("판결 확정 실패 indictmentId={}", row.getIndictmentId(), e);
            }
        }
        log.info("개표 배치 완료 대상={} 확정={}", due.size(), confirmed);
        return confirmed;
    }

    /**
     * 동률 — 판사 탕이에게 넘긴다.
     *
     * <p>어떤 실패든 무죄로 떨어뜨린다. 여기서 예외를 다시 던지면 이 재판은 다음 틱에 또 집히고,
     * 원인이 만료된 API 키 같은 것이라면 <b>영원히 확정되지 않는다.</b>
     */
    private VerdictDecision judgeByAi(VerdictTallyRow row) {
        try {
            VerdictDecision decision = tangiVerdictClient.judge(row);
            if (decision == null || decision.needsAi()) {
                throw new IllegalStateException("판사 탕이가 결론을 내지 못했다");
            }
            return decision;
        } catch (RuntimeException e) {
            log.error("판사 탕이 판결 실패 indictmentId={} — 무죄 추정으로 확정한다",
                    row.getIndictmentId(), e);
            return VerdictDecision.byAi(IndictmentStatus.INNOCENT, AI_FAILURE_REASON);
        }
    }
}
