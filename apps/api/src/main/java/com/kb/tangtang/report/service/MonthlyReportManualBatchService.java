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
import java.util.List;
import java.util.Map;

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

        MonthlyReportBatchRunResult result = batchService.runManualBatch(
                yearMonth, request.isForce(), toConsentOverrides(request.getMissingSnapshotAiConsents()));
        return new MonthlyReportBatchRunDto(
                yearMonth.toString(), result.getTargetCount(), result.getSnapshotSavedCount(),
                result.getAiGeneratedCount(), result.getFailureCount(), request.isForce());
    }

    private Map<Long, Boolean> toConsentOverrides(List<MonthlyReportAiConsentOverrideDto> overrides) {
        Map<Long, Boolean> result = new HashMap<>();
        if (overrides == null) {
            return result;
        }
        for (MonthlyReportAiConsentOverrideDto override : overrides) {
            if (override == null || override.getUserId() == null || override.getAiUsageConsented() == null
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
