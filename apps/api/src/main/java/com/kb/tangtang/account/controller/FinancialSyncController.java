package com.kb.tangtang.account.controller;

import com.kb.tangtang.account.dto.FinancialSyncResultDto;
import com.kb.tangtang.account.service.FinancialSyncService;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 금융 데이터 동기화 (이슈 #147).
 * 사용자 식별은 @LoginUser 로만 한다.
 */
@RestController
@RequestMapping("/api/financial-syncs")
public class FinancialSyncController {

    private final FinancialSyncService financialSyncService;

    public FinancialSyncController(FinancialSyncService financialSyncService) {
        this.financialSyncService = financialSyncService;
    }

    @PostMapping
    public ApiResponse<FinancialSyncResultDto> sync(@LoginUser Long userId) {
        return ApiResponse.ok(financialSyncService.sync(userId));
    }
}
