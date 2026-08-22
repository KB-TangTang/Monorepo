package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class FixedExpenseOverviewSummaryDto {

    private BigDecimal expectedMonthlyAmount;
    private int confirmedCount;
    private int candidateCount;
}
