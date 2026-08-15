package com.kb.tangtang.report.service;

import com.kb.tangtang.report.dto.ChallengeReportMonthDto;
import com.kb.tangtang.report.dto.ChallengeReportMonthsDto;
import com.kb.tangtang.report.mapper.ChallengeReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@Service
public class ChallengeReportService {

    private static final String ENTRY_STATE_NOT_AGREED = "NOT_AGREED";
    private static final String ENTRY_STATE_PREPARING_FIRST_REPORT = "PREPARING_FIRST_REPORT";
    private static final String ENTRY_STATE_READY = "READY";

    private final ChallengeReportMapper challengeReportMapper;
    private final Clock clock;

    public ChallengeReportService(ChallengeReportMapper challengeReportMapper) {
        this(challengeReportMapper, Clock.system(ZoneId.of("Asia/Seoul")));
    }

    @Autowired
    public ChallengeReportService(ChallengeReportMapper challengeReportMapper,
                                  @Value("${report.monthly.zone:Asia/Seoul}") String zoneId) {
        this(challengeReportMapper, Clock.system(ZoneId.of(zoneId)));
    }

    ChallengeReportService(ChallengeReportMapper challengeReportMapper, Clock clock) {
        this.challengeReportMapper = challengeReportMapper;
        this.clock = clock;
    }

    /**
     * 당월은 확정 전이므로 절대 반환하지 않는다.
     * 첫 확정 리포트도 READY 상태로 내려 화면은 일반 리포트를 보이고,
     * firstReport 만으로 전월 비교 영역을 숨긴다.
     */
    @Transactional(readOnly = true)
    public ChallengeReportMonthsDto getAvailableMonths(long userId) {
        if (!challengeReportMapper.hasActiveChallengeConsent(userId, java.time.LocalDateTime.now(clock))) {
            return ChallengeReportMonthsDto.builder()
                    .entryState(ENTRY_STATE_NOT_AGREED)
                    .months(List.of())
                    .build();
        }

        YearMonth currentMonth = YearMonth.now(clock);
        List<YearMonth> confirmedMonths = challengeReportMapper
                .findConfirmedReportMonths(userId, currentMonth.toString())
                .stream()
                .map(YearMonth::parse)
                .toList();
        if (confirmedMonths.isEmpty()) {
            String firstMissionMonth = challengeReportMapper.findFirstMissionMonth(userId, currentMonth.atDay(1));
            String preparingMonth = firstMissionMonth != null
                    ? firstMissionMonth
                    : challengeReportMapper.findChallengeConsentMonth(userId, currentMonth.atDay(1));
            if (preparingMonth != null) {
                YearMonth yearMonth = YearMonth.parse(preparingMonth);
                return ChallengeReportMonthsDto.builder()
                        .entryState(ENTRY_STATE_PREPARING_FIRST_REPORT)
                        .months(List.of(toPreparingMonth(yearMonth)))
                        .build();
            }
            return ChallengeReportMonthsDto.builder()
                    .entryState(ENTRY_STATE_PREPARING_FIRST_REPORT)
                    .months(List.of())
                    .build();
        }

        YearMonth firstReportMonth = confirmedMonths.get(confirmedMonths.size() - 1);
        List<ChallengeReportMonthDto> months = confirmedMonths.stream()
                .map(yearMonth -> ChallengeReportMonthDto.builder()
                        .value(yearMonth.toString())
                        .year(yearMonth.getYear())
                        .month(yearMonth.getMonthValue())
                        .available(true)
                        .hasReport(true)
                        .firstReport(yearMonth.equals(firstReportMonth))
                        .status(ENTRY_STATE_READY)
                        .build())
                .toList();
        return ChallengeReportMonthsDto.builder()
                .entryState(ENTRY_STATE_READY)
                .months(months)
                .build();
    }

    private ChallengeReportMonthDto toPreparingMonth(YearMonth yearMonth) {
        return ChallengeReportMonthDto.builder()
                .value(yearMonth.toString())
                .year(yearMonth.getYear())
                .month(yearMonth.getMonthValue())
                .available(false)
                .hasReport(false)
                .firstReport(false)
                .status(ENTRY_STATE_PREPARING_FIRST_REPORT)
                .build();
    }
}
