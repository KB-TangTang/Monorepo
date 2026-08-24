package com.kb.tangtang.report.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.report.docs.DevMonthlyReportControllerDocs;
import com.kb.tangtang.report.dto.MonthlyReportBatchRunDto;
import com.kb.tangtang.report.dto.MonthlyReportManualBatchRequestDto;
import com.kb.tangtang.report.service.MonthlyReportManualBatchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 배포 환경에서도 운영 키로 실행하는 월간 소비 리포트 배치다. */
@RestController
@RequestMapping("/api/dev/reports/monthly")
public class DevMonthlyReportController implements DevMonthlyReportControllerDocs {

    private final MonthlyReportManualBatchService manualBatchService;

    public DevMonthlyReportController(MonthlyReportManualBatchService manualBatchService) {
        this.manualBatchService = manualBatchService;
    }

    @Override
    @PostMapping("/batch")
    public ApiResponse<MonthlyReportBatchRunDto> runManualBatch(
            @LoginUser Long userId,
            @RequestHeader(value = "X-Report-Batch-Key", required = false) String operationKey,
            @RequestBody(required = false) MonthlyReportManualBatchRequestDto request) {
        return ApiResponse.ok(manualBatchService.run(operationKey, request));
    }
}
