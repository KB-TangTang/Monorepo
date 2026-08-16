package com.kb.tangtang.report.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.docs.DevChallengeReportControllerDocs;
import com.kb.tangtang.report.dto.ChallengeMonthlyReportBatchRunDto;
import com.kb.tangtang.report.service.DevChallengeReportService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/reports/challenge")
public class DevChallengeReportController implements DevChallengeReportControllerDocs {

    private final DevChallengeReportService devChallengeReportService;

    public DevChallengeReportController(DevChallengeReportService devChallengeReportService) {
        this.devChallengeReportService = devChallengeReportService;
    }

    @Override
    @PostMapping("/monthly")
    public ApiResponse<ChallengeMonthlyReportBatchRunDto> runMonthlyBatch(
            @LoginUser Long userId,
            @RequestParam String yearMonth,
            @RequestParam(defaultValue = "false") boolean force) {
        return ApiResponse.ok(devChallengeReportService.runMonthlyBatch(yearMonth, force));
    }

    @Override
    @PostMapping("/monthly/group-records")
    public ApiResponse<ChallengeMonthlyReportBatchRunDto> runMonthlyGroupRecordBatch(
            @LoginUser Long userId,
            @RequestParam String yearMonth) {
        return ApiResponse.ok(devChallengeReportService.runMonthlyGroupRecordBatch(yearMonth));
    }
}
