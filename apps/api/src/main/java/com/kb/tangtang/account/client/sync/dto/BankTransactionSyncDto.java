package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BankTransactionSyncDto {
    private final long transactionId;
    private final String transactedAt;
    private final String transTypeCode;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    /** 목서버 raw_json 원본. correlationId 등은 FinancialSyncService 가 여기서 파싱한다. */
    private final String rawJson;
}
