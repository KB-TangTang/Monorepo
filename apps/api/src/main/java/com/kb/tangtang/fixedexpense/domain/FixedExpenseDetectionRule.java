package com.kb.tangtang.fixedexpense.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** DB에서 읽은 월간 고정지출 탐지 룰. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedExpenseDetectionRule {

    private Long id;
    private int minRepeatCount;
    private BigDecimal amountTolerancePct;
    private int cycleToleranceDays;
}
