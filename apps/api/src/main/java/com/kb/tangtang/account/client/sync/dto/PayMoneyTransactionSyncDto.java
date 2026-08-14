package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayMoneyTransactionSyncDto {
    private final long transactionId;
    private final String transactedAt;
    private final String transTypeCode;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final String merchantName;
    private final String merchantCategoryCode;
    private final String merchantCategoryName;
    private final String description;
    private final String rawJson;
}
