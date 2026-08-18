package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

/**
 * 목록 카드의 재판 배지 한 그룹치 (이슈 #169).
 *
 * <p>그룹 하나씩 세면 카드 수만큼 쿼리가 나간다. 그래서 참여 중인 그룹 전체를 한 번에 세고
 * 서비스가 {@code groupId} 로 붙인다 ({@code GroupMemberMapper#findByGroupIds} 와 같은 방식).
 */
@Getter
@Setter
public class GroupTrialSummaryRow {

    private Long groupId;

    /**
     * 내가 피고이고 아직 변론을 안 낸 기소 수. 0 보다 크면 카드가 「변론필요」가 된다.
     *
     * <p>변론을 이미 낸 기소는 세지 않는다 — 배지는 상태가 아니라 <b>내가 지금 해야 할 일</b>을
     * 가리킨다({@code GroupActiveCard.vue} 의 주석). 변론을 낸 뒤에는 기다리는 것 말고 할 일이 없다.
     */
    private int myDefenseNeededCount;

    /** 남이 피고이고 투표 중인데 내가 아직 안 던진 기소 수. 그대로 {@code pendingTrialCount} 다. */
    private int pendingVoteCount;

    /** 투표 중인 기소 중 내가 이미 던진 것. {@code PENDING} 과 {@code DONE} 을 가르는 값이다. */
    private int castVoteCount;
}
