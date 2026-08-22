package com.kb.tangtang.challenge.domain;

/**
 * 그룹 챌린지 최종 판정. {@code tbl_group_member.final_outcome} 의 CHECK 제약(ck_gm_outcome)과
 * 1:1 대응한다 (이슈 #172).
 *
 * <p><b>NULL 이 「미확정」이다.</b> 진행 중에는 값을 채우지 않는다 — 목숨이 0이 된 순간에 적어
 * 두면 화면이 「탈락」을 먼저 말하고, 그 뒤 마지막 재판이 무죄로 끝나 감액이 반영되면 되돌릴 수
 * 없다. 확정은 {@code JUDGING → CLOSED} 배치 한 곳에서만 일어난다.
 */
public enum FinalOutcome {

    /** 완주. 일일평가는 목숨이 남았고, 기간평가는 누적 소비가 한도 이내다. */
    SURVIVED,

    /** 탈락. 순위는 생존자 전원 뒤의 공동 최하위다. */
    ELIMINATED
}
