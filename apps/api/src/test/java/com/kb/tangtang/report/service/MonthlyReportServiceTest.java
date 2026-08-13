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
import static org.mockito.Mockito.verify;
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
        service = serviceAt(TODAY);
        when(mapper.findUserCreatedDate(USER_ID)).thenReturn(LocalDate.of(2026, 3, 15));
    }

    private MonthlyReportService serviceAt(LocalDate currentDate) {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(currentDate.atStartOfDay(zoneId).toInstant(), zoneId);
        return new MonthlyReportService(mapper, clock);
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
        when(mapper.sumNetSpending(USER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1)))
                .thenReturn(new BigDecimal("50000"));

        MonthlySummaryDto result = service.getSummary(USER_ID, "2026-03");

        assertFalse(result.isHasPreviousComparison());
        assertNull(result.getPreviousMonthSpent());
        assertNull(result.getMonthOverMonthRate());
        verify(mapper).sumNetSpending(USER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1));
    }

    @Test
    @DisplayName("첫 리포트 카테고리는 가입월 1일부터 집계하고 전월 증감률을 제공하지 않는다")
    void omitsCategoryComparisonForFirstReport() {
        when(mapper.findMonthlyCategorySpending(
                USER_ID, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 4, 1)))
                .thenReturn(List.of(new MonthlyCategorySpendingRow(
                        "2026-03", 1L, "식비", 18L, "카페/간식", new BigDecimal("50000"))));
        when(mapper.sumNetSpending(USER_ID, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 1)))
                .thenReturn(new BigDecimal("50000"));

        MonthlyCategoryReportDto result = service.getCategories(USER_ID, "2026-03");

        assertEquals(1, result.getCategories().size());
        assertNull(result.getCategories().get(0).getPreviousMonthAmount());
        assertNull(result.getCategories().get(0).getChangeRate());
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
    @DisplayName("첫 리포트 6개월 추이는 가입 이전 5개월을 빈 슬롯으로 유지한다")
    void keepsPreJoinTrendSlotsEmptyForFirstReport() {
        when(mapper.findMonthlySpending(eq(USER_ID), any(), any())).thenReturn(List.of(
                new MonthlySpendingRow("2026-03", new BigDecimal("50000"))));

        MonthlySpendingTrendDto result = service.getSpendingTrend(USER_ID, "2026-03");

        assertEquals(6, result.getItems().size());
        assertTrue(result.getItems().subList(0, 5).stream().noneMatch(item -> item.isHasData()));
        assertTrue(result.getItems().get(5).isHasData());
        assertEquals(new BigDecimal("50000"), result.getItems().get(5).getAmount());
    }

    @Test
    @DisplayName("카테고리별 금액과 비율 및 전월 증감률을 계산한다")
    void returnsCategoryReport() {
        when(mapper.findMonthlyCategorySpending(eq(USER_ID), any(), any())).thenReturn(List.of(
                new MonthlyCategorySpendingRow("2026-06", 1L, "식비", 18L, "카페/간식",
                        new BigDecimal("200000")),
                new MonthlyCategorySpendingRow("2026-07", 1L, "식비", 18L, "카페/간식",
                        new BigDecimal("125000")),
                new MonthlyCategorySpendingRow("2026-07", 1L, "식비", 19L, "배달앱",
                        new BigDecimal("25000")),
                new MonthlyCategorySpendingRow("2026-07", null, "미분류", null, "미분류",
                        new BigDecimal("50000")),
                new MonthlyCategorySpendingRow("2026-07", 2L, "쇼핑", 3L, "환불초과",
                        new BigDecimal("-1000"))));
        when(mapper.sumNetSpending(eq(USER_ID), eq(LocalDate.of(2026, 7, 1)),
                eq(LocalDate.of(2026, 8, 1)))).thenReturn(new BigDecimal("200000"));

        MonthlyCategoryReportDto result = service.getCategories(USER_ID, "2026-07");

        assertEquals(2, result.getParentCategories().size());
        assertEquals("식비", result.getParentCategories().get(0).getCategoryName());
        assertEquals(new BigDecimal("150000"), result.getParentCategories().get(0).getAmount());
        assertEquals(new BigDecimal("75.00"), result.getParentCategories().get(0).getRatio());
        assertEquals("미분류", result.getParentCategories().get(1).getCategoryName());

        assertEquals(3, result.getCategories().size());
        assertEquals("카페/간식", result.getCategories().get(0).getCategoryName());
        assertEquals("식비", result.getCategories().get(0).getParentCategoryName());
        assertEquals(new BigDecimal("62.50"), result.getCategories().get(0).getRatio());
        assertEquals(new BigDecimal("-37.50"), result.getCategories().get(0).getChangeRate());
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
        assertEquals("CURRENT", result.getMonths().get(0).getStatus());
        assertTrue(result.getMonths().get(1).isAvailable());
        assertEquals("READY", result.getMonths().get(1).getStatus());
        assertEquals("FIRST_REPORT", result.getMonths().get(2).getStatus());
    }

    @Test
    @DisplayName("가입월 온보딩은 선택할 수 있지만 아직 리포트가 아니다")
    void makesJoinedMonthOnboardingAvailableWithoutReport() {
        when(mapper.findUserCreatedDate(USER_ID)).thenReturn(LocalDate.of(2026, 8, 1));

        MonthlyReportMonthsDto result = service.getAvailableMonths(USER_ID);

        assertEquals(1, result.getMonths().size());
        assertTrue(result.getMonths().get(0).isAvailable());
        assertFalse(result.getMonths().get(0).isHasReport());
        assertEquals("ONBOARDING", result.getMonths().get(0).getStatus());
    }

    @Test
    @DisplayName("가입월 마지막 날까지 온보딩이고 익월 1일부터 첫 리포트다")
    void changesFromOnboardingToFirstReportOnNextMonthFirstDay() {
        when(mapper.findUserCreatedDate(USER_ID)).thenReturn(LocalDate.of(2026, 8, 12));

        MonthlyReportMonthsDto lastDay = serviceAt(LocalDate.of(2026, 8, 31))
                .getAvailableMonths(USER_ID);
        MonthlyReportMonthsDto nextMonth = serviceAt(LocalDate.of(2026, 9, 1))
                .getAvailableMonths(USER_ID);

        assertEquals("ONBOARDING", lastDay.getMonths().get(0).getStatus());
        assertEquals("CURRENT", nextMonth.getMonths().get(0).getStatus());
        assertEquals("FIRST_REPORT", nextMonth.getMonths().get(1).getStatus());
    }

    @Test
    @DisplayName("12월 가입자는 다음 해 1월 1일부터 첫 리포트를 본다")
    void handlesYearBoundary() {
        when(mapper.findUserCreatedDate(USER_ID)).thenReturn(LocalDate.of(2025, 12, 20));

        MonthlyReportMonthsDto result = serviceAt(LocalDate.of(2026, 1, 1))
                .getAvailableMonths(USER_ID);

        assertEquals(List.of("2026-01", "2025-12"),
                result.getMonths().stream().map(item -> item.getValue()).toList());
        assertEquals("FIRST_REPORT", result.getMonths().get(1).getStatus());
    }

    @Test
    @DisplayName("윤년 2월 가입자는 3월 1일부터 첫 리포트를 본다")
    void handlesLeapYearFebruaryBoundary() {
        when(mapper.findUserCreatedDate(USER_ID)).thenReturn(LocalDate.of(2028, 2, 29));

        MonthlyReportMonthsDto onboarding = serviceAt(LocalDate.of(2028, 2, 29))
                .getAvailableMonths(USER_ID);
        MonthlyReportMonthsDto firstReport = serviceAt(LocalDate.of(2028, 3, 1))
                .getAvailableMonths(USER_ID);

        assertEquals("ONBOARDING", onboarding.getMonths().get(0).getStatus());
        assertEquals("FIRST_REPORT", firstReport.getMonths().get(1).getStatus());
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

        BusinessException future = assertThrows(BusinessException.class,
                () -> service.getSummary(USER_ID, "2026-09"));
        assertEquals("REPORT_NOT_AVAILABLE", future.getCode());

        BusinessException beforeJoin = assertThrows(BusinessException.class,
                () -> service.getSummary(USER_ID, "2026-02"));
        assertEquals("REPORT_NOT_AVAILABLE", beforeJoin.getCode());
    }
}
