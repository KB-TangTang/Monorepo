package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** 후보·확정 고정지출이 공통으로 쓰는 상세 응답이다. */
@Getter
@Builder
public class FixedExpenseDetailDto {

    private FixedExpenseItemDto item;
    private List<FixedExpensePaymentHistoryDto> paymentHistory;
    private List<FixedExpenseEvidenceMonthDto> evidenceMonths;
    private BigDecimal sixMonthTotal;
    private FixedExpenseChangeNoticeDto changeNotice;
}
