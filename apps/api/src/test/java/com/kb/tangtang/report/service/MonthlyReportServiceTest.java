package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyCategorySpendingRow;
import com.kb.tangtang.report.domain.MonthlySpendingRow;
import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.dto.MonthlyReportMonthsDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyReportServiceTest {

    private static final long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 12);

    @Mock
    private MonthlyReportMapper mapper;

    private MonthlyReportService service;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(TODAY.atStartOfDay(zoneId).toInstant(), zoneId);
        service = new MonthlyReportService(mapper, clock);
        when(mapper.findUserCreatedDate(USER_ID)).thenReturn(LocalDate.of(2026, 3, 15));
    }

    @Test
    @DisplayName("월 총소비와 전월 대비 감소율 및 고정지출 후보 개수를 계산한다")
    void returnsMonthlySummary() {
        when(mapper.sumNetSpending(eq(USER_ID), eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 8, 1)))).thenReturn(new BigDecimal("1284000"));
        when(mapper.sumNetSpending(eq(USER_ID), eq(LocalDate.of(2026, 6, 1)),
                eq(LocalDate.of(2026, 7, 1)))).thenReturn(new BigDecimal("1396000"));
        when(mapper.countActiveFixedExpenseCandidates(USER_ID)).thenReturn(2);

        MonthlySummaryDto result = service.getSummary(USER_ID, "2026-07");

        assertEquals(new BigDecimal("1284000"), result.getTotalSpent());
        assertEquals(new BigDecimal("1396000"), result.getPreviousMonthSpent());
        assertEquals(new BigDecimal("-8.02"), result.getMonthOverMonthRate());
        assertEquals(2, result.getFixedExpenseCandidateCount());
        assertTrue(result.isHasPreviousComparison());
    }

    @Test
    @DisplayName("전월 소비가 0이면 당월 증가율을 null로 반환한다")
    void returnsNullRateWhenPreviousMonthIsZero() {
        when(mapper.sumNetSpending(eq(USER_ID), any(), any()))
                .thenReturn(new BigDecimal("10000"), BigDecimal.ZERO);

        MonthlySummaryDto result = service.getSummary(USER_ID, "2026-07");

        assertNull(result.getMonthOverMonthRate());
    }

    @Test
    @DisplayName("가입 첫 달은 전월 비교를 제공하지 않는다")
    void omitsPreviousComparisonForFirstReport() {
        when(mapper.sumNetSpending(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("50000"));

        MonthlySummaryDto result = service.getSummary(USER_ID, "2026-03");

        assertFalse(result.isHasPreviousComparison());
        assertNull(result.getPreviousMonthSpent());
        assertNull(result.getMonthOverMonthRate());
    }

    @Test
    @DisplayName("최근 6개월에서 가입 전 빈 슬롯과 실제 0원 월을 구분한다")
    void distinguishesUnavailableAndZeroMonths() {
        when(mapper.findMonthlySpending(eq(USER_ID), any(), any())).thenReturn(List.of(
                new MonthlySpendingRow("2026-03", new BigDecimal("100000")),
                new MonthlySpendingRow("2026-05", BigDecimal.ZERO)));

        MonthlySpendingTrendDto result = service.getSpendingTrend(USER_ID, "2026-05");

        assertEquals("2025-12", result.getItems().get(0).getYearMonth());
        assertFalse(result.getItems().get(0).isHasData());
        assertNull(result.getItems().get(0).getAmount());
        assertTrue(result.getItems().get(3).isHasData());
        assertEquals(new BigDecimal("100000"), result.getItems().get(3).getAmount());
        assertEquals(BigDecimal.ZERO, result.getItems().get(4).getAmount());
        assertEquals(BigDecimal.ZERO, result.getItems().get(5).getAmount());
    }

    @Test
    @DisplayName("카테고리별 금액과 비율 및 전월 증감률을 계산한다")
    void returnsCategoryReport() {
        when(mapper.findMonthlyCategorySpending(eq(USER_ID), any(), any())).thenReturn(List.of(
                new MonthlyCategorySpendingRow("2026-06", 18L, "카페/간식",
                        new BigDecimal("200000")),
                new MonthlyCategorySpendingRow("2026-07", 18L, "카페/간식",
                        new BigDecimal("150000")),
                new MonthlyCategorySpendingRow("2026-07", null, "미분류",
                        new BigDecimal("50000")),
                new MonthlyCategorySpendingRow("2026-07", 3L, "환불초과",
                        new BigDecimal("-1000"))));
        when(mapper.sumNetSpending(eq(USER_ID), eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 8, 1)))).thenReturn(new BigDecimal("200000"));

        MonthlyCategoryReportDto result = service.getCategories(USER_ID, "2026-07");

        assertEquals(2, result.getCategories().size());
        assertEquals("카페/간식", result.getCategories().get(0).getCategoryName());
        assertEquals(new BigDecimal("75.00"), result.getCategories().get(0).getRatio());
        assertEquals(new BigDecimal("-25.00"), result.getCategories().get(0).getChangeRate());
        assertEquals("미분류", result.getCategories().get(1).getCategoryName());
    }

    @Test
    @DisplayName("가입월부터 현재월까지 조회 월 목록을 최신순으로 반환한다")
    void returnsAvailableMonths() {
        when(mapper.findUserCreatedDate(USER_ID)).thenReturn(LocalDate.of(2026, 6, 20));

        MonthlyReportMonthsDto result = service.getAvailableMonths(USER_ID);

        assertEquals(List.of("2026-08", "2026-07", "2026-06"),
                result.getMonths().stream().map(item -> item.getValue()).toList());
        assertFalse(result.getMonths().get(0).isAvailable());
        assertEquals("current", result.getMonths().get(0).getStatus());
        assertTrue(result.getMonths().get(1).isAvailable());
    }

    @Test
    @DisplayName("잘못된 형식과 현재월 및 가입 전 월은 거절한다")
    void rejectsInvalidPeriods() {
        BusinessException invalid = assertThrows(BusinessException.class,
                () -> service.getSummary(USER_ID, "2026-8"));
        assertEquals("INVALID_REQUEST", invalid.getCode());

        BusinessException current = assertThrows(BusinessException.class,
                () -> service.getSummary(USER_ID, "2026-08"));
        assertEquals("REPORT_NOT_AVAILABLE", current.getCode());

        BusinessException beforeJoin = assertThrows(BusinessException.class,
                () -> service.getSummary(USER_ID, "2026-02"));
        assertEquals("REPORT_NOT_AVAILABLE", beforeJoin.getCode());
    }
}
