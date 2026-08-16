package com.kb.tangtang.report.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.report.domain.ChallengeMonthlyReportRow;
import com.kb.tangtang.report.dto.ChallengeDifficultyResultDto;
import com.kb.tangtang.report.dto.ChallengeCategoryEffectDto;
import com.kb.tangtang.report.dto.ChallengeReportDetailDto;
import com.kb.tangtang.report.dto.ChallengeReportMonthDto;
import com.kb.tangtang.report.dto.ChallengeReportMonthsDto;
import com.kb.tangtang.report.dto.ChallengeWeeklyResultDto;
import com.kb.tangtang.report.dto.GroupRecordDto;
import com.kb.tangtang.report.dto.GroupRecordState;
import com.kb.tangtang.report.mapper.ChallengeReportMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.YearMonth;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class ChallengeReportService {

    private static final String ENTRY_STATE_NOT_AGREED = "NOT_AGREED";
    private static final String ENTRY_STATE_PREPARING_FIRST_REPORT = "PREPARING_FIRST_REPORT";
    private static final String ENTRY_STATE_READY = "READY";

    private final ChallengeReportMapper challengeReportMapper;
    private final Clock clock;
    private final ObjectMapper objectMapper;
    private final ChallengeMonthlyReportSnapshotService snapshotService;

    public ChallengeReportService(ChallengeReportMapper challengeReportMapper) {
        this(challengeReportMapper, Clock.system(ZoneId.of("Asia/Seoul")), new ObjectMapper(), null);
    }

    @Autowired
    public ChallengeReportService(ChallengeReportMapper challengeReportMapper,
                                  ObjectMapper objectMapper,
                                  ChallengeMonthlyReportSnapshotService snapshotService,
                                  @Value("${report.monthly.zone:Asia/Seoul}") String zoneId) {
        this(challengeReportMapper, Clock.system(ZoneId.of(zoneId)), objectMapper, snapshotService);
    }

    ChallengeReportService(ChallengeReportMapper challengeReportMapper, Clock clock) {
        this(challengeReportMapper, clock, new ObjectMapper(), null);
    }

    ChallengeReportService(ChallengeReportMapper challengeReportMapper, Clock clock,
                           ObjectMapper objectMapper) {
        this(challengeReportMapper, clock, objectMapper, null);
    }

    ChallengeReportService(ChallengeReportMapper challengeReportMapper, Clock clock,
                           ObjectMapper objectMapper,
                           ChallengeMonthlyReportSnapshotService snapshotService) {
        this.challengeReportMapper = challengeReportMapper;
        this.clock = clock;
        this.objectMapper = objectMapper;
        this.snapshotService = snapshotService;
    }

    /**
     * 당월은 확정 전이므로 절대 반환하지 않는다.
     * 첫 확정 리포트도 READY 상태로 내려 화면은 일반 리포트를 보이고,
     * firstReport 만으로 전월 비교 영역을 숨긴다.
     */
    @Transactional
    public ChallengeReportMonthsDto getAvailableMonths(long userId) {
        if (!challengeReportMapper.hasActiveChallengeConsent(userId, java.time.LocalDateTime.now(clock))) {
            return ChallengeReportMonthsDto.builder()
                    .entryState(ENTRY_STATE_NOT_AGREED)
                    .months(List.of())
                    .build();
        }

        YearMonth currentMonth = YearMonth.now(clock);
        recoverMissingPreviousMonthReport(userId, currentMonth);
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

    private void recoverMissingPreviousMonthReport(long userId, YearMonth currentMonth) {
        if (snapshotService == null) {
            return;
        }
        YearMonth previousMonth = currentMonth.minusMonths(1);
        if (challengeReportMapper.findMonthlyReport(userId, previousMonth.toString()) != null) {
            return;
        }
        snapshotService.finalizeUserMonth(userId, previousMonth, LocalDateTime.now(clock).withNano(0));
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

    @Transactional(readOnly = true)
    public ChallengeReportDetailDto getReport(long userId, String rawYearMonth) {
        YearMonth yearMonth = parsePastYearMonth(rawYearMonth);
        LocalDateTime now = LocalDateTime.now(clock);
        if (!challengeReportMapper.hasActiveChallengeConsent(userId, now)) {
            throw new BusinessException("CHALLENGE_REPORT_NOT_AVAILABLE",
                    "챌린지 리포트를 조회하려면 활성 챌린지 동의가 필요합니다.");
        }

        ChallengeMonthlyReportRow report = challengeReportMapper.findMonthlyReport(userId, yearMonth.toString());
        if (report == null) {
            throw new BusinessException("CHALLENGE_REPORT_NOT_FOUND", "확정된 챌린지 리포트를 찾을 수 없습니다.");
        }

        ChallengeMonthlyReportRow previousReport = challengeReportMapper
                .findPreviousMonthlyReport(userId, yearMonth.toString());
        BigDecimal successRate = calculateRate(report.getSuccessDays(), report.getTotalDays());
        boolean hasPreviousComparison = previousReport != null;
        BigDecimal percentagePoint = hasPreviousComparison
                ? successRate.subtract(calculateRate(previousReport.getSuccessDays(), previousReport.getTotalDays()))
                .setScale(2, RoundingMode.HALF_UP)
                : null;
        GroupRecordDto groupRecord = readGroupRecord(report.getGroupRecordJson());
        GroupRecordState groupRecordState = resolveGroupRecordState(
                groupRecord,
                challengeReportMapper.hasJudgingGroupRecord(
                        userId, yearMonth.atDay(1), yearMonth.atEndOfMonth()));

        return ChallengeReportDetailDto.builder()
                .period(report.getYearMonth())
                .hasChallengeHistory(true)
                .isFirstServiceMonth(!hasPreviousComparison)
                .hasPreviousComparison(hasPreviousComparison)
                .missionSuccessRate(successRate)
                .monthOverMonthPercentagePoint(percentagePoint)
                .successfulDays(report.getSuccessDays())
                .challengeDays(report.getTotalDays())
                .bestStreakDays(report.getMonthlyLongestStreak())
                .bestWeekday(report.getBestWeekday())
                .earnedPoints(report.getEarnedScore())
                .savedAmount(amount(report.getSavedAmount()))
                .overspentAmount(amount(report.getOverspentAmount()))
                .netSavings(amount(report.getNetAmount()))
                .annualizedNetSavings(amount(report.getNetAmount()).multiply(BigDecimal.valueOf(12)))
                .categoryEffects(readCategoryEffects(report.getCategoryEffectsJson()))
                .weeklyResults(readWeeklyResults(report.getWeeklyResultsJson()))
                .difficulties(readDifficultyResults(report.getDifficultyResultsJson()))
                .groupRecordState(groupRecordState)
                .groupRecord(groupRecordState == GroupRecordState.READY ? groupRecord : null)
                .build();
    }

    private GroupRecordState resolveGroupRecordState(GroupRecordDto groupRecord,
                                                     boolean hasJudgingGroupRecord) {
        if (hasJudgingGroupRecord) {
            return GroupRecordState.JUDGING;
        }
        return hasGroupRecord(groupRecord) ? GroupRecordState.READY : GroupRecordState.EMPTY;
    }

    private boolean hasGroupRecord(GroupRecordDto groupRecord) {
        return groupRecord != null && groupRecord.getParticipatingGroups() > 0;
    }

    private YearMonth parsePastYearMonth(String rawYearMonth) {
        if (rawYearMonth == null || rawYearMonth.trim().isEmpty()) {
            throw new BusinessException("INVALID_YEAR_MONTH", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
        try {
            YearMonth yearMonth = YearMonth.parse(rawYearMonth);
            if (!yearMonth.isBefore(YearMonth.now(clock))) {
                throw new BusinessException("CHALLENGE_REPORT_NOT_AVAILABLE",
                        "챌린지 리포트는 지난달까지 조회할 수 있습니다.");
            }
            return yearMonth;
        } catch (DateTimeParseException exception) {
            throw new BusinessException("INVALID_YEAR_MONTH", "yearMonth는 YYYY-MM 형식이어야 합니다.");
        }
    }

    private List<ChallengeWeeklyResultDto> readWeeklyResults(String rawJson) {
        return readJson(rawJson, new TypeReference<List<ChallengeWeeklyResultDto>>() { });
    }

    private List<ChallengeDifficultyResultDto> readDifficultyResults(String rawJson) {
        return readJson(rawJson, new TypeReference<List<ChallengeDifficultyResultDto>>() { });
    }

    private List<ChallengeCategoryEffectDto> readCategoryEffects(String rawJson) {
        return readJson(rawJson, new TypeReference<List<ChallengeCategoryEffectDto>>() { });
    }

    private GroupRecordDto readGroupRecord(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(rawJson, GroupRecordDto.class);
        } catch (Exception exception) {
            throw new BusinessException("CHALLENGE_REPORT_UNREADABLE",
                    "저장된 챌린지 리포트를 읽을 수 없습니다.");
        }
    }

    private <T> List<T> readJson(String rawJson, TypeReference<List<T>> type) {
        if (rawJson == null || rawJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(rawJson, type);
        } catch (Exception exception) {
            throw new BusinessException("CHALLENGE_REPORT_UNREADABLE",
                    "저장된 챌린지 리포트를 읽을 수 없습니다.");
        }
    }

    private BigDecimal calculateRate(int successDays, int totalDays) {
        if (totalDays == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(successDays)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
