package com.kb.tangtang.account.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * tbl_loan 한 행.
 *
 * ⚠ loanNoEncrypted: 목서버는 이미 마스킹된 loanNoMasked 만 주고, HMAC 해시할 원본 계좌번호가 없다.
 *   그래서 "MOCK-" + 목서버 loanId 를 그대로 저장한다 — 소스에서 유일하기만 하면 되는 값이지
 *   실제 암호화된 값이 아니다. NOT NULL + UNIQUE(user_id, loan_no_encrypted) 제약을 만족시키기 위함.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class Loan {
    private Long id;
    private Long userId;
    private String loanNoEncrypted;
    private String loanNoMasked;
    private String bankName;
    private String bankCode;
    private String loanType;
    private BigDecimal loanAmount;
    private BigDecimal balance;
    private BigDecimal interestRate;
    private LocalDate startDate;
    private LocalDate maturityDate;
    private BigDecimal monthlyPayment;
    private LocalDate nextPaymentDate;

    public Loan(Long id, Long userId, String loanNoEncrypted, String loanNoMasked, String bankName, String bankCode,
                String loanType, BigDecimal loanAmount, BigDecimal balance, BigDecimal interestRate,
                LocalDate startDate, LocalDate maturityDate, BigDecimal monthlyPayment,
                LocalDate nextPaymentDate) {
        this.id = id;
        this.userId = userId;
        this.loanNoEncrypted = loanNoEncrypted;
        this.loanNoMasked = loanNoMasked;
        this.bankName = bankName;
        this.bankCode = bankCode;
        this.loanType = loanType;
        this.loanAmount = loanAmount;
        this.balance = balance;
        this.interestRate = interestRate;
        this.startDate = startDate;
        this.maturityDate = maturityDate;
        this.monthlyPayment = monthlyPayment;
        this.nextPaymentDate = nextPaymentDate;
    }
}
