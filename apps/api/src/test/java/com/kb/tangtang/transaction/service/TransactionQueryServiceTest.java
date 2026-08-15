package com.kb.tangtang.transaction.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.transaction.domain.TransactionListRow;
import com.kb.tangtang.transaction.dto.LedgerSummaryDto;
import com.kb.tangtang.transaction.dto.TransactionListItemDto;
import com.kb.tangtang.transaction.dto.TransactionListResponseDto;
import com.kb.tangtang.transaction.dto.TransactionMonthDto;
import com.kb.tangtang.transaction.dto.TransactionMonthsDto;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionQueryServiceTest {

    private static final long USER_ID = 1L;
    /** 시스템 프롬프트의 오늘(2026-08-15)에 맞춘 고정 시계. */
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-08-15T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    private TransactionMapper transactionMapper;
    private TransactionQueryService service;

    @BeforeEach
    void setUp() {
        transactionMapper = mock(TransactionMapper.class);
        service = new TransactionQueryService(transactionMapper, FIXED_CLOCK);
    }

    private TransactionListRow row(String classification, BigDecimal amount, boolean isRefund,
                                    BigDecimal refundedAmount, String direction, String sourceType,
                                    String cardTypeCode, String cardInstitutionName, String accountBankName) {
        return TransactionListRow.builder()
                .id(1L)
                .trDate(LocalDate.of(2026, 7, 14))
                .merchantName("스타벅스 강남점")
                .categoryName("카페/간식")
                .classification(classification)
                .amount(amount)
                .isRefund(isRefund)
                .refundedAmount(refundedAmount)
                .direction(direction)
                .sourceType(sourceType)
                .cardTypeCode(cardTypeCode)
                .cardInstitutionName(cardInstitutionName)
                .accountBankName(accountBankName)
                .build();
    }

    @Test
    @DisplayName("데이터가 있는 가장 이른 달~현재월을 오름차순으로 반환하고, 거래가 있는 달만 hasData=true다")
    void returnsMonthsAscendingWithHasDataFlag() {
        // 06월과 08월(이번 달)에만 거래가 있고 07월은 빈 달 — 가입월(tbl_user.created_at)이 아니라
        // 실제 데이터의 가장 이른 달부터 시작해야 한다(2026-08-15 버그: 가입월 기준이면 범위가
        // "이번 달"뿐이라 월 이동이 항상 막혔다).
        when(transactionMapper.findDistinctDataMonths(USER_ID)).thenReturn(List.of("2026-06", "2026-08"));

        TransactionMonthsDto result = service.getAvailableMonths(USER_ID);

        List<TransactionMonthDto> months = result.getMonths();
        assertEquals(3, months.size());
        assertEquals("2026-06", months.get(0).getValue());
        assertTrue(months.get(0).isHasData());
        assertEquals("2026-07", months.get(1).getValue());
        assertFalse(months.get(1).isHasData());
        assertEquals("2026-08", months.get(2).getValue());
        assertTrue(months.get(2).isHasData());
    }

    @Test
    @DisplayName("거래가 하나도 없으면 현재월 한 칸만(hasData=false) 반환한다")
    void returnsCurrentMonthOnlyWhenNoDataExists() {
        when(transactionMapper.findDistinctDataMonths(USER_ID)).thenReturn(Collections.emptyList());

        TransactionMonthsDto result = service.getAvailableMonths(USER_ID);

        List<TransactionMonthDto> months = result.getMonths();
        assertEquals(1, months.size());
        assertEquals("2026-08", months.get(0).getValue());
        assertFalse(months.get(0).isHasData());
    }

    @Test
    @DisplayName("yearMonth를 생략하면 기간 없이 전체 거래를 조회하고 summary는 null이다")
    void returnsAllTransactionsWithoutSummaryWhenYearMonthBlank() {
        when(transactionMapper.findTransactionRows(USER_ID, null, null))
                .thenReturn(List.of(row("INCOME", BigDecimal.valueOf(3000), false, null, "IN", "BANK", null, null, null)));

        TransactionListResponseDto result = service.getTransactions(USER_ID, null);

        assertNull(result.getPeriod());
        assertNull(result.getSummary());
        assertEquals(1, result.getTransactions().size());
    }

    @Test
    @DisplayName("형식이 틀린 yearMonth는 400 INVALID_REQUEST다")
    void rejectsInvalidYearMonthFormat() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getTransactions(USER_ID, "2026/07"));

        assertEquals("INVALID_REQUEST", exception.getCode());
    }

    @Test
    @DisplayName("데이터가 있는 가장 이른 달보다 이전 yearMonth는 400 LEDGER_NOT_AVAILABLE이다")
    void rejectsYearMonthBeforeEarliestDataMonth() {
        when(transactionMapper.findDistinctDataMonths(USER_ID)).thenReturn(List.of("2026-07"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getTransactions(USER_ID, "2026-06"));

        assertEquals("LEDGER_NOT_AVAILABLE", exception.getCode());
    }

    @Test
    @DisplayName("CONSUMPTION은 음수, INCOME은 양수, 환불은 양수로 상계, TRANSFER는 direction 기준 부호다")
    void resolvesSignedAmountPerClassification() {
        when(transactionMapper.findDistinctDataMonths(USER_ID)).thenReturn(List.of("2026-07"));
        when(transactionMapper.findTransactionRows(eq(USER_ID), any(), any())).thenReturn(List.of(
                row("CONSUMPTION", BigDecimal.valueOf(10000), false, null, "OUT", "CARD_CREDIT", "01", "신한", null),
                row("INCOME", BigDecimal.valueOf(3350000), false, null, "IN", "BANK", null, null, null),
                row("CONSUMPTION", BigDecimal.valueOf(5000), true, BigDecimal.valueOf(4000), "OUT", "CARD_CREDIT", "01", "신한", null),
                row("TRANSFER", BigDecimal.valueOf(50000), false, null, "OUT", "BANK", null, null, "카카오뱅크")
        ));
        when(transactionMapper.sumNetConsumption(eq(USER_ID), any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionMapper.sumIncome(eq(USER_ID), any(), any())).thenReturn(BigDecimal.ZERO);

        List<TransactionListItemDto> items = service.getTransactions(USER_ID, "2026-07").getTransactions();

        assertEquals(0, new BigDecimal("-10000").compareTo(items.get(0).getAmount()));
        assertEquals(0, new BigDecimal("3350000").compareTo(items.get(1).getAmount()));
        assertEquals(0, new BigDecimal("4000").compareTo(items.get(2).getAmount()));
        assertEquals(0, new BigDecimal("-50000").compareTo(items.get(3).getAmount()));
    }

    @Test
    @DisplayName("신용카드는 카드사명+카드, 체크카드는 카드사명+체크카드로 라벨을 만든다")
    void resolvesPaymentMethodLabel() {
        when(transactionMapper.findDistinctDataMonths(USER_ID)).thenReturn(List.of("2026-07"));
        when(transactionMapper.findTransactionRows(eq(USER_ID), any(), any())).thenReturn(List.of(
                row("CONSUMPTION", BigDecimal.valueOf(1000), false, null, "OUT", "CARD_CREDIT", "01", "신한", null),
                row("CONSUMPTION", BigDecimal.valueOf(1000), false, null, "OUT", "CARD_CHECK", "02", "KB국민", null),
                row("INCOME", BigDecimal.valueOf(1000), false, null, "IN", "BANK", null, null, null)
        ));
        when(transactionMapper.sumNetConsumption(eq(USER_ID), any(), any())).thenReturn(BigDecimal.valueOf(2000));
        when(transactionMapper.sumIncome(eq(USER_ID), any(), any())).thenReturn(BigDecimal.valueOf(1000));

        List<TransactionListItemDto> items = service.getTransactions(USER_ID, "2026-07").getTransactions();

        assertEquals("신한카드", items.get(0).getPaymentMethod());
        assertEquals("KB국민 체크카드", items.get(1).getPaymentMethod());
        assertEquals("입금", items.get(2).getPaymentMethod());
    }

    @Test
    @DisplayName("summary.period도 채운다 — LedgerSummaryCard가 summary.period로 'N월 지출' 라벨을 그린다")
    void summaryIncludesPeriod() {
        when(transactionMapper.findDistinctDataMonths(USER_ID)).thenReturn(List.of("2026-07"));
        when(transactionMapper.findTransactionRows(eq(USER_ID), any(), any())).thenReturn(Collections.emptyList());
        when(transactionMapper.sumNetConsumption(eq(USER_ID), any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionMapper.sumIncome(eq(USER_ID), any(), any())).thenReturn(BigDecimal.ZERO);

        LedgerSummaryDto summary = service.getTransactions(USER_ID, "2026-07").getSummary();

        assertEquals("2026-07", summary.getPeriod());
    }

    @Test
    @DisplayName("전달 지출이 0이면 전월 대비 증감률은 0이다")
    void monthOverMonthRateIsZeroWhenPreviousSpentIsZero() {
        when(transactionMapper.findDistinctDataMonths(USER_ID)).thenReturn(List.of("2026-07"));
        when(transactionMapper.findTransactionRows(eq(USER_ID), any(), any())).thenReturn(Collections.emptyList());
        when(transactionMapper.sumNetConsumption(eq(USER_ID), any(), any())).thenReturn(BigDecimal.ZERO);
        when(transactionMapper.sumIncome(eq(USER_ID), any(), any())).thenReturn(BigDecimal.ZERO);

        LedgerSummaryDto summary = service.getTransactions(USER_ID, "2026-07").getSummary();

        assertEquals(0, BigDecimal.ZERO.compareTo(summary.getMonthOverMonthRate()));
    }
}
