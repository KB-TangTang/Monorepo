package com.kb.tangtang.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyReportSnapshotContent;
import com.kb.tangtang.report.domain.MonthlyReportSnapshotRow;
import com.kb.tangtang.report.mapper.MonthlyReportMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 월간 리포트 화면과 AI 분석에 필요한 비-AI 스냅샷을 저장한다.
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
        saveSnapshot(userId, yearMonth, aiUsageConsented, false);
    }

    /** force=false는 기존 스냅샷을 보존하고, force=true만 화면·AI 스냅샷을 재생성한다. */
    @Transactional
    public void saveSnapshot(long userId, String yearMonth, boolean aiUsageConsented, boolean force) {
        MonthlyReportSnapshotRow existing = monthlyReportMapper.findMonthlyReportSnapshot(userId, yearMonth);
        if (!force && existing != null) {
            return;
        }

        MonthlyReportSnapshotContent content = monthlyReportService.buildLiveSnapshot(
                userId, yearMonth, aiUsageConsented);
        BigDecimal totalAsset = zeroIfNull(monthlyReportMapper.sumActiveTotalAssets(userId));
        BigDecimal totalDebt = zeroIfNull(monthlyReportMapper.sumLoanBalances(userId));

        try {
            String snapshotJson = objectMapper.writeValueAsString(toSnapshotPayload(content));
            if (force) {
                monthlyReportMapper.overwriteMonthlyReportSnapshot(
                        userId, yearMonth, totalAsset, totalDebt, totalAsset.subtract(totalDebt), snapshotJson,
                        aiUsageConsented ? STATUS_NOT_REQUESTED : STATUS_NOT_CONSENTED);
            } else {
                monthlyReportMapper.insertMonthlyReportSnapshotIfAbsent(
                        userId, yearMonth, totalAsset, totalDebt, totalAsset.subtract(totalDebt), snapshotJson,
                        aiUsageConsented ? STATUS_NOT_REQUESTED : STATUS_NOT_CONSENTED);
            }
        } catch (JsonProcessingException ex) {
            throw new BusinessException("AI_ANALYSIS_SNAPSHOT_UNAVAILABLE",
                    "월간 분석 스냅샷을 준비하지 못했어요. 잠시 후 다시 시도해 주세요.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Map<String, Object> toSnapshotPayload(MonthlyReportSnapshotContent content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("snapshotVersion", MonthlyReportSnapshotReader.SNAPSHOT_VERSION);
        payload.put("aiUsageConsented", content.isAiUsageConsented());
        payload.put("summary", content.getSummary());
        payload.put("spendingTrend", content.getSpendingTrend());
        payload.put("categoryReport", content.getCategoryReport());
        return payload;
    }
}
