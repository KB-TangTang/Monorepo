package com.kb.tangtang.report.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import com.kb.tangtang.report.dto.MonthlyReportMonthsDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import com.kb.tangtang.report.service.MonthlyReportService;
import com.kb.tangtang.report.service.MonthlyAiAnalysisService;
import com.kb.tangtang.report.service.MonthlyAiAnalysisQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/monthly")
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;
    private final MonthlyAiAnalysisService monthlyAiAnalysisService;
    private final MonthlyAiAnalysisQueryService monthlyAiAnalysisQueryService;

    public MonthlyReportController(MonthlyReportService monthlyReportService,
                                   MonthlyAiAnalysisService monthlyAiAnalysisService,
                                   MonthlyAiAnalysisQueryService monthlyAiAnalysisQueryService) {
        this.monthlyReportService = monthlyReportService;
        this.monthlyAiAnalysisService = monthlyAiAnalysisService;
        this.monthlyAiAnalysisQueryService = monthlyAiAnalysisQueryService;
    }

    @GetMapping("/spending-trend")
    public ApiResponse<MonthlySpendingTrendDto> getSpendingTrend(
            @LoginUser Long userId,
            @RequestParam String yearMonth) {
        return ApiResponse.ok(monthlyReportService.getSpendingTrend(userId, yearMonth));
    }

    @GetMapping("/summary")
    public ApiResponse<MonthlySummaryDto> getSummary(
            @LoginUser Long userId,
            @RequestParam String yearMonth) {
        return ApiResponse.ok(monthlyReportService.getSummary(userId, yearMonth));
    }

    @GetMapping("/categories")
    public ApiResponse<MonthlyCategoryReportDto> getCategories(
            @LoginUser Long userId,
            @RequestParam String yearMonth) {
        return ApiResponse.ok(monthlyReportService.getCategories(userId, yearMonth));
    }

    @GetMapping("/months")
    public ApiResponse<MonthlyReportMonthsDto> getAvailableMonths(@LoginUser Long userId) {
        return ApiResponse.ok(monthlyReportService.getAvailableMonths(userId));
    }

    @PostMapping("/ai-analysis")
    public ApiResponse<MonthlyAiAnalysisDto> generateAiAnalysis(
            @LoginUser Long userId,
            @RequestParam String yearMonth) {
        return ApiResponse.ok(monthlyAiAnalysisService.generate(userId, yearMonth));
    }

    @GetMapping("/ai-analysis")
    public ApiResponse<MonthlyAiAnalysisDto> getAiAnalysis(
            @LoginUser Long userId,
            @RequestParam String yearMonth) {
        MonthlyAiAnalysisDto generated = monthlyAiAnalysisService.generateIfSnapshotMissing(userId, yearMonth);
        return ApiResponse.ok(generated != null
                ? generated
                : monthlyAiAnalysisQueryService.get(userId, yearMonth));
    }
}
