package com.kb.tangtang.report.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.MonthlyReportBatchCandidate;
import com.kb.tangtang.report.mapper.MonthlyReportBatchMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

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
    private final Clock clock;
    private final int maxAutoAttempts;
    private final int retryDelayMinutes;

    @Autowired
    public MonthlyReportBatchService(
            MonthlyReportBatchMapper batchMapper,
            MonthlyAiAnalysisSnapshotService snapshotService,
            MonthlyAiAnalysisService aiAnalysisService,
            @Value("${report.monthly.zone:Asia/Seoul}") String zoneId,
            @Value("${report.monthly.batch.max-auto-attempts:3}") int maxAutoAttempts,
            @Value("${report.monthly.batch.retry-delay-minutes:20}") int retryDelayMinutes) {
        this(batchMapper, snapshotService, aiAnalysisService, Clock.system(ZoneId.of(zoneId)),
                maxAutoAttempts, retryDelayMinutes);
    }

    MonthlyReportBatchService(MonthlyReportBatchMapper batchMapper,
                              MonthlyAiAnalysisSnapshotService snapshotService,
                              MonthlyAiAnalysisService aiAnalysisService,
                              Clock clock,
                              int maxAutoAttempts,
                              int retryDelayMinutes) {
        this.batchMapper = batchMapper;
        this.snapshotService = snapshotService;
        this.aiAnalysisService = aiAnalysisService;
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
        String yearMonth = targetMonth.toString();
        List<MonthlyReportBatchCandidate> candidates = batchMapper.findEligibleCandidates(
                yearMonth,
                targetMonth.plusMonths(1).atDay(1).atStartOfDay(),
                maxAutoAttempts,
                now.minusMinutes(retryDelayMinutes));

        int completed = 0;
        for (MonthlyReportBatchCandidate candidate : candidates) {
            Long userId = candidate.getUserId();
            try {
                snapshotService.savePendingSnapshot(userId, yearMonth);
                aiAnalysisService.generateUsingPreparedSnapshot(userId, yearMonth);
                completed++;
            } catch (BusinessException exception) {
                log.warn("월간 리포트 배치 건너뜀. userId={}, yearMonth={}, code={}",
                        userId, yearMonth, exception.getCode());
            } catch (RuntimeException exception) {
                log.error("월간 리포트 배치 실패. userId={}, yearMonth={}", userId, yearMonth, exception);
            }
        }
        log.info("월간 리포트 배치 완료. yearMonth={}, candidates={}, completed={}",
                yearMonth, candidates.size(), completed);
    }
}
