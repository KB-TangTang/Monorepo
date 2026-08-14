package com.kb.tangtang.fixedexpense.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FixedExpenseOverviewDto {

    private String yearMonth;
    private FixedExpenseOverviewSummaryDto summary;
    private List<FixedExpenseItemDto> confirmed;
    private List<FixedExpenseItemDto> candidates;
}
