package com.kb.tangtang.fixedexpense.dto;

import lombok.Getter;
import lombok.Setter;

/** PATCH /api/fixedExpenses/candidates/{candidateId} 요청 본문. */
@Getter
@Setter
public class FixedExpenseCandidateActionRequestDto {

    /** CONFIRM 또는 EXCLUDE. 유효성은 Service가 공통 BusinessException 흐름으로 검증한다. */
    private String action;
}
