package com.kb.tangtang.report.service;

import com.kb.tangtang.report.mapper.ChallengeReportMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeMonthlyReportBatchServiceTest {

    @Test
    void finalizesOnlyPreviousMonthCandidates() {
        ChallengeReportMapper mapper = mock(ChallengeReportMapper.class);
        ChallengeMonthlyReportSnapshotService snapshotService = mock(ChallengeMonthlyReportSnapshotService.class);
        when(mapper.findFinalizedReportUserIds(any(), any(), any())).thenReturn(List.of(7L, 8L));
        ChallengeMonthlyReportBatchService service = new ChallengeMonthlyReportBatchService(
                mapper,
                snapshotService,
                Clock.fixed(Instant.parse("2026-09-01T00:20:00Z"), ZoneId.of("Asia/Seoul")));

        service.finalizePreviousMonthReports();

        verify(mapper).findFinalizedReportUserIds(org.mockito.ArgumentMatchers.eq(java.time.LocalDate.of(2026, 8, 1)),
                org.mockito.ArgumentMatchers.eq(java.time.LocalDate.of(2026, 8, 31)),
                org.mockito.ArgumentMatchers.eq("2026-08"));
        verify(snapshotService).finalizeUserMonth(org.mockito.ArgumentMatchers.eq(7L),
                org.mockito.ArgumentMatchers.eq(java.time.YearMonth.of(2026, 8)), any());
        verify(snapshotService).finalizeUserMonth(org.mockito.ArgumentMatchers.eq(8L),
                org.mockito.ArgumentMatchers.eq(java.time.YearMonth.of(2026, 8)), any());
    }
}
