package com.kb.tangtang.report.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** tbl_asset_snapshot 에 저장된 월간 리포트 페이로드와 AI 상태다. */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportSnapshotRow {

    private Long id;
    private String yearMonth;
    private String categorySummaryJson;
    private String aiAnalysisStatus;
}
