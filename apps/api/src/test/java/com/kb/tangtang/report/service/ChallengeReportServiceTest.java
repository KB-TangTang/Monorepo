package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.ChallengeMonthlyReportRow;
import com.kb.tangtang.report.dto.ChallengeReportDetailDto;
import com.kb.tangtang.report.dto.ChallengeReportMonthsDto;
import com.kb.tangtang.report.mapper.ChallengeReportMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeReportServiceTest {

    private static final long USER_ID = 7L;
    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final ChallengeReportMapper mapper = mock(ChallengeReportMapper.class);
    private final ChallengeReportService service = new ChallengeReportService(mapper,
            Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), SEOUL_ZONE));

    @Test
    @DisplayName("챌린지 동의가 없으면 미동의 진입 상태와 빈 월 목록을 반환한다")
    void returnsNotAgreedWhenChallengeConsentIsMissing() {
        when(mapper.hasActiveChallengeConsent(eq(USER_ID), any())).thenReturn(false);

        ChallengeReportMonthsDto result = service.getAvailableMonths(USER_ID);

        assertEquals("NOT_AGREED", result.getEntryState());
        assertTrue(result.getMonths().isEmpty());
    }

    @Test
    @DisplayName("동의 후 확정 리포트가 없으면 첫 리포트 준비 상태를 반환한다")
    void returnsPreparingStateWithoutConfirmedReport() {
        when(mapper.hasActiveChallengeConsent(eq(USER_ID), any())).thenReturn(true);
        when(mapper.findConfirmedReportMonths(USER_ID, "2026-08")).thenReturn(List.of());
        when(mapper.findFirstMissionMonth(USER_ID, LocalDate.of(2026, 8, 1))).thenReturn(null);
        when(mapper.findChallengeConsentMonth(USER_ID, LocalDate.of(2026, 8, 1))).thenReturn(null);

        ChallengeReportMonthsDto result = service.getAvailableMonths(USER_ID);

        assertEquals("PREPARING_FIRST_REPORT", result.getEntryState());
        assertTrue(result.getMonths().isEmpty());
    }

    @Test
    @DisplayName("첫 리포트가 확정되기 전에도 지난 미션 기록의 월을 준비 상태로 반환한다")
    void returnsPreparingMonthFromMissionHistory() {
        when(mapper.hasActiveChallengeConsent(eq(USER_ID), any())).thenReturn(true);
        when(mapper.findConfirmedReportMonths(USER_ID, "2026-08")).thenReturn(List.of());
        when(mapper.findFirstMissionMonth(USER_ID, LocalDate.of(2026, 8, 1))).thenReturn("2026-06");

        ChallengeReportMonthsDto result = service.getAvailableMonths(USER_ID);

        assertEquals("PREPARING_FIRST_REPORT", result.getEntryState());
        assertEquals("2026-06", result.getMonths().get(0).getValue());
        assertFalse(result.getMonths().get(0).isAvailable());
        assertFalse(result.getMonths().get(0).isHasReport());
    }

    @Test
    @DisplayName("미션이 아직 없어도 지난 챌린지 동의 월을 준비 상태로 반환한다")
    void returnsPreparingMonthFromChallengeConsent() {
        when(mapper.hasActiveChallengeConsent(eq(USER_ID), any())).thenReturn(true);
        when(mapper.findConfirmedReportMonths(USER_ID, "2026-08")).thenReturn(List.of());
        when(mapper.findFirstMissionMonth(USER_ID, LocalDate.of(2026, 8, 1))).thenReturn(null);
        when(mapper.findChallengeConsentMonth(USER_ID, LocalDate.of(2026, 8, 1))).thenReturn("2026-06");

        ChallengeReportMonthsDto result = service.getAvailableMonths(USER_ID);

        assertEquals("2026-06", result.getMonths().get(0).getValue());
        assertFalse(result.getMonths().get(0).isAvailable());
    }

    @Test
    @DisplayName("확정 월만 최신순으로 반환하고 가장 오래된 확정 월을 첫 리포트로 표시한다")
    void returnsConfirmedMonthsAndMarksFirstReport() {
        when(mapper.hasActiveChallengeConsent(eq(USER_ID), any())).thenReturn(true);
        when(mapper.findConfirmedReportMonths(USER_ID, "2026-08"))
                .thenReturn(List.of("2026-07", "2026-06"));

        ChallengeReportMonthsDto result = service.getAvailableMonths(USER_ID);

        assertEquals("READY", result.getEntryState());
        assertEquals(List.of("2026-07", "2026-06"),
                result.getMonths().stream().map(item -> item.getValue()).toList());
        assertFalse(result.getMonths().get(0).isFirstReport());
        assertTrue(result.getMonths().get(1).isFirstReport());
        assertTrue(result.getMonths().stream().allMatch(item -> item.isAvailable() && item.isHasReport()));
        verify(mapper).findConfirmedReportMonths(USER_ID, "2026-08");
    }

    @Test
    @DisplayName("전월 확정 행이 없으면 월 목록 조회 시 해당 사용자만 보정 확정을 실행한다")
    void recoversMissingPreviousMonthReportWhenLoadingMonths() {
        ChallengeMonthlyReportSnapshotService snapshotService = mock(ChallengeMonthlyReportSnapshotService.class);
        ChallengeReportService recoveringService = new ChallengeReportService(mapper,
                Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), SEOUL_ZONE),
                new ObjectMapper(), snapshotService);
        when(mapper.hasActiveChallengeConsent(eq(USER_ID), any())).thenReturn(true);
        when(mapper.findMonthlyReport(USER_ID, "2026-07")).thenReturn(null);
        when(mapper.findConfirmedReportMonths(USER_ID, "2026-08")).thenReturn(List.of("2026-06"));

        recoveringService.getAvailableMonths(USER_ID);

        verify(snapshotService).finalizeUserMonth(eq(USER_ID), eq(java.time.YearMonth.of(2026, 7)), any());
    }

    @Test
    @DisplayName("전월 확정 행이 있으면 월 목록 조회에서 재계산하지 않는다")
    void doesNotRecalculateExistingPreviousMonthReportWhenLoadingMonths() {
        ChallengeMonthlyReportSnapshotService snapshotService = mock(ChallengeMonthlyReportSnapshotService.class);
        ChallengeReportService recoveringService = new ChallengeReportService(mapper,
                Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), SEOUL_ZONE),
                new ObjectMapper(), snapshotService);
        when(mapper.hasActiveChallengeConsent(eq(USER_ID), any())).thenReturn(true);
        when(mapper.findMonthlyReport(USER_ID, "2026-07")).thenReturn(report("2026-07", 20, 15));
        when(mapper.findConfirmedReportMonths(USER_ID, "2026-08")).thenReturn(List.of("2026-07", "2026-06"));

        recoveringService.getAvailableMonths(USER_ID);

        verify(snapshotService, never()).finalizeUserMonth(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(java.time.YearMonth.class),
                org.mockito.ArgumentMatchers.any(java.time.LocalDateTime.class));
    }

    @Test
    @DisplayName("첫 확정 리포트는 전월 비교 없이 저장된 개인 성과를 반환한다")
    void returnsFirstReportWithoutPreviousComparison() {
        when(mapper.hasActiveChallengeConsent(eq(USER_ID), any())).thenReturn(true);
        when(mapper.findMonthlyReport(USER_ID, "2026-07")).thenReturn(report("2026-07", 20, 15));
        when(mapper.findPreviousMonthlyReport(USER_ID, "2026-07")).thenReturn(null);

        ChallengeReportDetailDto result = service.getReport(USER_ID, "2026-07");

        assertEquals("2026-07", result.getPeriod());
        assertEquals(75.00, result.getMissionSuccessRate().doubleValue());
        assertFalse(result.isHasPreviousComparison());
        assertTrue(result.isFirstServiceMonth());
        assertNull(result.getMonthOverMonthPercentagePoint());
        assertEquals(15, result.getSuccessfulDays());
        assertEquals(20, result.getChallengeDays());
        assertEquals(42000, result.getSavedAmount().intValue());
        assertEquals(7000, result.getOverspentAmount().intValue());
        assertEquals(35000, result.getNetSavings().intValue());
        assertEquals(420000, result.getAnnualizedNetSavings().intValue());
        assertEquals("카페", result.getCategoryEffects().get(0).getCategoryName());
        assertEquals(0, result.getCategoryEffects().get(0).getOverspentAmount().intValue());
        assertEquals(2, result.getWeeklyResults().size());
        assertEquals("EASY", result.getDifficulties().get(0).getDifficultyName());
    }

    @Test
    @DisplayName("현재 월과 미래 월은 확정 전이라 상세 조회를 거절한다")
    void rejectsCurrentOrFutureReport() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getReport(USER_ID, "2026-08"));

        assertEquals("CHALLENGE_REPORT_NOT_AVAILABLE", exception.getCode());
    }

    @Test
    @DisplayName("확정 스냅샷이 없으면 상세 조회를 거절한다")
    void rejectsMissingConfirmedReport() {
        when(mapper.hasActiveChallengeConsent(eq(USER_ID), any())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getReport(USER_ID, "2026-07"));

        assertEquals("CHALLENGE_REPORT_NOT_FOUND", exception.getCode());
    }

    private ChallengeMonthlyReportRow report(String yearMonth, int totalDays, int successDays) {
        ChallengeMonthlyReportRow report = new ChallengeMonthlyReportRow();
        report.setYearMonth(yearMonth);
        report.setTotalDays(totalDays);
        report.setSuccessDays(successDays);
        report.setSavedAmount(new java.math.BigDecimal("42000"));
        report.setOverspentAmount(new java.math.BigDecimal("7000"));
        report.setNetAmount(new java.math.BigDecimal("35000"));
        report.setCategoryEffectsJson("[{\"categoryId\":1,\"categoryName\":\"카페\",\"successfulDays\":2,"
                + "\"savedAmount\":42000,\"failedDays\":1,\"overspentAmount\":0}]");
        report.setMonthlyLongestStreak(4);
        report.setBestWeekday("월요일");
        report.setEarnedScore(85);
        report.setWeeklyResultsJson("[{\"week\":1,\"successDays\":3,\"totalDays\":4,\"successRate\":75.00},"
                + "{\"week\":2,\"successDays\":12,\"totalDays\":16,\"successRate\":75.00}]");
        report.setDifficultyResultsJson("[{\"difficultyName\":\"EASY\",\"attempts\":10,\"successDays\":8,\"successRate\":80.00}]");
        return report;
    }
}
