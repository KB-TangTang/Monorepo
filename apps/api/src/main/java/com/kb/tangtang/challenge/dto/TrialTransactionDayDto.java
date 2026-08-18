package com.kb.tangtang.challenge.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 변론 화면 거래 목록의 하루 묶음 (이슈 #170).
 *
 * <p>일일결산은 이 묶음이 <b>하나</b>고, 기간결산은 거래가 있는 날만큼 여러 개다.
 * 거래가 없는 날은 내려보내지 않는다 — 화면이 빈 날 헤더를 그리지 않는다.
 */
@Getter
@Builder
public class TrialTransactionDayDto {

    private LocalDate date;

    /**
     * 그 날 소비 합계. {@code transactions[].amount} 의 합과 같다(환불 음수 포함).
     *
     * <p><b>무죄 감액은 빠져 있지 않다.</b> 감액은 구간 총액
     * ({@link TrialTransactionsDto#getCurrentAmount()})에만 적용된다 — 어느 날에서 깎을지가
     * 정해지지 않은 값이라 날짜별로 배분할 수 없다.
     */
    private BigDecimal dailyAmount;

    private List<TrialTransactionDto> transactions;
}
