package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class FixedExpenseChangeNoticeDto {

    private int month;
    private BigDecimal difference;
}
