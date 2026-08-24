package com.kb.tangtang.report.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 운영용 월간 소비 리포트 수동 배치 응답이다. */
@Getter
@AllArgsConstructor
public class MonthlyReportBatchRunDto {

    private String yearMonth;
    private int targetCount;
    private int snapshotSavedCount;
    private int aiGeneratedCount;
    private int failureCount;
    private boolean forced;
}
