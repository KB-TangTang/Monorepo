package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.Loan;
import com.kb.tangtang.account.dto.AssetLoanDetailDto;
import com.kb.tangtang.account.dto.AssetLoanItemDto;
import com.kb.tangtang.account.mapper.LoanMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 자산 상세(대출) 조회. tbl_loan.user_id 로 바로 조회한다(연결계좌 경유 불필요).
 * AssetSummaryService 가 이미 쓰는 LoanMapper.findByUser·sumBalanceByUser 를 그대로 재사용한다.
 */
@Service
public class AssetLoanDetailService {

    private final LoanMapper loanMapper;

    @Autowired
    public AssetLoanDetailService(LoanMapper loanMapper) {
        this.loanMapper = loanMapper;
    }

    @Transactional(readOnly = true)
    public AssetLoanDetailDto getLoans(long userId) {
        List<Loan> loans = loanMapper.findByUser(userId);
        BigDecimal total = zeroIfNull(loanMapper.sumBalanceByUser(userId));

        List<AssetLoanItemDto> loanDtos = loans.stream()
                .map(loan -> AssetLoanItemDto.builder()
                        .loanId(loan.getId())
                        .bankName(loan.getBankName())
                        .loanType(loan.getLoanType())
                        .loanAmount(loan.getLoanAmount())
                        .balance(loan.getBalance())
                        .interestRate(loan.getInterestRate())
                        .startDate(loan.getStartDate())
                        .maturityDate(loan.getMaturityDate())
                        .monthlyPayment(loan.getMonthlyPayment())
                        .nextPaymentDate(loan.getNextPaymentDate())
                        .build())
                .toList();

        return AssetLoanDetailDto.builder()
                .total(total)
                .loans(loanDtos)
                .build();
    }

    private BigDecimal zeroIfNull(BigDecimal amount) {
        return amount == null ? BigDecimal.ZERO : amount;
    }
}
