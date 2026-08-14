package com.kb.tangtang.fixedexpense.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** tbl_fixed_expense_candidate 한 행. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FixedExpenseCandidate {

    private Long id;
    private Long userId;
    private String merchantNameNormalized;
    private BigDecimal avgAmount;
    private int cycleDays;
    private int detectedCount;
    private Long categoryId;
    private boolean isExcluded;
    private String status;
    private LocalDate nextExpectedDate;
    private LocalDateTime confirmedAt;
    private LocalDateTime relapseDetectedAt;
}
