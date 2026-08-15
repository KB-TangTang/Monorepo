package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class FixedExpenseSavingsReportDto {

    private String yearMonth;
    private BigDecimal monthlySavings;
    private BigDecimal yearlySavings;
    private List<FixedExpenseSavingsItemDto> items;
}
