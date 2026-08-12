package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class MonthlyCategoryReportDto {

    private String yearMonth;
    private BigDecimal totalSpent;
    private List<MonthlyCategoryItemDto> categories;
}
