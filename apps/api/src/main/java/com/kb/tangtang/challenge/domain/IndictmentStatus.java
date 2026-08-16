package com.kb.tangtang.challenge.domain;

/**
 * 기소 상태. {@code tbl_indictment.status} 의 CHECK 제약(ck_ind_status)과 1:1 대응한다.
 *
 * <p><b>단방향이다.</b> 판결이 확정된 뒤에는 재심·재투표가 없다(팀 결정 A안).
 * 무죄로 한도 이하가 되어도 기소를 지우거나 되돌리지 않는다 — 평가 배치가 그 사실을 근거로
 * "이미 기소된 건" 을 걸러 내기 때문에, 지우면 다음 틱이 같은 위반을 다시 기소한다.
 *
 * <p>{@code result} 컬럼과 짝이 맞아야 한다(ck_ind_result).
 * GUILTY=1 · INNOCENT=0 · DEFENSE_WAIT/VOTING=NULL.
 */
public enum IndictmentStatus {

    /** 기소 직후. 피고의 변론을 기다린다. 평가 배치(#168)가 만드는 것은 항상 이 상태다. */
    DEFENSE_WAIT,

    /** 변론이 끝나 배심원 투표를 받는 중. */
    VOTING,

    /** 유죄 확정. */
    GUILTY,

    /** 무죄 확정. 이때만 변론의 감액분이 일별 결과에 반영된다. */
    INNOCENT
}
