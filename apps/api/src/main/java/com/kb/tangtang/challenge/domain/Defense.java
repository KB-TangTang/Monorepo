package com.kb.tangtang.challenge.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 변론 한 건 ({@code tbl_defense}, 이슈 #170).
 *
 * <p>기소 1건당 변론 1건이다({@code uk_def_indictment}). 수정·삭제가 없어 INSERT 와 조회만 있다.
 *
 * <p><b>컬럼명 주의</b> — {@code db/schema.sql} 에는 아직 옛 이름
 * ({@code requested_adjustment_amount})이 남아 있다. 실행 기준은
 * {@code db/migration/20260814_group_challenge_verdict_deduction.sql} 이고
 * 현재 컬럼은 {@code actual_burden_amount} · {@code deduction_amount} 다.
 */
@Getter
@Setter
public class Defense {

    private Long id;

    private Long indictmentId;

    private String content;

    /**
     * 피고가 신고한 실제 개인 부담금. 결산 구간 거래를 건별로 입력받아 합계만 저장한다
     * (거래-변론 연결 테이블을 만들지 않았다 — 이슈 #170 결정).
     */
    private BigDecimal actualBurdenAmount;

    /**
     * 무죄 확정 시 소비액에서 빼 줄 금액. <b>서버가 계산한다</b> —
     * {@code 결산 구간 소비액 - actualBurdenAmount}. 클라이언트 값을 믿지 않는다.
     *
     * <p>실제로 {@code tbl_group_challenge_daily_result.verdict_deduction_amount} 에 옮겨 적는 주체는
     * 개표·확정(이슈 #172)이다. 여기서는 기록만 해 둔다.
     */
    private BigDecimal deductionAmount;

    private LocalDateTime createdAt;
}
