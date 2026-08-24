package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyReportBatchCandidate;
import com.kb.tangtang.report.domain.MonthlyReportBatchRunResult;
import com.kb.tangtang.report.domain.MonthlyReportForceBatchCandidate;
import com.kb.tangtang.report.domain.MonthlyReportSnapshotContent;
import com.kb.tangtang.report.mapper.MonthlyReportBatchMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

/**
 * 이전 달 월간 리포트 스냅샷과 AI 분석을 사용자 단위로 생성한다.
 *
 * <p>배치 전체에는 트랜잭션을 걸지 않는다. 스냅샷 저장과 AI 상태 전이는 각각의 서비스 경계에서
 * 처리하므로 한 사용자의 외부 AI 실패가 다른 사용자의 생성을 되돌리지 않는다.</p>
 */
@Service
@Log4j2
public class MonthlyReportBatchService {

    private final MonthlyReportBatchMapper batchMapper;
    private final MonthlyAiAnalysisSnapshotService snapshotService;
    private final MonthlyAiAnalysisService aiAnalysisService;
    private final MonthlyReportSnapshotReader snapshotReader;
    private final Clock clock;
    private final int maxAutoAttempts;
    private final int retryDelayMinutes;

    @Autowired
    public MonthlyReportBatchService(
            MonthlyReportBatchMapper batchMapper,
            MonthlyAiAnalysisSnapshotService snapshotService,
            MonthlyAiAnalysisService aiAnalysisService,
            MonthlyReportSnapshotReader snapshotReader,
            @Value("${report.monthly.zone:Asia/Seoul}") String zoneId,
            @Value("${report.monthly.batch.max-auto-attempts:3}") int maxAutoAttempts,
            @Value("${report.monthly.batch.retry-delay-minutes:20}") int retryDelayMinutes) {
        this(batchMapper, snapshotService, aiAnalysisService, snapshotReader, Clock.system(ZoneId.of(zoneId)),
                maxAutoAttempts, retryDelayMinutes);
    }

    MonthlyReportBatchService(MonthlyReportBatchMapper batchMapper,
                              MonthlyAiAnalysisSnapshotService snapshotService,
                              MonthlyAiAnalysisService aiAnalysisService,
                              Clock clock,
                              int maxAutoAttempts,
                              int retryDelayMinutes) {
        this(batchMapper, snapshotService, aiAnalysisService,
                new MonthlyReportSnapshotReader(new ObjectMapper()), clock,
                maxAutoAttempts, retryDelayMinutes);
    }

    MonthlyReportBatchService(MonthlyReportBatchMapper batchMapper,
                              MonthlyAiAnalysisSnapshotService snapshotService,
                              MonthlyAiAnalysisService aiAnalysisService,
                              MonthlyReportSnapshotReader snapshotReader,
                              Clock clock,
                              int maxAutoAttempts,
                              int retryDelayMinutes) {
        this.batchMapper = batchMapper;
        this.snapshotService = snapshotService;
        this.aiAnalysisService = aiAnalysisService;
        this.snapshotReader = snapshotReader;
        this.clock = clock;
        this.maxAutoAttempts = maxAutoAttempts;
        this.retryDelayMinutes = retryDelayMinutes;
    }

    /** 현재 기준 이전 달만 생성한다. 과거 월을 자동 보정하지 않는다. */
    public void generatePreviousMonthReports() {
        LocalDateTime now = LocalDateTime.now(clock);
        generateReports(YearMonth.from(now).minusMonths(1), now);
    }

    void generateReports(YearMonth targetMonth, LocalDateTime now) {
        generateEligibleReports(targetMonth, now);
    }

    /** 운영 키가 검증된 수동 API에서만 과거 월 전체를 강제로 재생성한다. */
    public MonthlyReportBatchRunResult runManualBatch(YearMonth targetMonth,
                                                       boolean force,
                                                       Map<Long, Boolean> missingSnapshotAiConsents) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!force) {
            return generateEligibleReports(targetMonth, now);
        }

        List<MonthlyReportForceBatchCandidate> candidates = batchMapper.findForceBatchCandidates(
                targetMonth.toString(), targetMonth.plusMonths(1).atDay(1).atStartOfDay());
        return processCandidates(resolveForceCandidates(candidates, missingSnapshotAiConsents), targetMonth, true);
    }

    private MonthlyReportBatchRunResult generateEligibleReports(YearMonth targetMonth, LocalDateTime now) {
        String yearMonth = targetMonth.toString();
        List<MonthlyReportBatchCandidate> candidates = batchMapper.findEligibleCandidates(
                yearMonth,
                targetMonth.plusMonths(1).atDay(1).atStartOfDay(),
                maxAutoAttempts,
                now.minusMinutes(retryDelayMinutes));

        List<ResolvedBatchCandidate> resolved = candidates.stream()
                .map(candidate -> new ResolvedBatchCandidate(candidate.getUserId(), candidate.isAiUsageConsented()))
                .toList();
        MonthlyReportBatchRunResult result = processCandidates(resolved, targetMonth, false);
        log.info("월간 리포트 배치 완료. yearMonth={}, candidates={}, snapshots={}, aiCompleted={}, failures={}",
                yearMonth, result.getTargetCount(), result.getSnapshotSavedCount(), result.getAiGeneratedCount(),
                result.getFailureCount());
        return result;
    }

    private MonthlyReportBatchRunResult processCandidates(List<ResolvedBatchCandidate> candidates,
                                                           YearMonth targetMonth,
                                                           boolean force) {
        int snapshotSaved = 0;
        int aiGenerated = 0;
        int failures = 0;
        String yearMonth = targetMonth.toString();
        for (ResolvedBatchCandidate candidate : candidates) {
            Long userId = candidate.userId;
            try {
                snapshotService.saveSnapshot(userId, yearMonth, candidate.aiUsageConsented, force);
                snapshotSaved++;
                if (!candidate.aiUsageConsented) {
                    continue;
                }
                aiAnalysisService.generateUsingPreparedSnapshot(userId, yearMonth);
                aiGenerated++;
            } catch (BusinessException exception) {
                failures++;
                log.warn("월간 리포트 배치 건너뜀. userId={}, yearMonth={}, code={}",
                        userId, yearMonth, exception.getCode());
            } catch (RuntimeException exception) {
                failures++;
                log.error("월간 리포트 배치 실패. userId={}, yearMonth={}", userId, yearMonth, exception);
            }
        }
        return new MonthlyReportBatchRunResult(candidates.size(), snapshotSaved, aiGenerated, failures);
    }

    private List<ResolvedBatchCandidate> resolveForceCandidates(
            List<MonthlyReportForceBatchCandidate> candidates,
            Map<Long, Boolean> missingSnapshotAiConsents) {
        List<Long> unresolvedUserIds = new ArrayList<>();
        List<ResolvedBatchCandidate> resolved = new ArrayList<>();
        for (MonthlyReportForceBatchCandidate candidate : candidates) {
            MonthlyReportSnapshotContent content = snapshotReader.read(candidate.getCategorySummaryJson());
            if (content != null) {
                resolved.add(new ResolvedBatchCandidate(candidate.getUserId(), content.isAiUsageConsented()));
                continue;
            }

            Boolean aiUsageConsented = missingSnapshotAiConsents.get(candidate.getUserId());
            if (aiUsageConsented == null) {
                unresolvedUserIds.add(candidate.getUserId());
            } else {
                resolved.add(new ResolvedBatchCandidate(candidate.getUserId(), aiUsageConsented));
            }
        }
        if (!unresolvedUserIds.isEmpty()) {
            throw new BusinessException("MISSING_AI_CONSENT_INPUT",
                    "당시 AI 동의를 확인할 수 없는 사용자 ID의 동의값이 필요합니다: " + unresolvedUserIds);
        }
        return resolved;
    }

    private static class ResolvedBatchCandidate {

        private final Long userId;
        private final boolean aiUsageConsented;

        private ResolvedBatchCandidate(Long userId, boolean aiUsageConsented) {
            this.userId = userId;
            this.aiUsageConsented = aiUsageConsented;
        }
    }
}
