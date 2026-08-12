package com.kb.tangtang.challenge.domain;

/**
 * 그룹 챌린지 평가 주기. {@code tbl_challenge_group.eval_type} 의 CHECK 제약(ck_cg_eval)과 1:1 대응한다.
 *
 * 목숨 개수는 평가 주기가 단독으로 결정한다 (요구사항정의서 v3.11 · 6.5).
 */
public enum EvalType {

    /** 일일 평가 — 하루 단위로 제한 금액을 판정한다. 목숨 = 챌린지 기간 일수. */
    DAILY,

    /** 기간 평가 — 종료 다음 날 누적액으로 한 번 판정한다. 목숨 = 1 고정. */
    PERIOD;

    /**
     * 참여 시점에 부여할 목숨 개수.
     *
     * @param totalDays 챌린지 기간 일수 (시작일·종료일 포함)
     */
    public int livesFor(int totalDays) {
        return this == DAILY ? totalDays : 1;
    }
}
