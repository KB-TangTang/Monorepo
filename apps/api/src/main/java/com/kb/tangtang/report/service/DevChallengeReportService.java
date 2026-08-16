package com.kb.tangtang.report.service;

import com.kb.tangtang.common.dev.DevEnvironmentGuard;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.dto.ChallengeMonthlyReportBatchRunDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

/** 시연과 로컬 검증을 위해 선택한 월의 개인 챌린지 확정 배치를 실행한다. */
@Service
public class DevChallengeReportService {

    private final DevEnvironmentGuard guard;
    private final ChallengeMonthlyReportBatchService batchService;
    private final Clock clock;

    @Autowired
    public DevChallengeReportService(DevEnvironmentGuard guard,
                                     ChallengeMonthlyReportBatchService batchService,
                                     @Value("${challenge.report.monthly.zone:Asia/Seoul}") String zoneId) {
        this(guard, batchService, Clock.system(ZoneId.of(zoneId)));
    }

    DevChallengeReportService(DevEnvironmentGuard guard,
                              ChallengeMonthlyReportBatchService batchService,
                              Clock clock) {
        this.guard = guard;
        this.batchService = batchService;
        this.clock = clock;
    }

    public ChallengeMonthlyReportBatchRunDto runMonthlyBatch(String rawYearMonth, boolean force) {
        guard.ensureLocal();
        YearMonth yearMonth = parseYearMonth(rawYearMonth);
        if (!yearMonth.isBefore(YearMonth.now(clock))) {
            throw new BusinessException("CHALLENGE_REPORT_NOT_AVAILABLE",
                    "개발용 월 확정 배치는 지난달까지 실행할 수 있습니다.");
        }
        return new ChallengeMonthlyReportBatchRunDto(
                yearMonth.toString(), batchService.finalizeReports(yearMonth, force), force);
    }

    /** 로컬 검증 시 기존 개인 스냅샷을 보존하고 월말 종료 그룹 전적만 다시 집계한다. */
    public ChallengeMonthlyReportBatchRunDto runMonthlyGroupRecordBatch(String rawYearMonth) {
        guard.ensureLocal();
        YearMonth yearMonth = parseYearMonth(rawYearMonth);
        if (!yearMonth.isBefore(YearMonth.now(clock))) {
            throw new BusinessException("CHALLENGE_REPORT_NOT_AVAILABLE",
                    "개발용 그룹 전적 보강 배치는 지난달까지 실행할 수 있습니다.");
        }
        return new ChallengeMonthlyReportBatchRunDto(
                yearMonth.toString(), batchService.refreshMonthEndGroupRecords(yearMonth), false);
    }

    private YearMonth parseYearMonth(String rawYearMonth) {
        try {
            return YearMonth.parse(rawYearMonth);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new BusinessException("INVALID_YEAR_MONTH", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }
}
