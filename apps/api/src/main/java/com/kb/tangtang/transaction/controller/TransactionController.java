package com.kb.tangtang.transaction.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.transaction.docs.TransactionControllerDocs;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateRequestDto;
import com.kb.tangtang.transaction.dto.TransactionCategoryUpdateResultDto;
import com.kb.tangtang.transaction.service.TransactionService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 사용자가 자신의 거래 카테고리를 직접 수정한다. */
@RestController
@RequestMapping("/api/transactions")
public class TransactionController implements TransactionControllerDocs {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PatchMapping("/{transactionId}/category")
    public ApiResponse<TransactionCategoryUpdateResultDto> updateCategory(
            @LoginUser Long userId,
            @PathVariable long transactionId,
            @RequestBody TransactionCategoryUpdateRequestDto request) {
        return ApiResponse.ok(service.updateCategory(userId, transactionId,
                request.getCategoryId(), request.isApplyToMerchant()));
    }
}
