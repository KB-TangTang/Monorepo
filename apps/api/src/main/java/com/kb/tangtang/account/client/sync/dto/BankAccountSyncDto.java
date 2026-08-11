package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class BankAccountSyncDto {
    private final long accountId;
    private final String institutionCode;
    private final String institutionName;
    private final String accountTypeCode;
    private final String productName;
    private final String accountNoMasked;
    private final String currency;
    private final BigDecimal balance;
    private final BigDecimal availableAmount;
}
