package com.kb.tangtang.report.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.report.domain.ChallengeMonthlyDifficultyPolicy;
import com.kb.tangtang.report.domain.ChallengeMonthlyMissionRow;
import com.kb.tangtang.report.domain.ChallengeMonthlyReportSnapshot;
import com.kb.tangtang.report.dto.ChallengeCategoryEffectDto;
import com.kb.tangtang.report.dto.ChallengeDifficultyResultDto;
import com.kb.tangtang.report.dto.ChallengeWeeklyResultDto;
import com.kb.tangtang.report.mapper.ChallengeReportMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 최종 판정된 개인 미션 원장을 월 단위 확정 스냅샷으로 바꾼다. */
@Service
public class ChallengeMonthlyReportSnapshotService {

    private static final String CALCULATION_VERSION = "v2026-08-15-savings";
    private static final int STREAK_BONUS_POINTS = 5;
    private static final String[] KOREAN_WEEKDAYS = {"월", "화", "수", "목", "금", "토", "일"};

    private final ChallengeReportMapper mapper;
    private final ObjectMapper objectMapper;

    public ChallengeMonthlyReportSnapshotService(ChallengeReportMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void finalizeUserMonth(long userId, YearMonth targetMonth, LocalDateTime finalizedAt) {
        List<ChallengeMonthlyMissionRow> rows = mapper.findFinalizedMissionRows(
                userId, targetMonth.atDay(1), targetMonth.atEndOfMonth());
        if (rows.isEmpty()) {
            return;
        }

        LocalDate firstMissionDate = mapper.findFirstMissionDate(userId, targetMonth.atEndOfMonth());
        LocalDate reportStartDate = firstMissionDate != null && firstMissionDate.isAfter(targetMonth.atDay(1))
                ? firstMissionDate : targetMonth.atDay(1);
        List<ChallengeWeeklyResultDto> weeklyResults = calculateWeeklyResults(
                targetMonth, reportStartDate, rows);
        List<ChallengeDifficultyResultDto> difficultyResults = calculateDifficultyResults(
                mapper.findDifficultyPolicies(), rows);
        ChallengeSavingsResult savingsResult = calculateSavings(rows);

        int successDays = (int) rows.stream().filter(this::isSuccess).count();
        ChallengeMonthlyReportSnapshot snapshot = ChallengeMonthlyReportSnapshot.builder()
                .userId(userId)
                .yearMonth(targetMonth.toString())
                .totalDays(rows.size())
                .successDays(successDays)
                .savedAmount(savingsResult.savedAmount)
                .overspentAmount(savingsResult.overspentAmount)
                .categoryEffectsJson(toJson(savingsResult.categoryEffects))
                .monthlyLongestStreak(calculateLongestStreak(rows))
                .bestWeekday(calculateBestWeekday(rows))
                .earnedScore(calculateEarnedScore(rows))
                .weeklyResultsJson(toJson(weeklyResults))
                .difficultyResultsJson(toJson(difficultyResults))
                .calculationVersion(CALCULATION_VERSION)
                .finalizedAt(finalizedAt)
                .build();
        mapper.upsertMonthlyReport(snapshot);
    }

    private List<ChallengeWeeklyResultDto> calculateWeeklyResults(
            YearMonth targetMonth, LocalDate reportStartDate, List<ChallengeMonthlyMissionRow> rows) {
        Map<LocalDate, ChallengeMonthlyMissionRow> rowsByDate = new LinkedHashMap<>();
        for (ChallengeMonthlyMissionRow row : rows) {
            rowsByDate.put(row.getAssignDate(), row);
        }

        List<ChallengeWeeklyResultDto> results = new ArrayList<>();
        LocalDate weekStart = reportStartDate
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate calendarMonthFirstWeekStart = targetMonth.atDay(1)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthEnd = targetMonth.atEndOfMonth();
        int week = Math.toIntExact(ChronoUnit.WEEKS.between(calendarMonthFirstWeekStart, weekStart)) + 1;
        while (!weekStart.isAfter(monthEnd)) {
            int totalDays = 0;
            int successDays = 0;
            for (int offset = 0; offset < 7; offset++) {
                LocalDate date = weekStart.plusDays(offset);
                if (date.isBefore(reportStartDate) || date.isAfter(monthEnd)) {
                    continue;
                }
                ChallengeMonthlyMissionRow row = rowsByDate.get(date);
                if (row == null) {
                    continue;
                }
                totalDays++;
                if (isSuccess(row)) {
                    successDays++;
                }
            }
            results.add(new ChallengeWeeklyResultDto(week++, successDays, totalDays,
                    calculateRate(successDays, totalDays)));
            weekStart = weekStart.plusWeeks(1);
        }
        return results;
    }

    private List<ChallengeDifficultyResultDto> calculateDifficultyResults(
            List<ChallengeMonthlyDifficultyPolicy> policies, List<ChallengeMonthlyMissionRow> rows) {
        Map<String, DifficultyCounter> counters = new LinkedHashMap<>();
        for (ChallengeMonthlyDifficultyPolicy policy : policies) {
            counters.put(policy.getDifficultyName(), new DifficultyCounter());
        }
        for (ChallengeMonthlyMissionRow row : rows) {
            String difficultyName = row.getDifficultyName() == null ? "UNKNOWN" : row.getDifficultyName();
            DifficultyCounter counter = counters.computeIfAbsent(difficultyName, ignored -> new DifficultyCounter());
            counter.attempts++;
            if (isSuccess(row)) {
                counter.successDays++;
            }
        }

        return counters.entrySet().stream()
                .map(entry -> new ChallengeDifficultyResultDto(
                        entry.getKey(), entry.getValue().attempts, entry.getValue().successDays,
                        calculateRate(entry.getValue().successDays, entry.getValue().attempts)))
                .toList();
    }

    /**
     * 기준액은 미션 배정 원장에 고정된 B를 사용한다. 미션 종류가 아니라 B 보유 여부로
     * 집계 대상을 정했으므로, B가 저장된 절대형 미션도 상대형과 같은 규칙으로 포함된다.
     */
    private ChallengeSavingsResult calculateSavings(List<ChallengeMonthlyMissionRow> rows) {
        BigDecimal savedAmount = BigDecimal.ZERO;
        BigDecimal overspentAmount = BigDecimal.ZERO;
        Map<Long, CategoryEffectCounter> categories = new LinkedHashMap<>();

        for (ChallengeMonthlyMissionRow row : rows) {
            BigDecimal baseAmount = amount(row.getBaseAmount());
            BigDecimal actualAmount = amount(row.getActualAmount());
            CategoryEffectCounter category = categories.computeIfAbsent(
                    row.getCategoryId(), ignored -> new CategoryEffectCounter(row.getCategoryId(), row.getCategoryName()));

            if (isSuccess(row)) {
                BigDecimal saved = baseAmount.subtract(actualAmount);
                savedAmount = savedAmount.add(saved);
                category.successfulDays++;
                category.savedAmount = category.savedAmount.add(saved);
                continue;
            }

            BigDecimal overspent = actualAmount.subtract(baseAmount).max(BigDecimal.ZERO);
            overspentAmount = overspentAmount.add(overspent);
            category.failedDays++;
            category.overspentAmount = category.overspentAmount.add(overspent);
        }

        List<ChallengeCategoryEffectDto> categoryEffects = categories.values().stream()
                .map(CategoryEffectCounter::toDto)
                .toList();
        return new ChallengeSavingsResult(savedAmount, overspentAmount, categoryEffects);
    }

    private int calculateLongestStreak(List<ChallengeMonthlyMissionRow> rows) {
        int longest = 0;
        int current = 0;
        LocalDate previousDate = null;
        for (ChallengeMonthlyMissionRow row : rows.stream()
                .sorted(Comparator.comparing(ChallengeMonthlyMissionRow::getAssignDate)).toList()) {
            if (isSuccess(row)) {
                current = previousDate != null && row.getAssignDate().equals(previousDate.plusDays(1))
                        ? current + 1 : 1;
                longest = Math.max(longest, current);
            } else {
                current = 0;
            }
            previousDate = row.getAssignDate();
        }
        return longest;
    }

    private String calculateBestWeekday(List<ChallengeMonthlyMissionRow> rows) {
        Map<DayOfWeek, DifficultyCounter> counters = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek dayOfWeek : DayOfWeek.values()) {
            counters.put(dayOfWeek, new DifficultyCounter());
        }
        for (ChallengeMonthlyMissionRow row : rows) {
            DifficultyCounter counter = counters.get(row.getAssignDate().getDayOfWeek());
            counter.attempts++;
            if (isSuccess(row)) {
                counter.successDays++;
            }
        }

        DayOfWeek bestDay = DayOfWeek.MONDAY;
        for (DayOfWeek candidate : DayOfWeek.values()) {
            if (isBetterWeekday(counters.get(candidate), candidate, counters.get(bestDay), bestDay)) {
                bestDay = candidate;
            }
        }
        return KOREAN_WEEKDAYS[bestDay.getValue() - 1] + "요일";
    }

    private boolean isBetterWeekday(DifficultyCounter candidate, DayOfWeek candidateDay,
                                    DifficultyCounter current, DayOfWeek currentDay) {
        int rateCompare = calculateRate(candidate.successDays, candidate.attempts)
                .compareTo(calculateRate(current.successDays, current.attempts));
        if (rateCompare != 0) {
            return rateCompare > 0;
        }
        if (candidate.attempts != current.attempts) {
            return candidate.attempts > current.attempts;
        }
        return candidateDay.getValue() < currentDay.getValue();
    }

    private int calculateEarnedScore(List<ChallengeMonthlyMissionRow> rows) {
        return rows.stream()
                .filter(this::isSuccess)
                .mapToInt(row -> (row.getDifficultyScore() == null ? 0 : row.getDifficultyScore())
                        + (row.isPreviousSuccess() ? STREAK_BONUS_POINTS : 0))
                .sum();
    }

    private BigDecimal calculateRate(int successDays, int totalDays) {
        if (totalDays == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return BigDecimal.valueOf(successDays)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalDays), 2, RoundingMode.HALF_UP);
    }

    private boolean isSuccess(ChallengeMonthlyMissionRow row) {
        return "SUCCESS".equals(row.getResult());
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("챌린지 월간 성과를 직렬화하지 못했습니다.", exception);
        }
    }

    private static class DifficultyCounter {
        private int attempts;
        private int successDays;
    }

    private static class CategoryEffectCounter {
        private final long categoryId;
        private final String categoryName;
        private int successfulDays;
        private BigDecimal savedAmount = BigDecimal.ZERO;
        private int failedDays;
        private BigDecimal overspentAmount = BigDecimal.ZERO;

        private CategoryEffectCounter(Long categoryId, String categoryName) {
            this.categoryId = categoryId == null ? 0L : categoryId;
            this.categoryName = categoryName;
        }

        private ChallengeCategoryEffectDto toDto() {
            return new ChallengeCategoryEffectDto(
                    categoryId, categoryName, successfulDays, savedAmount, failedDays, overspentAmount);
        }
    }

    private record ChallengeSavingsResult(
            BigDecimal savedAmount,
            BigDecimal overspentAmount,
            List<ChallengeCategoryEffectDto> categoryEffects) {
    }
}
