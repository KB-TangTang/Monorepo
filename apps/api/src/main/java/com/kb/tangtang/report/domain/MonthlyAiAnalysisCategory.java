package com.kb.tangtang.report.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class MonthlyAiAnalysisCategory {

    private String parentCategoryName;
    private String categoryName;
    private BigDecimal amount;
}
