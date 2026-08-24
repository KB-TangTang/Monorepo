package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyReportBatchRunResult;
import com.kb.tangtang.report.dto.MonthlyReportAiConsentOverrideDto;
import com.kb.tangtang.report.dto.MonthlyReportBatchRunDto;
import com.kb.tangtang.report.dto.MonthlyReportManualBatchRequestDto;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MonthlyReportManualBatchServiceTest {

    @Test
    void passesPerUserHistoricalConsentOverridesToForcedBatch() {
        MonthlyReportManualBatchAccessGuard guard = mock(MonthlyReportManualBatchAccessGuard.class);
        MonthlyReportBatchService batchService = mock(MonthlyReportBatchService.class);
        MonthlyReportManualBatchService service = serviceAt(guard, batchService);
        MonthlyReportManualBatchRequestDto request = request("2026-07", true, 17L, false);
        when(batchService.runManualBatch(eq(java.time.YearMonth.of(2026, 7)), eq(true),
                eq(Map.of(17L, false))))
                .thenReturn(new MonthlyReportBatchRunResult(3, 3, 2, 1));

        MonthlyReportBatchRunDto result = service.run("operation-key", request);

        assertEquals("2026-07", result.getYearMonth());
        assertEquals(3, result.getTargetCount());
        assertEquals(3, result.getSnapshotSavedCount());
        assertEquals(2, result.getAiGeneratedCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(true, result.isForced());
        verify(guard).ensureAuthorized("operation-key");
    }

    @Test
    void rejectsDuplicateOrIncompleteConsentOverride() {
        MonthlyReportManualBatchAccessGuard guard = mock(MonthlyReportManualBatchAccessGuard.class);
        MonthlyReportManualBatchService service = serviceAt(guard, mock(MonthlyReportBatchService.class));
        MonthlyReportManualBatchRequestDto request = new MonthlyReportManualBatchRequestDto();
        request.setYearMonth("2026-07");
        request.setForce(true);
        request.setMissingSnapshotAiConsents(List.of(
                override(17L, true), override(17L, false)));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.run("operation-key", request));

        assertEquals("INVALID_REQUEST", exception.getCode());
    }

    @Test
    void rejectsCurrentMonth() {
        MonthlyReportManualBatchAccessGuard guard = mock(MonthlyReportManualBatchAccessGuard.class);
        MonthlyReportManualBatchService service = serviceAt(guard, mock(MonthlyReportBatchService.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.run("operation-key", request("2026-08", false, null, null)));

        assertEquals("REPORT_NOT_AVAILABLE", exception.getCode());
    }

    private MonthlyReportManualBatchService serviceAt(MonthlyReportManualBatchAccessGuard guard,
                                                       MonthlyReportBatchService batchService) {
        return new MonthlyReportManualBatchService(guard, batchService,
                Clock.fixed(Instant.parse("2026-08-15T03:00:00Z"), ZoneId.of("Asia/Seoul")));
    }

    private MonthlyReportManualBatchRequestDto request(String yearMonth,
                                                        boolean force,
                                                        Long userId,
                                                        Boolean aiUsageConsented) {
        MonthlyReportManualBatchRequestDto request = new MonthlyReportManualBatchRequestDto();
        request.setYearMonth(yearMonth);
        request.setForce(force);
        if (userId != null) {
            request.setMissingSnapshotAiConsents(List.of(override(userId, aiUsageConsented)));
        }
        return request;
    }

    private MonthlyReportAiConsentOverrideDto override(Long userId, Boolean aiUsageConsented) {
        MonthlyReportAiConsentOverrideDto override = new MonthlyReportAiConsentOverrideDto();
        override.setUserId(userId);
        override.setAiUsageConsented(aiUsageConsented);
        return override;
    }
}
