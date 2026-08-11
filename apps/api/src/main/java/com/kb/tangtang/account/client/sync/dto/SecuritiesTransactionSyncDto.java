package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class SecuritiesTransactionSyncDto {
    private final long transactionId;
    private final String transactedAt;
    private final String transTypeCode;
    private final String securityProductCode;
    private final String securityProductName;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal transactionAmount;
}
