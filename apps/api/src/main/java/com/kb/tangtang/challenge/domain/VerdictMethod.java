package com.kb.tangtang.challenge.domain;

/**
 * 판결이 어떤 경로로 확정됐는지. {@code tbl_indictment.verdict_method} 의
 * CHECK 제약(ck_ind_verdict_method)과 1:1 대응한다.
 *
 * <p>평가·기소 배치(#168)는 이 값을 쓰지 않는다 — 기소는 DEFENSE_WAIT 로만 만들어지고
 * verdict_method 는 NULL 로 남는다. 판결 확정(#171 · #172)이 채운다.
 * 그럼에도 여기서 정의해 두는 이유는 값의 출처를 한 곳으로 모으기 위함이다.
 *
 * <p>⚠ {@code SCRATCH_LOTTERY}(동률 시 스크래치 복권)는 폐기된 값이다.
 * "재판인데 결과가 운으로 갈리면 판결에 근거가 없다"는 0808 멘토링 지적에 따라
 * {@link #AI_JUDGMENT} 로 교체됐다.
 * ({@code db/migration/20260809_ai_verdict_and_mission_pool_check.sql})
 * 지금 DB CHECK 는 SCRATCH_LOTTERY 를 거부하므로 넣으면 INSERT 가 실패한다.
 */
public enum VerdictMethod {

    /** 배심원 다수결. */
    VOTE,

    /** 투표가 0표라 무죄로 추정. */
    NO_VOTE,

    /**
     * 투표 동률이라 판사 탕이가 AI 로 판결.
     * 이 값일 때만 {@code ai_verdict_reason} 에 사유를 남길 수 있다(ck_ind_ai_reason).
     */
    AI_JUDGMENT,

    /** 피고가 스스로 혐의를 인정. 투표 없이 유죄로 확정된다. */
    CONFESSION
}
