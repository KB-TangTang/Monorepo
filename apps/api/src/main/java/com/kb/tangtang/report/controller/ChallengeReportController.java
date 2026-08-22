package com.kb.tangtang.report.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.docs.ChallengeReportControllerDocs;
import com.kb.tangtang.report.dto.ChallengeReportDetailDto;
import com.kb.tangtang.report.dto.ChallengeReportMonthsDto;
import com.kb.tangtang.report.service.ChallengeReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports/challenge")
public class ChallengeReportController implements ChallengeReportControllerDocs {

    private final ChallengeReportService challengeReportService;

    public ChallengeReportController(ChallengeReportService challengeReportService) {
        this.challengeReportService = challengeReportService;
    }

    @GetMapping("/months")
    public ApiResponse<ChallengeReportMonthsDto> getAvailableMonths(@LoginUser Long userId) {
        return ApiResponse.ok(challengeReportService.getAvailableMonths(userId));
    }

    @GetMapping
    public ApiResponse<ChallengeReportDetailDto> getReport(
            @LoginUser Long userId,
            @RequestParam String yearMonth) {
        return ApiResponse.ok(challengeReportService.getReport(userId, yearMonth));
    }
}
