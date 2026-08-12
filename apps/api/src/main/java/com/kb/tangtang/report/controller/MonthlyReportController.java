package com.kb.tangtang.report.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.dto.MonthlyReportMonthsDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import com.kb.tangtang.report.service.MonthlyReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/monthly")
public class MonthlyReportController {

    private final MonthlyReportService monthlyReportService;

    public MonthlyReportController(MonthlyReportService monthlyReportService) {
        this.monthlyReportService = monthlyReportService;
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
}
