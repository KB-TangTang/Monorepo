package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class PayMoneySyncDto {
    private final long payMoneyId;
    private final String providerCode;
    private final String providerName;
    private final String walletName;
    private final BigDecimal balance;
    private final BigDecimal availableAmount;
    private final BigDecimal pointAmount;
}
