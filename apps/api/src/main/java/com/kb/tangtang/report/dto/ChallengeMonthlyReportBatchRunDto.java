package com.kb.tangtang.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChallengeMonthlyReportBatchRunDto {

    private String yearMonth;
    private int affected;
    private boolean forced;
}
