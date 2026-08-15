package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DepositSyncDto {
    private final long depositAccountId;
    private final String institutionCode;
    private final String institutionName;
    private final String productName;
    private final String accountNoMasked;
    private final String currency;
    private final BigDecimal principal;
    private final BigDecimal balance;
    private final BigDecimal interestRate;
    private final String maturityDate;
}
