package com.kb.tangtang.report.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 월간 소비 리포트 배치의 사용자 단위 처리 결과다. */
@Getter
@AllArgsConstructor
public class MonthlyReportBatchRunResult {

    private int targetCount;
    private int snapshotSavedCount;
    private int aiGeneratedCount;
    private int failureCount;
}
