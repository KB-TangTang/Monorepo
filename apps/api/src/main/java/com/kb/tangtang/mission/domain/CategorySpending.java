package com.kb.tangtang.mission.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** 최근 28일 카테고리별 소비 집계 결과. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CategorySpending {

    private Long categoryId;
    private String parentCategoryName;
    private String categoryName;
    private BigDecimal totalAmount;
    private int transactionCount;
}
