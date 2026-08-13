package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisSnapshot;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyAiAnalysisQueryServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private MonthlyReportMapper mapper;

    private MonthlyAiAnalysisQueryService service;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(LocalDate.of(2026, 8, 12).atStartOfDay(zoneId).toInstant(), zoneId);
        service = new MonthlyAiAnalysisQueryService(mapper,
                new MonthlyAiAnalysisResultReader(new ObjectMapper()), clock);
        when(mapper.findUserCreatedDate(USER_ID)).thenReturn(LocalDate.of(2026, 3, 15));
    }

    @Test
    void returnsNotRequestedWithoutCreatingASnapshot() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07")).thenReturn(null);

        MonthlyAiAnalysisDto result = service.get(USER_ID, "2026-07");

        assertEquals("NOT_REQUESTED", result.getStatus());
        assertEquals(List.of(), result.getFeedbacks());
        assertNull(result.getSavingsAnalogy());
        verify(mapper).findAiAnalysisSnapshot(USER_ID, "2026-07");
    }

    @Test
    void returnsOnlyStoredCompletedResult() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L,
                        "[\"Review food-delivery spending once this month.\"]",
                        "This month saved 128,000 won.", "COMPLETED"));

        MonthlyAiAnalysisDto result = service.get(USER_ID, "2026-07");

        assertEquals("COMPLETED", result.getStatus());
        assertEquals(List.of("Review food-delivery spending once this month."), result.getFeedbacks());
        assertEquals("This month saved 128,000 won.", result.getSavingsAnalogy());
    }

    @Test
    void hidesStoredFieldsForAnIncompleteStatus() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L, "[\"old\"]", "old", "FAILED"));

        MonthlyAiAnalysisDto result = service.get(USER_ID, "2026-07");

        assertEquals("FAILED", result.getStatus());
        assertEquals(List.of(), result.getFeedbacks());
        assertNull(result.getSavingsAnalogy());
    }

    @Test
    void returnsAStoredResultErrorForInvalidCompletedJson() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L, "not-json", null, "COMPLETED"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.get(USER_ID, "2026-07"));

        assertEquals("AI_ANALYSIS_RESULT_UNAVAILABLE", exception.getCode());
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getHttpStatus());
    }
}
