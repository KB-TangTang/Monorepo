package com.kb.tangtang.report.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class MonthlyAiAnalysisInput {

    private String yearMonth;
    private BigDecimal currentMonthSpent;
    private BigDecimal previousMonthSpent;
    private boolean hasPreviousComparison;
    private BigDecimal savingsAmount;
    private List<MonthlyAiAnalysisCategory> categories;
}
