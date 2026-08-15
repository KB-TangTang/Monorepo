package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.fixedexpense.domain.FixedExpenseReadRow;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseDetailDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseOverviewDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpensePaymentHistoryDto;
import com.kb.tangtang.fixedexpense.dto.FixedExpenseSavingsReportDto;
import com.kb.tangtang.fixedexpense.mapper.FixedExpenseQueryMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FixedExpenseQueryServiceTest {

    private static final long USER_ID = 7L;
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Test
    void separatesCandidatesAndConfirmedItemsAndCalculatesConsistentSummary() {
        FixedExpenseQueryMapper mapper = mock(FixedExpenseQueryMapper.class);
        when(mapper.findActiveItems(USER_ID, null, true)).thenReturn(List.of(
                row(101L, "넷플릭스", "구독", "17000", 3, true)));
        when(mapper.findActiveItems(USER_ID, null, false)).thenReturn(List.of(
                row(201L, "유튜브 프리미엄", "구독", "14900", 4, false)));

        FixedExpenseOverviewDto result = service(mapper).getOverview(USER_ID, "2026-08", null);

        assertEquals("2026-08", result.getYearMonth());
        assertEquals(1, result.getConfirmed().size());
        assertEquals(1, result.getCandidates().size());
        assertTrue(result.getConfirmed().get(0).getIsConfirmed());
        assertFalse(result.getCandidates().get(0).getIsConfirmed());
        assertEquals(0, new BigDecimal("31900").compareTo(
                result.getSummary().getExpectedMonthlyAmount()));
        assertEquals(1, result.getSummary().getConfirmedCount());
        assertEquals(1, result.getSummary().getCandidateCount());
    }

    @Test
    void forwardsCategoryFilterToBothListsAndReturnsEmptyListsAsNormalData() {
        FixedExpenseQueryMapper mapper = mock(FixedExpenseQueryMapper.class);
        when(mapper.findActiveItems(USER_ID, 3L, true)).thenReturn(List.of());
        when(mapper.findActiveItems(USER_ID, 3L, false)).thenReturn(List.of());

        FixedExpenseOverviewDto result = service(mapper).getOverview(USER_ID, null, 3L);

        verify(mapper).findActiveItems(USER_ID, 3L, true);
        verify(mapper).findActiveItems(USER_ID, 3L, false);
        assertEquals("2026-08", result.getYearMonth());
        assertTrue(result.getConfirmed().isEmpty());
        assertTrue(result.getCandidates().isEmpty());
        assertEquals(0, result.getSummary().getExpectedMonthlyAmount().signum());
    }

    @Test
    void excludesCandidatesFromSavingsAndMultipliesConfirmedMonthlyAmountByTwelve() {
        FixedExpenseQueryMapper mapper = mock(FixedExpenseQueryMapper.class);
        when(mapper.findActiveItems(USER_ID, null, true)).thenReturn(List.of(
                row(101L, "넷플릭스", "구독", "17000", 3, true),
                row(102L, "헬스장", "생활", "55000", 4, true)));

        FixedExpenseSavingsReportDto result = service(mapper).getSavingsReport(USER_ID, null);

        assertEquals(2, result.getItems().size());
        assertEquals(0, new BigDecimal("72000").compareTo(result.getMonthlySavings()));
        assertEquals(0, new BigDecimal("864000").compareTo(result.getYearlySavings()));
        assertEquals("넷플릭스", result.getItems().get(0).getTitle());
    }

    @Test
    void returnsOwnedDetailWithPaymentHistorySortedByMapperAndEmptyHistoryWhenAbsent() {
        FixedExpenseQueryMapper mapper = mock(FixedExpenseQueryMapper.class);
        when(mapper.findOwnedActiveItem(USER_ID, 101L)).thenReturn(
                row(101L, "넷플릭스", "구독", "17000", 4, true));
        when(mapper.findRecentPaymentHistory(eq(USER_ID), eq(101L), any(), any())).thenReturn(List.of(
                payment(11L, "2026-08-10", "신한카드", "17000"),
                payment(10L, "2026-07-10", "신한카드", "13500")));
        when(mapper.sumPaymentHistory(eq(USER_ID), eq(101L), any(), any()))
                .thenReturn(new BigDecimal("81500"));

        FixedExpenseDetailDto result = service(mapper).getDetail(USER_ID, 101L);

        assertTrue(result.getItem().getIsConfirmed());
        assertEquals("2026-08-10", result.getPaymentHistory().get(0).getDate().toString());
        assertEquals(0, new BigDecimal("81500").compareTo(result.getSixMonthTotal()));
        assertEquals(8, result.getChangeNotice().getMonth());
        assertEquals(0, new BigDecimal("3500").compareTo(result.getChangeNotice().getDifference()));
        assertEquals(4, result.getEvidenceMonths().size());

        when(mapper.findOwnedActiveItem(USER_ID, 102L)).thenReturn(
                row(102L, "헬스장", "생활", "55000", 3, false));
        when(mapper.findRecentPaymentHistory(eq(USER_ID), eq(102L), any(), any())).thenReturn(List.of());
        when(mapper.sumPaymentHistory(eq(USER_ID), eq(102L), any(), any())).thenReturn(BigDecimal.ZERO);

        FixedExpenseDetailDto emptyHistory = service(mapper).getDetail(USER_ID, 102L);
        assertTrue(emptyHistory.getPaymentHistory().isEmpty());
        assertNull(emptyHistory.getChangeNotice());
    }

    @Test
    void blocksOtherUsersAndInvalidCurrentMonthInputs() {
        FixedExpenseQueryMapper mapper = mock(FixedExpenseQueryMapper.class);
        when(mapper.findOwnedActiveItem(USER_ID, 999L)).thenReturn(null);

        BusinessException notFound = assertThrows(BusinessException.class,
                () -> service(mapper).getDetail(USER_ID, 999L));
        assertEquals("NOT_FOUND", notFound.getCode());
        assertEquals(HttpStatus.NOT_FOUND, notFound.getHttpStatus());

        BusinessException malformed = assertThrows(BusinessException.class,
                () -> service(mapper).getOverview(USER_ID, "2026/08", null));
        assertEquals("INVALID_REQUEST", malformed.getCode());

        BusinessException pastMonth = assertThrows(BusinessException.class,
                () -> service(mapper).getSavingsReport(USER_ID, "2026-07"));
        assertEquals("NOT_FOUND", pastMonth.getCode());

        BusinessException invalidCategory = assertThrows(BusinessException.class,
                () -> service(mapper).getOverview(USER_ID, null, 0L));
        assertEquals("INVALID_REQUEST", invalidCategory.getCode());
    }

    private FixedExpenseQueryService service(FixedExpenseQueryMapper mapper) {
        return new FixedExpenseQueryService(mapper, CLOCK);
    }

    private FixedExpenseReadRow row(long id, String name, String category, String amount,
                                    int detectedCount, boolean confirmed) {
        return FixedExpenseReadRow.builder()
                .id(id)
                .userId(USER_ID)
                .merchantNameNormalized(name)
                .avgAmount(new BigDecimal(amount))
                .cycleDays(30)
                .detectedCount(detectedCount)
                .categoryId(3L)
                .categoryName(category)
                .isExcluded(false)
                .status("ACTIVE")
                .nextExpectedDate(LocalDate.of(2026, 8, 17))
                .confirmedAt(confirmed ? java.time.LocalDateTime.of(2026, 8, 1, 9, 0) : null)
                .build();
    }

    private FixedExpensePaymentHistoryDto payment(long id, String date, String provider, String amount) {
        return FixedExpensePaymentHistoryDto.builder()
                .id(id)
                .date(LocalDate.parse(date))
                .provider(provider)
                .amount(new BigDecimal(amount))
                .build();
    }
}
