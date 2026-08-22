package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.Loan;
import com.kb.tangtang.account.dto.AssetLoanDetailDto;
import com.kb.tangtang.account.mapper.LoanMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetLoanDetailServiceTest {

    private static final long USER_ID = 1L;

    @Mock
    private LoanMapper loanMapper;

    private AssetLoanDetailService service() {
        return new AssetLoanDetailService(loanMapper);
    }

    @Test
    @DisplayName("대출 목록과 잔액 합계를 그대로 옮긴다")
    void returnsLoansAndTotal() {
        Loan loan = Loan.builder()
                .id(1L)
                .userId(USER_ID)
                .loanNoEncrypted("MOCK-1")
                .bankName("하나캐피탈")
                .bankCode("CP_HANA")
                .loanType("신용대출")
                .loanAmount(new BigDecimal("2000000"))
                .balance(new BigDecimal("1500000"))
                .interestRate(new BigDecimal("4.50"))
                .startDate(LocalDate.of(2025, 1, 10))
                .maturityDate(LocalDate.of(2027, 12, 10))
                .monthlyPayment(new BigDecimal("50000"))
                .nextPaymentDate(LocalDate.of(2026, 9, 10))
                .build();
        when(loanMapper.findByUser(USER_ID)).thenReturn(List.of(loan));
        when(loanMapper.sumBalanceByUser(USER_ID)).thenReturn(new BigDecimal("1500000"));

        AssetLoanDetailDto result = service().getLoans(USER_ID);

        assertEquals(new BigDecimal("1500000"), result.getTotal());
        assertEquals(1, result.getLoans().size());
        assertEquals("하나캐피탈", result.getLoans().get(0).getBankName());
        assertEquals("CP_HANA", result.getLoans().get(0).getBankCode());
        assertEquals(new BigDecimal("4.50"), result.getLoans().get(0).getInterestRate());
        assertEquals(LocalDate.of(2027, 12, 10), result.getLoans().get(0).getMaturityDate());
    }

    @Test
    @DisplayName("대출이 없으면 0 원과 빈 목록을 돌려준다")
    void returnsZeroWhenNoLoans() {
        when(loanMapper.findByUser(USER_ID)).thenReturn(List.of());
        when(loanMapper.sumBalanceByUser(USER_ID)).thenReturn(BigDecimal.ZERO);

        AssetLoanDetailDto result = service().getLoans(USER_ID);

        assertEquals(BigDecimal.ZERO, result.getTotal());
        assertEquals(0, result.getLoans().size());
    }
}
