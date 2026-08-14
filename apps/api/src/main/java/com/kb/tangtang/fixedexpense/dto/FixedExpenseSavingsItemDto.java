package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class FixedExpenseSavingsItemDto {

    private long id;
    private String categoryCode;
    private String categoryLabel;
    private String title;
    private String description;
    private BigDecimal savingsAmount;
}
