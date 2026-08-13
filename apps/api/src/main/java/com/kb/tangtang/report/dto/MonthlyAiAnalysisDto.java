package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MonthlyAiAnalysisDto {

    private String yearMonth;
    private List<String> feedbacks;
    private String savingsAnalogy;
}
