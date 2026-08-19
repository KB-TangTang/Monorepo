package com.kb.tangtang.account.controller;

import com.kb.tangtang.account.dto.FinancialSyncRequestDto;
import com.kb.tangtang.account.dto.FinancialSyncResultDto;
import com.kb.tangtang.account.service.FinancialSyncService;
import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    /**
     * institutionCodes 는 선택 항목이다 — 계좌 연동 직후 최초 동기화(LinkDoneView)만 채워 보낸다.
     * 자세한 이유는 FinancialSyncService.sync(long, Set) 참고(#334).
     */
    @PostMapping
    public ApiResponse<FinancialSyncResultDto> sync(@LoginUser Long userId,
                                                     @RequestBody(required = false) FinancialSyncRequestDto request) {
        List<String> institutionCodes = request == null ? null : request.getInstitutionCodes();
        Set<String> extra = institutionCodes == null ? Set.of() : new HashSet<>(institutionCodes);
        return ApiResponse.ok(financialSyncService.sync(userId, extra));
    }
}
