package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FixedExpenseEvidenceMonthDto {

    private int month;
    private boolean detected;
}
