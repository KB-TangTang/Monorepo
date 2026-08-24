package com.kb.tangtang.report.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 강제 월간 리포트 배치의 사용자별 기존 스냅샷 상태다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportForceBatchCandidate {

    private Long userId;
    private String categorySummaryJson;
    private String aiAnalysisStatus;
}
