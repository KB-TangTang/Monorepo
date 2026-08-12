package com.kb.tangtang.report.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyCategorySpendingRow {

    private String yearMonth;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
}
