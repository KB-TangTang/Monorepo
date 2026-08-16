package com.kb.tangtang.challenge.domain;

/**
 * 그룹 챌린지 상태. {@code tbl_challenge_group.status} 의 CHECK 제약(ck_cg_status)과 1:1 대응한다.
 *
 * RECRUITING → ACTIVE 전이는 시작일 배치가 담당한다(별도 이슈). 이 이슈에서는 전이시키지 않는다.
 */
public enum ChallengeGroupStatus {

    /** 모집 중. 시작일 당일 23:59 까지 참여할 수 있다. */
    RECRUITING,

    /** 진행 중. */
    ACTIVE,

    /** 종료 후 최종 판정 중. */
    JUDGING,

    /**
     * 종료. <b>「정상 종료 + 최종 결과 확정 완료」</b>를 뜻한다 — 그룹 전적·월간 리포트가 이 상태를
     * 「{@code final_*} 가 채워져 있다」는 계약으로 읽는다. 모집 미달로 시작조차 못 한 그룹은
     * 여기로 오지 않고 삭제된다(이슈 #261).
     */
    CLOSED
}
