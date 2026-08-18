package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.GroupTrialEvents;
import com.kb.tangtang.challenge.domain.VerdictDecision;
import com.kb.tangtang.challenge.domain.VerdictTallyRow;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import com.kb.tangtang.challenge.mapper.IndictmentMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * 판결 확정 한 건 (이슈 #172). {@code VOTING → GUILTY / INNOCENT} 와 그 뒤처리.
 *
 * <p><b>배치 본체와 클래스를 나눈 이유</b>는 트랜잭션을 기소 하나마다 끊기 위해서다. 한 클래스
 * 안에서 부르면 자기호출이라 프록시를 타지 않아 {@code @Transactional} 이 걸리지 않고, 배치
 * 전체가 한 트랜잭션이 되면 마지막 재판에서 난 오류가 앞서 확정된 판결까지 되돌린다 —
 * 채팅·알림은 이미 나간 뒤라 DB 와 알림함이 어긋난다.
 * ({@link ChallengeGroupStatusTransitionService} 와 같은 구조)
 *
 * <p><b>멱등성은 {@code confirmVerdict} 의 반환값이 보장한다.</b> {@code WHERE ... AND
 * status = 'VOTING'} 이라 이미 확정된 재판은 0행을 바꾸고 그 자리에서 멈춘다. 이 반환값을 보지
 * 않고 뒤처리로 넘어가면 배치를 두 번 돌릴 때 <b>목숨이 두 번 깎이고</b> 감액이 두 번 더해진다.
 *
 * <p>뒤처리는 판결에 따라 하나씩만 일어난다.
 * <pre>
 *   GUILTY   → 피고의 목숨 1개 차감 (lives_count &gt; 0 일 때만. 음수 방어는 SQL 이 한다)
 *   INNOCENT → 변론에서 인정된 감액을 결산 행에 누적 (변론이 없으면 아무것도 하지 않는다)
 * </pre>
 */
@Service
@Log4j2
public class GroupVerdictTransitionService {

    private final IndictmentMapper indictmentMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final GroupChallengeResultMapper resultMapper;
    private final ApplicationEventPublisher events;

    public GroupVerdictTransitionService(IndictmentMapper indictmentMapper,
                                         GroupMemberMapper groupMemberMapper,
                                         GroupChallengeResultMapper resultMapper,
                                         ApplicationEventPublisher events) {
        this.indictmentMapper = indictmentMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.resultMapper = resultMapper;
        this.events = events;
    }

    /**
     * 기소 한 건의 판결을 확정하고 뒤처리한다.
     *
     * @param decision AI 위임({@link VerdictDecision#needsAi()})이 아닌, 이미 결론이 난 판결
     * @return 실제로 확정했으면 {@code true}. 이미 다른 실행이 확정했으면 {@code false}
     */
    @Transactional
    public boolean confirm(VerdictTallyRow row, VerdictDecision decision) {
        if (decision.needsAi()) {
            /* 배치가 AI 호출을 마치고 결론이 난 판결만 넘겨야 한다. 여기까지 오면 호출부 버그다. */
            throw new IllegalArgumentException(
                    "AI 위임 상태로는 확정할 수 없습니다: indictmentId=" + row.getIndictmentId());
        }

        int changed = indictmentMapper.confirmVerdict(
                row.getIndictmentId(),
                decision.getStatus().name(),
                decision.getResult(),
                decision.getMethod().name(),
                decision.getAiReason());
        if (changed == 0) {
            /* 조회한 뒤 다른 실행이 먼저 확정했다. 여기서 멈추지 않으면 목숨이 두 번 깎인다. */
            log.info("판결 확정 건너뜀 indictmentId={} — 이미 VOTING 이 아니다", row.getIndictmentId());
            return false;
        }

        String summary = decision.isGuilty() ? applyGuilty(row) : applyInnocent(row);
        events.publishEvent(new GroupTrialEvents.VerdictConfirmed(
                row.getGroupId(), row.getIndictmentId(), summary));

        log.info("판결 확정 indictmentId={} groupId={} userId={} {} / {}",
                row.getIndictmentId(), row.getGroupId(), row.getUserId(),
                decision.getStatus(), decision.getMethod());
        return true;
    }

    /**
     * 유죄 — 목숨 1개 차감.
     *
     * <p>이미 0목숨이면 0행이 바뀐다. 오류가 아니다 — 남은 목숨이 없어도 판결 자체는 성립하고,
     * 탈락 판정은 종료 후 확정 배치가 {@code lives_count = 0} 으로 따로 내린다.
     * 대신 문구를 갈라 「차감됐어요」라고 거짓말하지 않는다.
     */
    private String applyGuilty(VerdictTallyRow row) {
        int decreased = groupMemberMapper.decreaseLife(row.getGroupId(), row.getUserId());
        if (decreased == 0) {
            log.info("목숨 차감 없음 groupId={} userId={} — 남은 목숨이 0", row.getGroupId(), row.getUserId());
            return nickname(row) + "님 재판 — 유죄. 남은 목숨이 없어요.";
        }
        return nickname(row) + "님 재판 — 유죄. 목숨 1개가 차감됐어요.";
    }

    /**
     * 무죄 — 변론에서 인정된 감액을 결산 행에 누적한다.
     *
     * <p>감액이 없는 경우가 둘이다. 변론을 내지 않은 채 마감돼 투표로 넘어간 재판
     * ({@code deductionAmount == null})과, 변론은 냈지만 대납·공동부담이 없어 감액이 0인 재판이다.
     * 둘 다 UPDATE 를 건너뛴다 — 0을 더하는 UPDATE 는 결과가 같지만 결산 행을 건드린 흔적만 남는다.
     */
    private String applyInnocent(VerdictTallyRow row) {
        BigDecimal deduction = row.getDeductionAmount();
        if (deduction == null || deduction.signum() <= 0) {
            return nickname(row) + "님 재판 — 무죄.";
        }
        resultMapper.addVerdictDeduction(row.getResultId(), deduction);
        return nickname(row) + "님 재판 — 무죄. " + money(deduction) + "이 소비액에서 빠졌어요.";
    }

    /** 닉네임은 NULL 허용이다. 조회 SQL 이 social_name 까지 떨어뜨려도 비어 있을 수 있다. */
    private String nickname(VerdictTallyRow row) {
        String nickname = row.getNickname();
        return nickname == null || nickname.isBlank() ? "참여자" : nickname;
    }

    /** 원 단위 미만은 버린다. 표시용이고 계산은 이미 DB 에서 끝났다. */
    private String money(BigDecimal amount) {
        return NumberFormat.getIntegerInstance(Locale.KOREA).format(amount) + "원";
    }
}
