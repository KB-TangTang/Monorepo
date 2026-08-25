package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyReportSnapshotRow;
import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.dto.MonthlySpendingTrendDto;
import com.kb.tangtang.report.dto.MonthlySummaryDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyReportSnapshotReadServiceTest {

    @Mock
    private MonthlyReportMapper mapper;

    private MonthlyReportService service;

    @BeforeEach
    void setUp() {
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        service = new MonthlyReportService(mapper,
                Clock.fixed(LocalDate.of(2026, 8, 12).atStartOfDay(zoneId).toInstant(), zoneId));
        when(mapper.findUserCreatedDate(7L)).thenReturn(LocalDate.of(2026, 3, 15));
    }

    @Test
    void returnsAllReportSectionsFromVersionTwoSnapshotOnly() {
        when(mapper.findMonthlyReportSnapshot(7L, "2026-07"))
                .thenReturn(new MonthlyReportSnapshotRow(1L, "2026-07", snapshotJson(), "COMPLETED"));

        MonthlySummaryDto summary = service.getSummary(7L, "2026-07");
        MonthlySpendingTrendDto trend = service.getSpendingTrend(7L, "2026-07");
        MonthlyCategoryReportDto categories = service.getCategories(7L, "2026-07");

        assertEquals(1284000, summary.getTotalSpent().intValue());
        assertEquals(2, summary.getFixedExpenseCandidateCount());
        assertEquals(6, trend.getItems().size());
        assertEquals("식비", categories.getParentCategories().get(0).getCategoryName());
        verify(mapper, never()).sumNetSpending(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verify(mapper, never()).findMonthlyCategorySpending(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsLegacyCategoryOnlySnapshotInsteadOfReaggregatingTransactions() {
        when(mapper.findMonthlyReportSnapshot(7L, "2026-07"))
                .thenReturn(new MonthlyReportSnapshotRow(1L, "2026-07", "{\"yearMonth\":\"2026-07\"}",
                        "NOT_REQUESTED"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.getSummary(7L, "2026-07"));

        assertEquals("REPORT_NOT_AVAILABLE", exception.getCode());
        verify(mapper, never()).sumNetSpending(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private String snapshotJson() {
        return "{\"snapshotVersion\":2,\"aiUsageConsented\":true,"
                + "\"summary\":{\"yearMonth\":\"2026-07\",\"totalSpent\":1284000,"
                + "\"previousMonthSpent\":1396000,\"hasPreviousComparison\":true,"
                + "\"monthOverMonthRate\":-8.02,\"fixedExpenseCandidateCount\":2,"
                + "\"confirmedFixedExpenseCount\":3},"
                + "\"spendingTrend\":{\"yearMonth\":\"2026-07\",\"items\":["
                + "{\"yearMonth\":\"2026-02\",\"amount\":null,\"hasData\":false},"
                + "{\"yearMonth\":\"2026-03\",\"amount\":0,\"hasData\":true},"
                + "{\"yearMonth\":\"2026-04\",\"amount\":0,\"hasData\":true},"
                + "{\"yearMonth\":\"2026-05\",\"amount\":0,\"hasData\":true},"
                + "{\"yearMonth\":\"2026-06\",\"amount\":1396000,\"hasData\":true},"
                + "{\"yearMonth\":\"2026-07\",\"amount\":1284000,\"hasData\":true}]},"
                + "\"categoryReport\":{\"yearMonth\":\"2026-07\",\"totalSpent\":1284000,"
                + "\"parentCategories\":[{\"categoryId\":1,\"categoryName\":\"식비\","
                + "\"amount\":400000,\"ratio\":31.15}],\"categories\":[]}}";
    }
}
