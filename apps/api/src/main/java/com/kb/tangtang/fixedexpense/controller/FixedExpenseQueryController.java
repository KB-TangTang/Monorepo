package com.kb.tangtang.fixedexpense.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.fixedexpense.docs.FixedExpenseQueryControllerDocs;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseDetailDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseOverviewDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseSavingsReportDto;
import com.kb.tangtang.fixedexpense.service.FixedExpenseQueryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 고정지출 관리 화면의 목록·상세·절약 가능액 조회를 제공한다. */
@RestController
@RequestMapping("/api/fixedExpenses")
public class FixedExpenseQueryController implements FixedExpenseQueryControllerDocs {

    private final FixedExpenseQueryService service;

    public FixedExpenseQueryController(FixedExpenseQueryService service) {
        this.service = service;
    }

    @Override
    @GetMapping("/candidates")
    public ApiResponse<FixedExpenseOverviewDto> getOverview(
            @LoginUser Long userId,
            @RequestParam(required = false) String yearMonth,
            @RequestParam(required = false) Long categoryId) {
        return ApiResponse.ok(service.getOverview(userId, yearMonth, categoryId));
    }

    @Override
    @GetMapping("/candidates/{candidateId}")
    public ApiResponse<FixedExpenseDetailDto> getDetail(
            @LoginUser Long userId,
            @PathVariable long candidateId) {
        return ApiResponse.ok(service.getDetail(userId, candidateId));
    }

    @Override
    @GetMapping("/savingReport")
    public ApiResponse<FixedExpenseSavingsReportDto> getSavingsReport(
            @LoginUser Long userId,
            @RequestParam(required = false) String yearMonth) {
        return ApiResponse.ok(service.getSavingsReport(userId, yearMonth));
    }
}
