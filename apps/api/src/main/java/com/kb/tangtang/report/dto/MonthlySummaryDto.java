package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MonthlySummaryDto {

    private String yearMonth;
    private BigDecimal totalSpent;
    private BigDecimal previousMonthSpent;
    private boolean hasPreviousComparison;
    private BigDecimal monthOverMonthRate;
    private int fixedExpenseCandidateCount;
}
