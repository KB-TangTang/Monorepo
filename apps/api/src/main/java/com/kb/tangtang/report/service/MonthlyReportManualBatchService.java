package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyReportBatchRunResult;
import com.kb.tangtang.report.dto.MonthlyReportAiConsentOverrideDto;
import com.kb.tangtang.report.dto.MonthlyReportBatchRunDto;
import com.kb.tangtang.report.dto.MonthlyReportManualBatchRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** 배포·로컬에서 운영 키로 실행하는 월간 소비 리포트 강제 배치다. */
@Service
public class MonthlyReportManualBatchService {

    private final MonthlyReportManualBatchAccessGuard accessGuard;
    private final MonthlyReportBatchService batchService;
    private final Clock clock;

    @Autowired
    public MonthlyReportManualBatchService(MonthlyReportManualBatchAccessGuard accessGuard,
                                           MonthlyReportBatchService batchService,
                                           @Value("${report.monthly.zone:Asia/Seoul}") String zoneId) {
        this(accessGuard, batchService, Clock.system(ZoneId.of(zoneId)));
    }

    MonthlyReportManualBatchService(MonthlyReportManualBatchAccessGuard accessGuard,
                                    MonthlyReportBatchService batchService,
                                    Clock clock) {
        this.accessGuard = accessGuard;
        this.batchService = batchService;
        this.clock = clock;
    }

    public MonthlyReportBatchRunDto run(String operationKey, MonthlyReportManualBatchRequestDto request) {
        accessGuard.ensureAuthorized(operationKey);
        if (request == null) {
            throw new BusinessException("INVALID_REQUEST", "월간 리포트 배치 요청 본문이 필요합니다.");
        }

        YearMonth yearMonth = parseYearMonth(request.getYearMonth());
        if (!yearMonth.isBefore(YearMonth.now(clock))) {
            throw new BusinessException("REPORT_NOT_AVAILABLE",
                    "월간 리포트 배치는 지난달까지 실행할 수 있습니다.");
        }

        Set<Long> targetUserIds = toTargetUserIds(request.isForce(), request.getTargetUserIds());
        MonthlyReportBatchRunResult result = batchService.runManualBatch(
                yearMonth, request.isForce(), targetUserIds,
                toConsentOverrides(request.getMissingSnapshotAiConsents(), targetUserIds));
        return new MonthlyReportBatchRunDto(
                yearMonth.toString(), result.getTargetCount(), result.getSnapshotSavedCount(),
                result.getAiGeneratedCount(), result.getFailureCount(), request.isForce());
    }

    private Set<Long> toTargetUserIds(boolean force, List<Long> targetUserIds) {
        if (!force) {
            if (targetUserIds != null && !targetUserIds.isEmpty()) {
                throw new BusinessException("INVALID_REQUEST",
                        "targetUserIds는 force=true 수동 배치에서만 사용할 수 있습니다.");
            }
            return Set.of();
        }
        if (targetUserIds == null || targetUserIds.isEmpty()) {
            throw new BusinessException("INVALID_REQUEST",
                    "force=true 수동 배치에는 targetUserIds가 필요합니다.");
        }
        Set<Long> result = new HashSet<>();
        for (Long userId : targetUserIds) {
            if (userId == null || userId <= 0 || !result.add(userId)) {
                throw new BusinessException("INVALID_REQUEST",
                        "targetUserIds에는 중복 없는 양수 사용자 ID만 넣을 수 있습니다.");
            }
        }
        return result;
    }

    private Map<Long, Boolean> toConsentOverrides(List<MonthlyReportAiConsentOverrideDto> overrides,
                                                   Set<Long> targetUserIds) {
        Map<Long, Boolean> result = new HashMap<>();
        if (overrides == null) {
            return result;
        }
        for (MonthlyReportAiConsentOverrideDto override : overrides) {
            if (override == null || override.getUserId() == null || override.getAiUsageConsented() == null
                    || !targetUserIds.contains(override.getUserId())
                    || result.putIfAbsent(override.getUserId(), override.getAiUsageConsented()) != null) {
                throw new BusinessException("INVALID_REQUEST",
                        "missingSnapshotAiConsents에는 중복 없는 사용자별 AI 동의값이 필요합니다.");
            }
        }
        return result;
    }

    private YearMonth parseYearMonth(String rawYearMonth) {
        try {
            return YearMonth.parse(rawYearMonth);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new BusinessException("INVALID_YEAR_MONTH", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }
}
