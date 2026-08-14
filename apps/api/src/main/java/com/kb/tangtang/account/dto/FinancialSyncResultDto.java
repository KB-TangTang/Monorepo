package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FinancialSyncResultDto {
    private String status;
    private List<String> syncedSources;
    private String syncedAt;
    /** 이번 POST 호출에서 upsert 된 거래 수 (누적이 아니다). */
    private int collectedTransactionCount;
    /** 이번 호출에서 규칙(1~4단계)으로 분류된 거래 수. */
    private int ruleCategorizedCount;
    /** 이번 호출에서 LLM 분류 작업으로 등록 대상이 된 거래 수. */
    private int llmPendingTransactionCount;
    /** PENDING(LLM 대상 있음) | NOT_REQUIRED(전부 규칙으로 분류됐거나 대상 자체가 없음). */
    private String llmCategorizationStatus;
}
