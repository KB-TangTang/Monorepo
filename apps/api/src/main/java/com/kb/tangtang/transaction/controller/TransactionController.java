package com.kb.tangtang.transaction.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.transaction.docs.TransactionControllerDocs;
import com.kb.tangtang.transaction.dto.DailySpendingSummaryDto;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateRequestDto;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateResultDto;
import com.kb.tangtang.transaction.dto.TransactionListResponseDto;
import com.kb.tangtang.transaction.dto.TransactionMonthsDto;
import com.kb.tangtang.transaction.service.TransactionQueryService;
import com.kb.tangtang.transaction.service.TransactionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 사용자가 자신의 거래내역을 조회하고, 거래 카테고리를 직접 수정한다. */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController implements TransactionControllerDocs {

    private final TransactionService service;
    private final TransactionQueryService queryService;

    public TransactionController(TransactionService service, TransactionQueryService queryService) {
        this.service = service;
        this.queryService = queryService;
    }

    @PatchMapping("/{transactionId}/category")
    public ApiResponse<TransactionCategoryUpdateResultDto> updateCategory(
            @LoginUser Long userId,
            @PathVariable long transactionId,
            @RequestBody TransactionCategoryUpdateRequestDto request) {
        return ApiResponse.ok(service.updateCategory(userId, transactionId,
                request.getCategoryId(), request.isApplyToMerchant()));
    }

    @GetMapping("/months")
    public ApiResponse<TransactionMonthsDto> getAvailableMonths(@LoginUser Long userId) {
        return ApiResponse.ok(queryService.getAvailableMonths(userId));
    }

    @GetMapping
    public ApiResponse<TransactionListResponseDto> getTransactions(
            @LoginUser Long userId,
            @RequestParam(required = false) String yearMonth) {
        return ApiResponse.ok(queryService.getTransactions(userId, yearMonth));
    }

    @GetMapping("/summary/daily")
    public ApiResponse<DailySpendingSummaryDto> getDailySpendingSummary(@LoginUser Long userId) {
        return ApiResponse.ok(queryService.getDailySpendingSummary(userId));
    }
}
