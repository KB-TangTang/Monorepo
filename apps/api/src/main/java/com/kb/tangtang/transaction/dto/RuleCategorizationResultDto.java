package com.kb.tangtang.transaction.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class RuleCategorizationResultDto {
    private int ruleCategorizedCount;
    private List<Long> llmEligibleTransactionIds;
}
