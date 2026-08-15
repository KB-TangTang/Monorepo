package com.kb.tangtang.report.service;

import com.kb.tangtang.common.dev.DevEnvironmentGuard;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.dto.ChallengeMonthlyReportBatchRunDto;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DevChallengeReportServiceTest {

    @Test
    void forceOptionExplicitlyRecalculatesPastMonth() {
        ChallengeMonthlyReportBatchService batchService = mock(ChallengeMonthlyReportBatchService.class);
        when(batchService.finalizeReports(java.time.YearMonth.of(2026, 7), true)).thenReturn(3);
        DevChallengeReportService service = new DevChallengeReportService(
                new DevEnvironmentGuard("local"), batchService,
                Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), ZoneId.of("Asia/Seoul")));

        ChallengeMonthlyReportBatchRunDto result = service.runMonthlyBatch("2026-07", true);

        assertEquals("2026-07", result.getYearMonth());
        assertEquals(3, result.getAffected());
        assertEquals(true, result.isForced());
        verify(batchService).finalizeReports(java.time.YearMonth.of(2026, 7), true);
    }

    @Test
    void rejectsCurrentOrFutureMonth() {
        DevChallengeReportService service = new DevChallengeReportService(
                new DevEnvironmentGuard("local"), mock(ChallengeMonthlyReportBatchService.class),
                Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), ZoneId.of("Asia/Seoul")));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.runMonthlyBatch("2026-08", false));

        assertEquals("CHALLENGE_REPORT_NOT_AVAILABLE", exception.getCode());
    }
}
