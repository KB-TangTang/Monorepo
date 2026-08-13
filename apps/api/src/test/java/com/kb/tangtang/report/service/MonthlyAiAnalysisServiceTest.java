package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisInput;
import com.kb.tangtang.report.domain.MonthlyAiAnalysisSnapshot;
import com.kb.tangtang.report.dto.MonthlyAiAnalysisDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyAiAnalysisServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private MonthlyReportMapper mapper;

    @Mock
    private MonthlyAiAnalysisProvider provider;

    @Mock
    private MonthlyAiAnalysisStateService stateService;

    private MonthlyAiAnalysisService service;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        Clock clock = Clock.fixed(LocalDate.of(2026, 8, 12).atStartOfDay(zoneId).toInstant(), zoneId);
        service = new MonthlyAiAnalysisService(mapper, provider, stateService, new ObjectMapper(), clock);
        when(mapper.findUserCreatedDate(USER_ID)).thenReturn(LocalDate.of(2026, 3, 15));
    }

    @Test
    @DisplayName("완료된 월은 외부 AI를 다시 호출하지 않고 저장된 결과를 재사용한다")
    void reusesCompletedResult() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L,
                        "[\"식비 지출을 점검해 보세요.\"]",
                        "이번달 아낀 128,000원은 카페라떼 26잔", "COMPLETED"));

        MonthlyAiAnalysisDto result = service.generate(USER_ID, "2026-07");

        assertEquals(List.of("식비 지출을 점검해 보세요."), result.getFeedbacks());
        assertEquals("이번달 아낀 128,000원은 카페라떼 26잔", result.getSavingsAnalogy());
        verifyNoInteractions(provider);
        verify(stateService, never()).claim(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("생성 성공 결과를 JSON 피드백과 단일 절약 비유로 스냅샷에 저장한다")
    void savesGeneratedResult() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L, null, null, "NOT_REQUESTED"));
        when(mapper.sumNetSpending(eq(USER_ID), any(), any()))
                .thenReturn(new BigDecimal("1284000"), new BigDecimal("1412000"));
        when(mapper.findMonthlyCategorySpending(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(stateService.claim(eq(USER_ID), eq("2026-07"), eq("OPENAI"),
                eq("gpt-5-nano"), eq("monthly-report-ai-v7"), any())).thenReturn(1);
        when(provider.generate(any())).thenReturn(MonthlyAiAnalysisDto.builder()
                .yearMonth("2026-07")
                .feedbacks(List.of("고정지출을 제외한 소비가 줄었어요."))
                .savingsAnalogy("이번달 아낀 128,000원은 치킨 5마리")
                .build());
        when(stateService.complete(USER_ID, "2026-07",
                "[\"고정지출을 제외한 소비가 줄었어요.\"]",
                "이번달 아낀 128,000원은 치킨 5마리")).thenReturn(1);

        MonthlyAiAnalysisDto result = service.generate(USER_ID, "2026-07");

        assertEquals("2026-07", result.getYearMonth());
        ArgumentCaptor<MonthlyAiAnalysisInput> inputCaptor = ArgumentCaptor.forClass(MonthlyAiAnalysisInput.class);
        verify(provider).generate(inputCaptor.capture());
        MonthlyAiAnalysisInput input = inputCaptor.getValue();
        assertEquals(new BigDecimal("1284000"), input.getCurrentMonthSpent());
        assertEquals(new BigDecimal("1412000"), input.getPreviousMonthSpent());
        assertEquals(new BigDecimal("128000"), input.getSavingsAmount());
        assertFalse(new ObjectMapper().valueToTree(input).has("userId"));
        assertFalse(new ObjectMapper().valueToTree(input).has("merchantName"));
        assertFalse(new ObjectMapper().valueToTree(input).has("accountNumber"));
        verify(stateService).complete(USER_ID, "2026-07",
                "[\"고정지출을 제외한 소비가 줄었어요.\"]",
                "이번달 아낀 128,000원은 치킨 5마리");
    }

    @Test
    @DisplayName("다른 요청이 생성 중이면 409로 막고 외부 AI를 호출하지 않는다")
    void rejectsInProgressGeneration() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L, null, null, "IN_PROGRESS"));
        when(mapper.sumNetSpending(eq(USER_ID), any(), any()))
                .thenReturn(new BigDecimal("1284000"), new BigDecimal("1412000"));
        when(mapper.findMonthlyCategorySpending(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(stateService.claim(eq(USER_ID), eq("2026-07"), eq("OPENAI"),
                eq("gpt-5-nano"), eq("monthly-report-ai-v7"), any())).thenReturn(0);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.generate(USER_ID, "2026-07"));

        assertEquals("AI_ANALYSIS_IN_PROGRESS", exception.getCode());
        assertEquals(HttpStatus.CONFLICT, exception.getHttpStatus());
        verifyNoInteractions(provider);
    }

    @Test
    @DisplayName("AI 호출 제한은 실패 상태를 저장하고 429로 전달한다")
    void savesFailureWhenProviderRateLimited() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L, null, null, "NOT_REQUESTED"));
        when(mapper.sumNetSpending(eq(USER_ID), any(), any()))
                .thenReturn(new BigDecimal("1284000"), new BigDecimal("1412000"));
        when(mapper.findMonthlyCategorySpending(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(stateService.claim(eq(USER_ID), eq("2026-07"), eq("OPENAI"),
                eq("gpt-5-nano"), eq("monthly-report-ai-v7"), any())).thenReturn(1);
        when(provider.generate(any())).thenThrow(new AiProviderException("TOO_MANY_REQUESTS",
                "AI 요청이 많아요.", HttpStatus.TOO_MANY_REQUESTS));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.generate(USER_ID, "2026-07"));

        assertEquals("TOO_MANY_REQUESTS", exception.getCode());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.getHttpStatus());
        verify(provider).generate(any());
        verify(stateService).fail(USER_ID, "2026-07", "TOO_MANY_REQUESTS");
    }

    @Test
    @DisplayName("가입 첫 달은 전월 비교 없이 절약 비유를 만들지 않는다")
    void doesNotCreateSavingsAnalogyForFirstReport() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-03"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L, null, null, "NOT_REQUESTED"));
        when(mapper.sumNetSpending(eq(USER_ID), any(), any())).thenReturn(new BigDecimal("50000"));
        when(mapper.findMonthlyCategorySpending(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(stateService.claim(eq(USER_ID), eq("2026-03"), eq("OPENAI"),
                eq("gpt-5-nano"), eq("monthly-report-ai-v7"), any())).thenReturn(1);
        when(provider.generate(any())).thenReturn(MonthlyAiAnalysisDto.builder()
                .yearMonth("2026-03")
                .feedbacks(List.of("이번 달 소비 흐름을 확인해 보세요."))
                .savingsAnalogy(null)
                .build());
        when(stateService.complete(eq(USER_ID), eq("2026-03"), any(), nullable(String.class))).thenReturn(1);

        service.generate(USER_ID, "2026-03");

        ArgumentCaptor<MonthlyAiAnalysisInput> inputCaptor = ArgumentCaptor.forClass(MonthlyAiAnalysisInput.class);
        verify(provider).generate(inputCaptor.capture());
        assertFalse(inputCaptor.getValue().isHasPreviousComparison());
        assertNull(inputCaptor.getValue().getPreviousMonthSpent());
        assertEquals(BigDecimal.ZERO, inputCaptor.getValue().getSavingsAmount());
        verify(stateService).complete(USER_ID, "2026-03",
                "[\"이번 달 소비 흐름을 확인해 보세요.\"]", null);
    }

    @Test
    @DisplayName("전월보다 소비가 늘거나 같거나 전월 소비가 0원이면 절감액은 0원이다")
    void usesZeroSavingsWhenNoPositiveReductionExists() {
        assertZeroSavings(new BigDecimal("130000"), new BigDecimal("120000"));
        assertZeroSavings(new BigDecimal("120000"), new BigDecimal("120000"));
        assertZeroSavings(new BigDecimal("120000"), BigDecimal.ZERO);
    }

    @Test
    @DisplayName("FAILED 상태는 다음 수동 요청에서 다시 생성할 수 있다")
    void retriesFailedGeneration() {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L, null, null, "FAILED"));
        when(mapper.sumNetSpending(eq(USER_ID), any(), any()))
                .thenReturn(new BigDecimal("1284000"), new BigDecimal("1412000"));
        when(mapper.findMonthlyCategorySpending(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(stateService.claim(eq(USER_ID), eq("2026-07"), eq("OPENAI"),
                eq("gpt-5-nano"), eq("monthly-report-ai-v7"), any())).thenReturn(1);
        when(provider.generate(any())).thenReturn(MonthlyAiAnalysisDto.builder()
                .yearMonth("2026-07")
                .feedbacks(List.of("이번 달 지출을 점검해 보세요."))
                .savingsAnalogy("이번달 아낀 128,000원은 치킨 5마리")
                .build());
        when(stateService.complete(eq(USER_ID), eq("2026-07"), any(), any())).thenReturn(1);

        service.generate(USER_ID, "2026-07");

        verify(stateService).claim(eq(USER_ID), eq("2026-07"), eq("OPENAI"),
                eq("gpt-5-nano"), eq("monthly-report-ai-v7"), any());
        verify(provider).generate(any());
    }

    private void assertZeroSavings(BigDecimal currentMonthSpent, BigDecimal previousMonthSpent) {
        when(mapper.findAiAnalysisSnapshot(USER_ID, "2026-07"))
                .thenReturn(new MonthlyAiAnalysisSnapshot(1L, null, null, "NOT_REQUESTED"));
        when(mapper.sumNetSpending(eq(USER_ID), any(), any()))
                .thenReturn(currentMonthSpent, previousMonthSpent);
        when(mapper.findMonthlyCategorySpending(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(stateService.claim(eq(USER_ID), eq("2026-07"), eq("OPENAI"),
                eq("gpt-5-nano"), eq("monthly-report-ai-v7"), any())).thenReturn(1);
        when(provider.generate(any())).thenReturn(MonthlyAiAnalysisDto.builder()
                .yearMonth("2026-07")
                .feedbacks(List.of("이번 달 소비 흐름을 확인해 보세요."))
                .savingsAnalogy(null)
                .build());
        when(stateService.complete(eq(USER_ID), eq("2026-07"), any(), nullable(String.class))).thenReturn(1);

        service.generate(USER_ID, "2026-07");

        ArgumentCaptor<MonthlyAiAnalysisInput> inputCaptor = ArgumentCaptor.forClass(MonthlyAiAnalysisInput.class);
        verify(provider, org.mockito.Mockito.atLeastOnce()).generate(inputCaptor.capture());
        assertEquals(BigDecimal.ZERO, inputCaptor.getValue().getSavingsAmount());
    }
}
