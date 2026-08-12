package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MonthlySpendingTrendDto {

    private String yearMonth;
    private List<MonthlySpendingTrendItemDto> items;
}
