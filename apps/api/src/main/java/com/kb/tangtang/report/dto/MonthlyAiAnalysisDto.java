package com.kb.tangtang.report.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class MonthlyAiAnalysisDto {

    private String yearMonth;
    private String status;
    private List<String> feedbacks;
    private String savingsAnalogy;
}
