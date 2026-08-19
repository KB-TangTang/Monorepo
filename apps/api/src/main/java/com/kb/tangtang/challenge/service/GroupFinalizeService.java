package com.kb.tangtang.challenge.service;

import com.kb.tangtang.challenge.domain.ChallengeGroup;
import com.kb.tangtang.challenge.domain.ChallengeGroupStatus;
import com.kb.tangtang.challenge.domain.EvalType;
import com.kb.tangtang.challenge.domain.FinalOutcome;
import com.kb.tangtang.challenge.domain.GroupChallengeEvents;
import com.kb.tangtang.challenge.domain.GroupFinalizeRow;
import com.kb.tangtang.challenge.mapper.ChallengeGroupMapper;
import com.kb.tangtang.challenge.mapper.GroupChallengeResultMapper;
import com.kb.tangtang.challenge.mapper.GroupMemberMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 그룹 챌린지 하나의 최종 결과 확정 (이슈 #172). {@code JUDGING → CLOSED}.
 *
 * <p><b>배치 본체와 클래스를 나눈 이유</b>는 트랜잭션을 그룹 하나마다 끊기 위해서다. 한 클래스
 * 안에서 부르면 자기호출이라 프록시를 타지 않아 {@code @Transactional} 이 걸리지 않는다.
 * ({@link GroupVerdictTransitionService} · {@link ChallengeGroupStatusTransitionService} 와 같은 구조)
 *
 * <p><b>확정은 여기 한 곳에서만 일어난다.</b> 목숨이 0이 된 순간에 {@code final_outcome} 을 적어
 * 두면 화면이 「탈락」을 먼저 말하고, 그 뒤 마지막 재판이 무죄로 끝나 감액이 반영돼도
 * {@code final_*} 3개는 write-once 라 되돌릴 수 없다. 진행 중에는 NULL 로 둔다.
 *
 * <p>탈락 판정은 평가 주기로 갈린다.
 * <pre>
 *   DAILY  → lives_count = 0            (재판에서 유죄를 받은 횟수가 기간 일수만큼 쌓였다)
 *   PERIOD → 기간 실효 소비액 &gt; 한도    (목숨은 1로 고정이라 보지 않는다)
 * </pre>
 */
@Service
@Log4j2
public class GroupFinalizeService {

    private final ChallengeGroupMapper challengeGroupMapper;
    private final GroupChallengeResultMapper resultMapper;
    private final GroupMemberMapper groupMemberMapper;
    private final ApplicationEventPublisher events;

    public GroupFinalizeService(ChallengeGroupMapper challengeGroupMapper,
                                GroupChallengeResultMapper resultMapper,
                                GroupMemberMapper groupMemberMapper,
                                ApplicationEventPublisher events) {
        this.challengeGroupMapper = challengeGroupMapper;
        this.resultMapper = resultMapper;
        this.groupMemberMapper = groupMemberMapper;
        this.events = events;
    }

    /**
     * 그룹 하나의 최종 결과를 확정한다.
     *
     * <p>참여자 쓰기를 먼저 하고 상태 전이를 뒤에 두는 순서다. 반대로 두면 다른 실행이
     * {@code CLOSED} 를 먼저 잡았을 때 이쪽은 아무것도 안 한 채 빠지는데, 그 실행이 참여자
     * 쓰기를 마쳤다는 보장이 없다. 참여자 쓰기는 {@code final_outcome IS NULL} 로 write-once 라
     * 두 실행이 겹쳐도 값이 뒤집히지 않는다.
     *
     * @return 실제로 확정했으면 {@code true}. 이미 다른 실행이 확정했으면 {@code false}
     */
    @Transactional
    public boolean finalizeGroup(ChallengeGroup group) {
        List<GroupFinalizeRow> rows = resultMapper.findFinalConsumption(group.getId());

        List<GroupFinalizeRow> survivors = new ArrayList<>();
        List<GroupFinalizeRow> eliminated = new ArrayList<>();
        for (GroupFinalizeRow row : rows) {
            (isEliminated(group, row) ? eliminated : survivors).add(row);
        }

        writeSurvivors(group.getId(), survivors);
        writeEliminated(group.getId(), eliminated, survivors.size() + 1);

        int changed = challengeGroupMapper.updateStatusIfCurrent(
                group.getId(),
                ChallengeGroupStatus.JUDGING.name(),
                ChallengeGroupStatus.CLOSED.name());
        if (changed == 0) {
            /* 조회한 뒤 다른 실행이 먼저 확정했다. 여기서 발행하면 같은 알림·전적 갱신이 두 번 돈다 */
            log.info("최종 확정 건너뜀 groupId={} — 이미 JUDGING 이 아니다", group.getId());
            return false;
        }

        events.publishEvent(new GroupChallengeEvents.GroupChallengeClosed(
                group.getId(), group.getEndDate(), memberUserIds(rows)));

        log.info("그룹 챌린지 최종 확정 groupId={} JUDGING → CLOSED 완주={} 탈락={}",
                group.getId(), survivors.size(), eliminated.size());
        return true;
    }

    /**
     * 탈락 판정.
     *
     * <p>일일평가는 목숨만 본다. 소비액을 다시 보지 않는 이유는 하루하루의 초과가 이미 기소 →
     * 재판을 거쳐 목숨으로 환산됐기 때문이다 — 여기서 다시 합계로 판정하면 무죄로 끝난 날까지
     * 탈락 사유로 되살아난다.
     *
     * <p>기간평가는 반대로 목숨을 보지 않는다. {@code lives_count} 가 1로 고정이라
     * 유죄 한 번이면 0이 되는데, 그건 「한도를 넘겼다」의 다른 표현일 뿐이고 무죄로 끝났거나
     * 아직 재판이 없는 그룹까지 같은 기준으로 판정하려면 소비액을 봐야 한다.
     */
    private boolean isEliminated(ChallengeGroup group, GroupFinalizeRow row) {
        if (EvalType.PERIOD.name().equals(group.getEvalType())) {
            return row.getChargeAmount()
                    .compareTo(BigDecimal.valueOf(group.getLimitAmount())) > 0;
        }
        return row.getLivesCount() != null && row.getLivesCount() <= 0;
    }

    /**
     * 생존자 — 실효 소비액 오름차순으로 등수를 매긴다. 적게 쓴 사람이 1위다.
     *
     * <p>같은 금액이면 같은 등수를 준다(공동 순위). 뒤 사람의 등수는 그만큼 건너뛴다 —
     * 1·1·3 이지 1·1·2 가 아니다. 금액이 같은데 {@code user_id} 로 앞뒤를 가르면
     * 화면상 근거 없는 차이가 생긴다.
     */
    private void writeSurvivors(Long groupId, List<GroupFinalizeRow> survivors) {
        survivors.sort(Comparator.comparing(GroupFinalizeRow::getChargeAmount));

        int rank = 0;
        BigDecimal previous = null;
        for (int i = 0; i < survivors.size(); i++) {
            GroupFinalizeRow row = survivors.get(i);
            if (previous == null || row.getChargeAmount().compareTo(previous) != 0) {
                rank = i + 1;
                previous = row.getChargeAmount();
            }
            write(groupId, row, FinalOutcome.SURVIVED, rank);
        }
    }

    /**
     * 탈락자 — 전원 공동 최하위({@code 생존자 수 + 1}). {@code schema.sql} 의 컬럼 주석 그대로다.
     *
     * <p>탈락자끼리 소비액으로 순위를 가르지 않는다. 탈락은 「완주 실패」 하나로 같은 결과고,
     * 덜 쓴 탈락자가 더 나은 등수를 받으면 완주의 의미가 흐려진다.
     * 전원이 탈락한 그룹은 모두 1위가 되는데, 생존자가 없으니 그 자리가 비어 있는 것이 맞다.
     */
    private void writeEliminated(Long groupId, List<GroupFinalizeRow> eliminated, int rank) {
        for (GroupFinalizeRow row : eliminated) {
            write(groupId, row, FinalOutcome.ELIMINATED, rank);
        }
    }

    private void write(Long groupId, GroupFinalizeRow row, FinalOutcome outcome, int rank) {
        int changed = groupMemberMapper.finalizeMember(
                groupId, row.getUserId(), outcome.name(), rank, row.getChargeAmount());
        if (changed == 0) {
            /* write-once 가 막았다. 이미 확정된 참여자라 값이 보존된다 */
            log.info("참여자 확정 건너뜀 groupId={} userId={} — 이미 확정됨", groupId, row.getUserId());
        }
    }

    private List<Long> memberUserIds(List<GroupFinalizeRow> rows) {
        List<Long> userIds = new ArrayList<>(rows.size());
        for (GroupFinalizeRow row : rows) {
            userIds.add(row.getUserId());
        }
        return userIds;
    }
}
