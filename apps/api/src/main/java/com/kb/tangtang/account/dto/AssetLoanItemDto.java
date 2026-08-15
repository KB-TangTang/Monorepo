package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 자산 상세(대출) 목록의 대출 한 행.
 * loanNoEncrypted 는 표시용이 아니라(내부 upsert 키) 응답에서 제외한다.
 */
@Getter
@Builder
public class AssetLoanItemDto {

    private final long loanId;
    private final String bankName;
    private final String loanType;
    private final BigDecimal loanAmount;
    private final BigDecimal balance;
    private final BigDecimal interestRate;
    private final LocalDate startDate;
    private final LocalDate maturityDate;
    private final BigDecimal monthlyPayment;
    private final LocalDate nextPaymentDate;
}
