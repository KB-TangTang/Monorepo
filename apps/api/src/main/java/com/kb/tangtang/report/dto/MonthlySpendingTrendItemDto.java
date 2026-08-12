package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MonthlySpendingTrendItemDto {

    private String yearMonth;
    private BigDecimal amount;
    private boolean hasData;
}
