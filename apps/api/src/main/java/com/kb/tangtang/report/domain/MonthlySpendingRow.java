package com.kb.tangtang.report.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySpendingRow {

    private String yearMonth;
    private BigDecimal amount;
}
