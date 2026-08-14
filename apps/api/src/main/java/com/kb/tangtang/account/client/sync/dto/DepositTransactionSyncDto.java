package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DepositTransactionSyncDto {
    private final long transactionId;
    private final String transactedAt;
    private final String transTypeCode;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String description;
    private final String rawJson;
}
