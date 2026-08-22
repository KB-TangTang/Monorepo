package com.kb.tangtang.challenge.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code tbl_group_challenge_daily_result} 한 행. 그룹 참여자의 하루치 소비 집계다.
 *
 * <p><b>컬럼마다 쓰는 주체가 다르다.</b> 이 구분이 무너지면 판결 결과가 조용히 사라진다.
 * ({@code db/migration/20260814_group_challenge_verdict_deduction.sql})
 *
 * <table>
 *   <tr><th>컬럼</th><th>쓰는 주체</th><th>비고</th></tr>
 *   <tr><td>{@code dailyAmount}</td><td>평가 배치만</td><td>5분마다 UPSERT 로 덮어쓴다</td></tr>
 *   <tr><td>{@code verdictDeductionAmount}</td><td>판결만</td><td>배치가 건드리면 5분마다 0으로 초기화된다</td></tr>
 *   <tr><td>{@code effectiveAmount}</td><td>아무도 안 씀</td><td>생성 컬럼. 조회는 전부 이 값을 본다</td></tr>
 * </table>
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupChallengeDailyResult {

    private Long id;
    private Long groupId;
    private Long userId;
    private LocalDate challengeDate;

    /** 집계 원본. 평가 배치 전용이다. 판결 감액을 여기에 반영하지 않는다. */
    private BigDecimal dailyAmount;

    /** 무죄로 인정된 감액 누계. 판결 전용이다. 배치의 UPSERT UPDATE 절에 절대 넣지 않는다. */
    private BigDecimal verdictDeductionAmount;

    /**
     * 실효 소비액 = {@code GREATEST(dailyAmount - verdictDeductionAmount, 0)}.
     *
     * <p><b>DB 생성 컬럼(STORED)이라 쓰기가 불가능하다.</b> 여기에 값을 채워 INSERT·UPDATE 하려 하면
     * MySQL 이 거부한다. 일일 기소 판정과 랭킹이 보는 값이 이것이다.
     */
    private BigDecimal effectiveAmount;

    /** 미션 해당 타겟의 소비 '횟수'. 금액이 아니다. */
    private Integer targetCount;
}
