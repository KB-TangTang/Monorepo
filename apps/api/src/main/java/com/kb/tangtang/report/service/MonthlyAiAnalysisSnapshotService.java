package com.kb.tangtang.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.dto.MonthlyCategoryReportDto;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * 미생성·실패 상태에서만 월간 AI 분석에 필요한 비-AI 스냅샷을 저장한다.
 */
@Service
public class MonthlyAiAnalysisSnapshotService {

    private static final String STATUS_NOT_REQUESTED = "NOT_REQUESTED";
    private static final String STATUS_NOT_CONSENTED = "NOT_CONSENTED";

    private final MonthlyReportService monthlyReportService;
    private final MonthlyReportMapper monthlyReportMapper;
    private final ObjectMapper objectMapper;

    public MonthlyAiAnalysisSnapshotService(MonthlyReportService monthlyReportService,
                                            MonthlyReportMapper monthlyReportMapper,
                                            ObjectMapper objectMapper) {
        this.monthlyReportService = monthlyReportService;
        this.monthlyReportMapper = monthlyReportMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void saveSnapshot(long userId, String yearMonth, boolean aiUsageConsented) {
        MonthlyCategoryReportDto categorySummary = monthlyReportService.getCategories(userId, yearMonth);
        BigDecimal totalAsset = zeroIfNull(monthlyReportMapper.sumActiveTotalAssets(userId));
        BigDecimal totalDebt = zeroIfNull(monthlyReportMapper.sumLoanBalances(userId));

        try {
            monthlyReportMapper.upsertPendingAiAnalysisSnapshot(
                    userId,
                    yearMonth,
                    totalAsset,
                    totalDebt,
                    totalAsset.subtract(totalDebt),
                    objectMapper.writeValueAsString(categorySummary),
                    aiUsageConsented ? STATUS_NOT_REQUESTED : STATUS_NOT_CONSENTED);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("AI_ANALYSIS_SNAPSHOT_UNAVAILABLE",
                    "월간 분석 스냅샷을 준비하지 못했어요. 잠시 후 다시 시도해 주세요.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
