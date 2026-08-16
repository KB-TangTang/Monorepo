package com.kb.tangtang.report.service;

import com.kb.tangtang.report.mapper.ChallengeReportMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

/** 익월 1일에 전월 개인 챌린지 성과와 확정 그룹 전적을 사용자별로 저장한다. */
@Service
@Log4j2
public class ChallengeMonthlyReportBatchService {

    private final ChallengeReportMapper mapper;
    private final ChallengeMonthlyReportSnapshotService snapshotService;
    private final Clock clock;

    @Autowired
    public ChallengeMonthlyReportBatchService(
            ChallengeReportMapper mapper,
            ChallengeMonthlyReportSnapshotService snapshotService,
            @Value("${challenge.report.monthly.zone:Asia/Seoul}") String zoneId) {
        this(mapper, snapshotService, Clock.system(ZoneId.of(zoneId)));
    }

    ChallengeMonthlyReportBatchService(ChallengeReportMapper mapper,
                                       ChallengeMonthlyReportSnapshotService snapshotService,
                                       Clock clock) {
        this.mapper = mapper;
        this.snapshotService = snapshotService;
        this.clock = clock;
    }

    public void finalizePreviousMonthReports() {
        LocalDateTime now = LocalDateTime.now(clock).withNano(0);
        finalizeReports(YearMonth.from(now).minusMonths(1), false, now);
    }

    /**
     * end_date가 전월 말일인 그룹은 최종 확정 시점이 익월 2일이다.
     * 개인 챌린지 스냅샷은 건드리지 않고 해당 사용자의 group_record_json만 다시 집계한다.
     */
    public void refreshPreviousMonthEndGroupRecords() {
        refreshMonthEndGroupRecords(YearMonth.now(clock).minusMonths(1));
    }

    public int refreshMonthEndGroupRecords(YearMonth targetMonth) {
        List<Long> userIds = mapper.findMonthEndClosedGroupReportUserIds(
                targetMonth.atEndOfMonth(), targetMonth.toString());
        int completed = 0;
        for (Long userId : userIds) {
            try {
                snapshotService.refreshUserMonthGroupRecord(userId, targetMonth);
                completed++;
            } catch (RuntimeException exception) {
                log.error("월말 종료 그룹 전적 보강 실패. userId={}, yearMonth={}",
                        userId, targetMonth, exception);
            }
        }
        log.info("월말 종료 그룹 전적 보강 완료. yearMonth={}, candidates={}, completed={}",
                targetMonth, userIds.size(), completed);
        return completed;
    }

    public int finalizeReports(YearMonth targetMonth) {
        return finalizeReports(targetMonth, false);
    }

    public int finalizeReports(YearMonth targetMonth, boolean includeExisting) {
        return finalizeReports(targetMonth, includeExisting, LocalDateTime.now(clock).withNano(0));
    }

    int finalizeReports(YearMonth targetMonth, boolean includeExisting, LocalDateTime finalizedAt) {
        List<Long> userIds = mapper.findFinalizedReportUserIds(
                targetMonth.atDay(1), targetMonth.atEndOfMonth(), targetMonth.toString(), includeExisting);
        int completed = 0;
        for (Long userId : userIds) {
            try {
                snapshotService.finalizeUserMonth(userId, targetMonth, finalizedAt);
                completed++;
            } catch (RuntimeException exception) {
                log.error("챌린지 월 확정 실패. userId={}, yearMonth={}",
                        userId, targetMonth, exception);
            }
        }
        log.info("챌린지 월 확정 완료. yearMonth={}, includeExisting={}, candidates={}, completed={}",
                targetMonth, includeExisting, userIds.size(), completed);
        return completed;
    }
}
