package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyReportBatchCandidate;
import com.kb.tangtang.report.mapper.MonthlyReportBatchMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyReportBatchServiceTest {

    @Mock
    private MonthlyReportBatchMapper batchMapper;

    @Mock
    private MonthlyAiAnalysisSnapshotService snapshotService;

    @Mock
    private MonthlyAiAnalysisService aiAnalysisService;

    private MonthlyReportBatchService service;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(LocalDate.of(2026, 8, 1).atTime(0, 15).atZone(zoneId).toInstant(), zoneId);
        service = new MonthlyReportBatchService(batchMapper, snapshotService, aiAnalysisService, clock, 3, 20);
    }

    @Test
    void generatesOnlyPreviousMonthUsingKstClock() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 0, 15);
        when(batchMapper.findEligibleCandidates(
                "2026-07", LocalDateTime.of(2026, 8, 1, 0, 0), 3, now.minusMinutes(20)))
                .thenReturn(List.of(new MonthlyReportBatchCandidate(1L)));

        service.generatePreviousMonthReports();

        verify(batchMapper).findEligibleCandidates(
                eq("2026-07"), eq(LocalDateTime.of(2026, 8, 1, 0, 0)), eq(3), eq(now.minusMinutes(20)));
        verify(snapshotService).savePendingSnapshot(1L, "2026-07");
        verify(aiAnalysisService).generateUsingPreparedSnapshot(1L, "2026-07");
    }

    @Test
    void continuesWithLaterUsersAfterOneUserFails() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 0, 40);
        when(batchMapper.findEligibleCandidates(
                "2026-07", LocalDateTime.of(2026, 8, 1, 0, 0), 3, now.minusMinutes(20)))
                .thenReturn(List.of(new MonthlyReportBatchCandidate(1L), new MonthlyReportBatchCandidate(2L)));
        doThrow(new BusinessException("AI_PROVIDER_UNAVAILABLE", "temporary"))
                .when(aiAnalysisService).generateUsingPreparedSnapshot(1L, "2026-07");

        service.generateReports(YearMonth.of(2026, 7), now);

        verify(snapshotService).savePendingSnapshot(1L, "2026-07");
        verify(snapshotService).savePendingSnapshot(2L, "2026-07");
        verify(aiAnalysisService).generateUsingPreparedSnapshot(2L, "2026-07");
    }
}
