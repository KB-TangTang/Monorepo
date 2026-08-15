package com.kb.tangtang.report.service;

import com.kb.tangtang.common.dev.DevEnvironmentGuard;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.dto.ChallengeMonthlyReportBatchRunDto;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

/** 시연과 로컬 검증을 위해 선택한 월의 개인 챌린지 확정 배치를 실행한다. */
@Service
public class DevChallengeReportService {

    private final DevEnvironmentGuard guard;
    private final ChallengeMonthlyReportBatchService batchService;

    public DevChallengeReportService(DevEnvironmentGuard guard,
                                     ChallengeMonthlyReportBatchService batchService) {
        this.guard = guard;
        this.batchService = batchService;
    }

    public ChallengeMonthlyReportBatchRunDto runMonthlyBatch(String rawYearMonth) {
        guard.ensureLocal();
        YearMonth yearMonth = parseYearMonth(rawYearMonth);
        return new ChallengeMonthlyReportBatchRunDto(
                yearMonth.toString(), batchService.finalizeReports(yearMonth));
    }

    private YearMonth parseYearMonth(String rawYearMonth) {
        try {
            return YearMonth.parse(rawYearMonth);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw new BusinessException("INVALID_YEAR_MONTH", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }
}
