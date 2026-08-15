package com.kb.tangtang.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** 한 카테고리에서 개인 미션 성공·실패로 생긴 확정 효과다. */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ChallengeCategoryEffectDto {

    private long categoryId;
    private String categoryName;
    private int successfulDays;
    private BigDecimal savedAmount;
    private int failedDays;
    private BigDecimal overspentAmount;
}
