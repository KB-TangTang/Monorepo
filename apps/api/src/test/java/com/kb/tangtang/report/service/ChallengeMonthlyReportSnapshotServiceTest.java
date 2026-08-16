package com.kb.tangtang.report.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.report.domain.ChallengeMonthlyDifficultyPolicy;
import com.kb.tangtang.report.domain.ChallengeMonthlyMissionRow;
import com.kb.tangtang.report.domain.ChallengeMonthlyReportSnapshot;
import com.kb.tangtang.report.mapper.ChallengeReportMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChallengeMonthlyReportSnapshotServiceTest {

    @Mock private ChallengeReportMapper mapper;

    @Test
    void finalizesRelativeAndAbsoluteMissionsWithMonthOnlyCalendarWeeks() throws Exception {
        when(mapper.findFinalizedMissionRows(7L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(
                        row("2026-08-01", "SUCCESS", "RELATIVE", "EASY", 10, true, 1L, "카페", 10000, 7000),
                        row("2026-08-02", "FAIL", "ABSOLUTE", "NORMAL", 20, false, 1L, "카페", 10000, 9000),
                        row("2026-08-03", "SUCCESS", "ABSOLUTE", "EASY", 10, false, 2L, "배달", 20000, 8000),
                        row("2026-08-04", "SUCCESS", "RELATIVE", "HARD", 30, true, 2L, "배달", 20000, 15000),
                        row("2026-08-05", "FAIL", "RELATIVE", "HARD", 30, false, 2L, "배달", 20000, 25000),
                        row("2026-08-10", "SUCCESS", "ABSOLUTE", "NORMAL", 20, false, 1L, "카페", 10000, 10000)
                ));
        when(mapper.findDifficultyPolicies()).thenReturn(List.of(
                policy("EASY"), policy("NORMAL"), policy("HARD")));

        ChallengeMonthlyReportSnapshotService service = new ChallengeMonthlyReportSnapshotService(
                mapper, new ObjectMapper());

        service.finalizeUserMonth(7L, YearMonth.of(2026, 8), LocalDateTime.of(2026, 9, 1, 0, 20));

        ArgumentCaptor<ChallengeMonthlyReportSnapshot> captor =
                ArgumentCaptor.forClass(ChallengeMonthlyReportSnapshot.class);
        verify(mapper).upsertMonthlyReport(captor.capture());
        ChallengeMonthlyReportSnapshot snapshot = captor.getValue();
        assertEquals(6, snapshot.getTotalDays());
        assertEquals(4, snapshot.getSuccessDays());
        assertEquals(2, snapshot.getMonthlyLongestStreak());
        assertEquals("월요일", snapshot.getBestWeekday());
        assertEquals(80, snapshot.getEarnedScore());
        assertEquals(new BigDecimal("20000"), snapshot.getSavedAmount());
        assertEquals(new BigDecimal("5000"), snapshot.getOverspentAmount());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode weeks = objectMapper.readTree(snapshot.getWeeklyResultsJson());
        assertEquals(6, weeks.size());
        assertEquals(2, weeks.get(0).get("totalDays").asInt());
        assertEquals(1, weeks.get(0).get("successDays").asInt());
        assertEquals(50.00, weeks.get(0).get("successRate").asDouble());
        assertEquals(3, weeks.get(1).get("totalDays").asInt());
        assertEquals(2, weeks.get(1).get("successDays").asInt());

        JsonNode difficulties = objectMapper.readTree(snapshot.getDifficultyResultsJson());
        assertEquals("EASY", difficulties.get(0).get("difficultyName").asText());
        assertEquals(2, difficulties.get(0).get("attempts").asInt());
        assertEquals(2, difficulties.get(0).get("successDays").asInt());

        JsonNode categories = objectMapper.readTree(snapshot.getCategoryEffectsJson());
        assertEquals(2, categories.size());
        assertEquals("카페", categories.get(0).get("categoryName").asText());
        assertEquals(2, categories.get(0).get("successfulDays").asInt());
        assertEquals(0, categories.get(0).get("overspentAmount").asInt());
        assertEquals("배달", categories.get(1).get("categoryName").asText());
        assertEquals(1, categories.get(1).get("failedDays").asInt());
        assertEquals(5000, categories.get(1).get("overspentAmount").asInt());
    }

    @Test
    void skipsPersistenceWhenThereAreNoFinalizedMissions() {
        when(mapper.findFinalizedMissionRows(any(Long.class), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(List.of());
        ChallengeMonthlyReportSnapshotService service = new ChallengeMonthlyReportSnapshotService(
                mapper, new ObjectMapper());

        service.finalizeUserMonth(7L, YearMonth.of(2026, 8), LocalDateTime.now());

        verify(mapper, org.mockito.Mockito.never()).upsertMonthlyReport(any());
    }

    @Test
    void startsFirstReportWeeklyResultsFromChallengeStartDate() throws Exception {
        when(mapper.findFinalizedMissionRows(7L, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)))
                .thenReturn(List.of(row("2026-08-15", "SUCCESS", "ABSOLUTE", "EASY", 10, false,
                        1L, "카페", 10000, 7000)));
        when(mapper.findFirstMissionDate(7L, LocalDate.of(2026, 8, 31)))
                .thenReturn(LocalDate.of(2026, 8, 15));
        when(mapper.findDifficultyPolicies()).thenReturn(List.of(policy("EASY")));
        ChallengeMonthlyReportSnapshotService service = new ChallengeMonthlyReportSnapshotService(
                mapper, new ObjectMapper());

        service.finalizeUserMonth(7L, YearMonth.of(2026, 8), LocalDateTime.of(2026, 9, 1, 0, 20));

        ArgumentCaptor<ChallengeMonthlyReportSnapshot> captor =
                ArgumentCaptor.forClass(ChallengeMonthlyReportSnapshot.class);
        verify(mapper).upsertMonthlyReport(captor.capture());
        JsonNode weeks = new ObjectMapper().readTree(captor.getValue().getWeeklyResultsJson());
        assertEquals(4, weeks.size());
        assertEquals(3, weeks.get(0).get("week").asInt());
        assertEquals(1, weeks.get(0).get("totalDays").asInt());
        assertEquals(1, weeks.get(0).get("successDays").asInt());
        assertEquals(6, weeks.get(3).get("week").asInt());
    }

    private ChallengeMonthlyMissionRow row(String assignDate, String result, String missionType,
                                           String difficultyName, int difficultyScore,
                                           boolean previousSuccess, long categoryId, String categoryName,
                                           int baseAmount, int actualAmount) {
        ChallengeMonthlyMissionRow row = new ChallengeMonthlyMissionRow();
        row.setAssignDate(LocalDate.parse(assignDate));
        row.setResult(result);
        row.setMissionType(missionType);
        row.setDifficultyName(difficultyName);
        row.setDifficultyScore(difficultyScore);
        row.setPreviousSuccess(previousSuccess);
        row.setCategoryId(categoryId);
        row.setCategoryName(categoryName);
        row.setBaseAmount(BigDecimal.valueOf(baseAmount));
        row.setActualAmount(BigDecimal.valueOf(actualAmount));
        return row;
    }

    private ChallengeMonthlyDifficultyPolicy policy(String difficultyName) {
        ChallengeMonthlyDifficultyPolicy policy = new ChallengeMonthlyDifficultyPolicy();
        policy.setDifficultyName(difficultyName);
        return policy;
    }
}
