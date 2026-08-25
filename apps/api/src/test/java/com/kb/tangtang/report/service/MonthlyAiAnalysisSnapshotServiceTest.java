package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.report.dto.MonthlyCategoryItemDto;
import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.dto.MonthlyParentCategoryItemDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import com.kb.tangtang.report.domain.MonthlyReportSnapshotContent;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyAiAnalysisSnapshotServiceTest {

    private static final long USER_ID = 7L;

    @Mock
    private MonthlyReportService monthlyReportService;

    @Mock
    private MonthlyReportMapper mapper;

    @Test
    void savesAllNonAiSnapshotFieldsAndTheCategoryReportJsonShape() throws Exception {
        MonthlyCategoryReportDto categoryReport = MonthlyCategoryReportDto.builder()
                .yearMonth("2026-07")
                .totalSpent(new BigDecimal("3802832"))
                .parentCategories(List.of(MonthlyParentCategoryItemDto.builder()
                        .categoryId(1L)
                        .categoryName("Food")
                        .amount(new BigDecimal("671083"))
                        .ratio(new BigDecimal("17.65"))
                        .build()))
                .categories(List.of(MonthlyCategoryItemDto.builder()
                        .parentCategoryId(1L)
                        .parentCategoryName("Food")
                        .categoryId(13L)
                        .categoryName("Restaurant")
                        .amount(new BigDecimal("174649"))
                        .ratio(new BigDecimal("4.59"))
                        .previousMonthAmount(new BigDecimal("136374"))
                        .changeRate(new BigDecimal("28.07"))
                        .build()))
                .build();
        when(monthlyReportService.buildLiveSnapshot(USER_ID, "2026-07", true)).thenReturn(
                new MonthlyReportSnapshotContent(
                        true,
                        MonthlySummaryDto.builder()
                                .yearMonth("2026-07")
                                .totalSpent(new BigDecimal("3802832"))
                                .previousMonthSpent(new BigDecimal("3900000"))
                                .hasPreviousComparison(true)
                                .monthOverMonthRate(new BigDecimal("-2.49"))
                                .fixedExpenseCandidateCount(2)
                                .confirmedFixedExpenseCount(1)
                                .build(),
                        MonthlySpendingTrendDto.builder().yearMonth("2026-07").items(List.of()).build(),
                        categoryReport));
        when(mapper.sumActiveTotalAssets(USER_ID)).thenReturn(new BigDecimal("16000000"));
        when(mapper.sumLoanBalances(USER_ID)).thenReturn(new BigDecimal("2500000"));

        MonthlyAiAnalysisSnapshotService service = new MonthlyAiAnalysisSnapshotService(
                monthlyReportService, mapper, new ObjectMapper());

        service.saveSnapshot(USER_ID, "2026-07", true);

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(mapper).insertMonthlyReportSnapshotIfAbsent(
                eq(USER_ID), eq("2026-07"), eq(new BigDecimal("16000000")),
                eq(new BigDecimal("2500000")), eq(new BigDecimal("13500000")), jsonCaptor.capture(),
                eq("NOT_REQUESTED"));
        JsonNode saved = new ObjectMapper().readTree(jsonCaptor.getValue());
        assertEquals(2, saved.get("snapshotVersion").asInt());
        assertTrue(saved.get("aiUsageConsented").asBoolean());
        assertEquals("2026-07", saved.get("summary").get("yearMonth").asText());
        assertEquals(3802832, saved.get("categoryReport").get("totalSpent").asInt());
        assertEquals("Food", saved.get("categoryReport").get("parentCategories").get(0)
                .get("categoryName").asText());
        assertEquals(13, saved.get("categoryReport").get("categories").get(0)
                .get("categoryId").asInt());
        assertEquals(136374, saved.get("categoryReport").get("categories").get(0)
                .get("previousMonthAmount").asInt());
        assertTrue(saved.get("categoryReport").get("categories").get(0).has("changeRate"));
    }

    @Test
    void savesNotConsentedStatusWithoutAiCall() {
        when(monthlyReportService.buildLiveSnapshot(USER_ID, "2026-07", false)).thenReturn(
                new MonthlyReportSnapshotContent(
                        false,
                        MonthlySummaryDto.builder().yearMonth("2026-07").totalSpent(BigDecimal.ZERO).build(),
                        MonthlySpendingTrendDto.builder().yearMonth("2026-07").items(List.of()).build(),
                        MonthlyCategoryReportDto.builder().yearMonth("2026-07").totalSpent(BigDecimal.ZERO)
                                .parentCategories(List.of()).categories(List.of()).build()));
        MonthlyAiAnalysisSnapshotService service = new MonthlyAiAnalysisSnapshotService(
                monthlyReportService, mapper, new ObjectMapper());

        service.saveSnapshot(USER_ID, "2026-07", false);

        verify(mapper).insertMonthlyReportSnapshotIfAbsent(
                eq(USER_ID), eq("2026-07"), eq(BigDecimal.ZERO), eq(BigDecimal.ZERO), eq(BigDecimal.ZERO),
                org.mockito.ArgumentMatchers.contains("\"snapshotVersion\":2"), eq("NOT_CONSENTED"));
    }
}
