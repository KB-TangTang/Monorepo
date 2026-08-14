package com.kb.tangtang.fixedexpense.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 고정지출 조회 화면을 만들기 위한 후보·확정 항목의 조인 결과다. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Setter
public class FixedExpenseReadRow {

    private Long id;
    private Long userId;
    private String merchantNameNormalized;
    private BigDecimal avgAmount;
    private int cycleDays;
    private int detectedCount;
    private Long categoryId;
    private String categoryName;
    private boolean isExcluded;
    private String status;
    private LocalDate nextExpectedDate;
    private LocalDateTime confirmedAt;
}
