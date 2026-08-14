package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 관리 목록의 후보·확정 항목 공통 표시 모델이다. */
@Getter
@Builder
public class FixedExpenseItemDto {

    private long id;
    private String status;
    private Boolean isExcluded;
    private Boolean isConfirmed;
    private LocalDateTime confirmedAt;
    private Long categoryId;
    private String categoryCode;
    private String categoryLabel;
    private String name;
    private String category;
    private String description;
    private BigDecimal averageAmount;
    private FixedExpenseBillingCycleDto billingCycle;
    private LocalDate nextPaymentDate;
    private String paymentLabel;
    private String caseNumber;
    private int evidenceCount;
}
