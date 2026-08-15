package com.kb.tangtang.report.service;

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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
}
