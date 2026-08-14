package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MonthlyCategoryItemDto {

    private Long parentCategoryId;
    private String parentCategoryName;
    private Long categoryId;
    private String categoryName;
    private BigDecimal amount;
    private BigDecimal ratio;
    private BigDecimal previousMonthAmount;
    private BigDecimal changeRate;
}
