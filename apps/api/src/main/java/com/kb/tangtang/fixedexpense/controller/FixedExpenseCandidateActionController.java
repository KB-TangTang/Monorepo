package com.kb.tangtang.fixedexpense.controller;

import com.kb.tangtang.common.auth.LoginUser;
import com.kb.tangtang.common.dto.ApiResponse;
import com.kb.tangtang.fixedexpense.docs.FixedExpenseCandidateActionControllerDocs;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseCandidateActionRequestDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseCandidateActionResponseDto;
import com.kb.tangtang.fixedexpense.service.FixedExpenseCandidateActionService;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 사용자가 자신의 고정지출 탐지 후보를 확정하거나 제외한다. */
@RestController
@RequestMapping("/api/fixedExpenses")
public class FixedExpenseCandidateActionController implements FixedExpenseCandidateActionControllerDocs {

    private final FixedExpenseCandidateActionService service;

    public FixedExpenseCandidateActionController(FixedExpenseCandidateActionService service) {
        this.service = service;
    }

    @PatchMapping("/candidates/{candidateId}")
    public ApiResponse<FixedExpenseCandidateActionResponseDto> decideCandidate(
            @LoginUser Long userId,
            @PathVariable long candidateId,
            @RequestBody(required = false) FixedExpenseCandidateActionRequestDto request) {
        String action = request == null ? null : request.getAction();
        return ApiResponse.ok(service.decide(userId, candidateId, action));
    }
}
