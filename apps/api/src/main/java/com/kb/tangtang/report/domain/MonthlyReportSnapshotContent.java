package com.kb.tangtang.report.domain;

import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** category_summary_json v2를 역직렬화한 월간 리포트 화면 데이터다. */
@Getter
@AllArgsConstructor
public class MonthlyReportSnapshotContent {

    private boolean aiUsageConsented;
    private MonthlySummaryDto summary;
    private MonthlySpendingTrendDto spendingTrend;
    private MonthlyCategoryReportDto categoryReport;
}
