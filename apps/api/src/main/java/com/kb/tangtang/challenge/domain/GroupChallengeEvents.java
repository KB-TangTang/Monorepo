package com.kb.tangtang.challenge.domain;

import java.time.LocalDate;
import java.util.List;

/**
 * 그룹 챌린지 자체의 생애 이벤트 (이슈 #172). 재판 진행 이벤트는 {@link GroupTrialEvents} 다.
 *
 * <p>모듈 경계를 넘는 통로다. 최종 확정은 {@code challenge} 가 하지만 그 결과를 반영해야 하는
 * 쪽은 {@code report} 라, Service 를 직접 부르지 않고 이벤트로 알린다
 * ({@code apps/api/AGENTS.md} 「모듈 경계」).
 */
public final class GroupChallengeEvents {

    private GroupChallengeEvents() {
    }

    /**
     * 그룹 챌린지의 최종 결과가 확정됐다 ({@code JUDGING → CLOSED}).
     *
     * <p><b>{@code endDate} 를 함께 싣는 이유가 이 이벤트의 핵심이다.</b> 수신자는 이 결과를
     * 「어느 달의 전적인가」로 적어야 하는데, 그 달은 <b>수신 시각이 아니라 챌린지가 끝난 달</b>이다.
     * 말일에 끝난 그룹은 투표 마감(기소 + 30시간)이 다음 달로 넘어가는 것이 정상 경로라,
     * 수신 시각에서 월을 뽑으면 전적이 엉뚱한 달에 붙거나 영영 채워지지 않는다.
     *
     * <p>{@code memberUserIds} 는 탈락자를 포함한 참여자 전원이다. 전적은 승패를 모두 센다.
     */
    public static final class GroupChallengeClosed {
        private final long groupId;
        private final LocalDate endDate;
        private final List<Long> memberUserIds;

        public GroupChallengeClosed(long groupId, LocalDate endDate, List<Long> memberUserIds) {
            this.groupId = groupId;
            this.endDate = endDate;
            /* 발행 뒤 원본 목록이 바뀌어도 @Async 리스너가 다른 값을 보지 않게 복사해 잠근다 */
            this.memberUserIds = memberUserIds == null ? List.of() : List.copyOf(memberUserIds);
        }

        public long getGroupId() { return groupId; }
        public LocalDate getEndDate() { return endDate; }
        public List<Long> getMemberUserIds() { return memberUserIds; }
    }
}
