package com.kb.tangtang.fixedexpense.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 월간 고정지출 탐지에 사용할 정상 소비 거래 한 행. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedExpenseDetectionTransaction {

    private Long id;
    private Long userId;
    private String merchantNameNormalized;
    private BigDecimal amount;
    private LocalDate trDate;
    private Long categoryId;
    private String classification;
    private String cancelYn;
    private boolean isRefund;
    private boolean isExcludedFromSummary;
}
