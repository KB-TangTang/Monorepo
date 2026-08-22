package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisSnapshot;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyAiAnalysisPreparedSnapshotGenerationTest {

    @Mock
    private MonthlyReportMapper mapper;

    @Mock
    private MonthlyAiAnalysisProvider provider;

    @Mock
    private MonthlyAiAnalysisStateService stateService;

    @Mock
    private MonthlyAiAnalysisSnapshotService snapshotService;

    private MonthlyAiAnalysisService service;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(LocalDate.of(2026, 8, 2).atStartOfDay(zoneId).toInstant(), zoneId);
        ObjectMapper objectMapper = new ObjectMapper();
        service = new MonthlyAiAnalysisService(mapper, provider, stateService, snapshotService,
                new MonthlyAiAnalysisResultReader(objectMapper), objectMapper, clock);
        when(mapper.findUserCreatedDate(1L)).thenReturn(LocalDate.of(2026, 4, 2));
    }

    @Test
    void usesPreparedSnapshotWithoutSavingItAgain() {
        when(mapper.findAiAnalysisSnapshot(1L, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L, null, null, "NOT_REQUESTED"));
        when(mapper.sumNetSpending(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("100000"), new BigDecimal("120000"));
        when(mapper.findMonthlyCategorySpending(eq(1L), any(), any())).thenReturn(List.of());
        when(stateService.claim(eq(1L), eq("2026-07"), eq("OPENAI"),
                eq("gpt-5-nano"), eq("monthly-report-ai-v10"), anyString())).thenReturn(1);
        when(provider.generate(any())).thenReturn(MonthlyAiAnalysisDto.builder()
                .yearMonth("2026-07")
                .feedbacks(List.of("feedback"))
                .savingsAnalogy("saved")
                .build());
        when(stateService.complete(eq(1L), eq("2026-07"), anyString(), any())).thenReturn(1);

        MonthlyAiAnalysisDto result = service.generateUsingPreparedSnapshot(1L, "2026-07");

        assertEquals("COMPLETED", result.getStatus());
        verify(provider).generate(any());
        verifyNoInteractions(snapshotService);
    }
}
