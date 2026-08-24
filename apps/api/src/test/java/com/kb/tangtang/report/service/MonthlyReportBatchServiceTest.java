package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyReportBatchCandidate;
import com.kb.tangtang.report.domain.MonthlyReportBatchRunResult;
import com.kb.tangtang.report.domain.MonthlyReportForceBatchCandidate;
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
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
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
                .thenReturn(List.of(new MonthlyReportBatchCandidate(1L, true)));

        service.generatePreviousMonthReports();

        verify(batchMapper).findEligibleCandidates(
                eq("2026-07"), eq(LocalDateTime.of(2026, 8, 1, 0, 0)), eq(3), eq(now.minusMinutes(20)));
        verify(snapshotService).saveSnapshot(1L, "2026-07", true, false);
        verify(aiAnalysisService).generateUsingPreparedSnapshot(1L, "2026-07");
    }

    @Test
    void continuesWithLaterUsersAfterOneUserFails() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 0, 40);
        when(batchMapper.findEligibleCandidates(
                "2026-07", LocalDateTime.of(2026, 8, 1, 0, 0), 3, now.minusMinutes(20)))
                .thenReturn(List.of(new MonthlyReportBatchCandidate(1L, true), new MonthlyReportBatchCandidate(2L, true)));
        doThrow(new BusinessException("AI_PROVIDER_UNAVAILABLE", "temporary"))
                .when(aiAnalysisService).generateUsingPreparedSnapshot(1L, "2026-07");

        service.generateReports(YearMonth.of(2026, 7), now);

        verify(snapshotService).saveSnapshot(1L, "2026-07", true, false);
        verify(snapshotService).saveSnapshot(2L, "2026-07", true, false);
        verify(aiAnalysisService).generateUsingPreparedSnapshot(2L, "2026-07");
    }

    @Test
    void storesNotConsentedSnapshotWithoutCallingAi() {
        LocalDateTime now = LocalDateTime.of(2026, 8, 1, 0, 15);
        when(batchMapper.findEligibleCandidates(
                "2026-07", LocalDateTime.of(2026, 8, 1, 0, 0), 3, now.minusMinutes(20)))
                .thenReturn(List.of(new MonthlyReportBatchCandidate(1L, false)));

        service.generatePreviousMonthReports();

        verify(snapshotService).saveSnapshot(1L, "2026-07", false, false);
        verify(aiAnalysisService, never()).generateUsingPreparedSnapshot(1L, "2026-07");
    }

    @Test
    void forceRunUsesStoredConsentAndOverwritesCompletedSnapshot() {
        when(batchMapper.findForceBatchCandidates("2026-07", LocalDateTime.of(2026, 8, 1, 0, 0), Set.of(1L)))
                .thenReturn(List.of(new MonthlyReportForceBatchCandidate(1L, snapshotJson(true), "COMPLETED")));

        MonthlyReportBatchRunResult result = service.runManualBatch(
                YearMonth.of(2026, 7), true, Set.of(1L), Map.of());

        assertEquals(1, result.getTargetCount());
        assertEquals(1, result.getSnapshotSavedCount());
        assertEquals(1, result.getAiGeneratedCount());
        verify(snapshotService).saveSnapshot(1L, "2026-07", true, true);
        verify(aiAnalysisService).generateUsingPreparedSnapshot(1L, "2026-07");
    }

    @Test
    void forceRunRequiresExplicitConsentForMissingOrLegacySnapshotBeforeWriting() {
        when(batchMapper.findForceBatchCandidates("2026-07", LocalDateTime.of(2026, 8, 1, 0, 0), Set.of(1L)))
                .thenReturn(List.of(new MonthlyReportForceBatchCandidate(1L, null, null)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.runManualBatch(YearMonth.of(2026, 7), true, Set.of(1L), Map.of()));

        assertEquals("MISSING_AI_CONSENT_INPUT", exception.getCode());
        verify(snapshotService, never()).saveSnapshot(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    @Test
    void forceRunRejectsUserWhoJoinedAfterTheTargetMonth() {
        when(batchMapper.findForceBatchCandidates("2026-05", LocalDateTime.of(2026, 6, 1, 0, 0), Set.of(1L)))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.runManualBatch(YearMonth.of(2026, 5), true, Set.of(1L), Map.of()));

        assertEquals("REPORT_NOT_AVAILABLE", exception.getCode());
        verify(snapshotService, never()).saveSnapshot(
                org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyBoolean(), org.mockito.ArgumentMatchers.anyBoolean());
    }

    private String snapshotJson(boolean aiUsageConsented) {
        return "{\"snapshotVersion\":2,\"aiUsageConsented\":" + aiUsageConsented + ","
                + "\"summary\":{\"yearMonth\":\"2026-07\",\"totalSpent\":0,"
                + "\"previousMonthSpent\":null,\"hasPreviousComparison\":false,"
                + "\"monthOverMonthRate\":null,\"fixedExpenseCandidateCount\":0,"
                + "\"confirmedFixedExpenseCount\":0},"
                + "\"spendingTrend\":{\"yearMonth\":\"2026-07\",\"items\":[]},"
                + "\"categoryReport\":{\"yearMonth\":\"2026-07\",\"totalSpent\":0,"
                + "\"parentCategories\":[],\"categories\":[]}}";
    }
}
