package com.kb.tangtang.transaction.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** yearMonth 를 지정했을 때만 채워진다. 검색용 전체 조회(yearMonth 생략)는 summary=null. */
@Getter
@Builder
public class LedgerSummaryDto {

    /** 프론트 LedgerSummaryCard 가 summary.period 로 "N월 지출" 라벨을 그려서 여기도 채운다(응답 최상위 period 와 중복). */
    private String period;
    private BigDecimal totalSpent;
    private BigDecimal totalDeposit;
    /** 전달 지출이 0이면 0. 데이터가 있는 가장 이른 달 이전이라 비교 대상이 없어도 0. */
    private BigDecimal monthOverMonthRate;
    private List<String> paymentMethods;
}
