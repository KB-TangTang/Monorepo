package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FixedExpenseBillingCycleDto {

    private String type;
    private int day;
}
