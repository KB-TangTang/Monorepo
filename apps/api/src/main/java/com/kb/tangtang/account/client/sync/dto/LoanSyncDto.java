package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class LoanSyncDto {
    private final long loanId;
    private final String institutionCode;
    private final String institutionName;
    private final String productName;
    private final String loanNoMasked;
    private final BigDecimal principal;
    private final BigDecimal balance;
    private final BigDecimal interestRate;
    private final String startDate;
    private final String maturityDate;
    private final BigDecimal monthlyPayment;
    private final String nextPaymentDate;
}
