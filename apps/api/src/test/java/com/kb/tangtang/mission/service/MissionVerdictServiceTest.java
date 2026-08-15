package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.domain.MissionVerdictRow;
import com.kb.tangtang.mission.dto.MissionVerdictAcknowledgeDto;
import com.kb.tangtang.mission.dto.MissionVerdictDto;
import com.kb.tangtang.mission.dto.WeeklyMissionResultDto;
import com.kb.tangtang.mission.mapper.MissionVerdictMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MissionVerdictServiceTest {

    private static final long USER_ID = 7L;
    private static final long ASSIGNMENT_ID = 123L;
    private static final LocalDate ASSIGN_DATE = LocalDate.of(2026, 8, 13);
    private static final LocalDateTime CHECKED_AT = LocalDateTime.of(2026, 8, 14, 9, 10);

    @Mock MissionVerdictMapper mapper;
    MissionVerdictService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-14T00:10:00Z"), ZoneId.of("Asia/Seoul"));
        service = new MissionVerdictService(mapper, clock);
    }

    @Test
    void returnsNullWhenThereIsNoUncheckedVerdict() {
        when(mapper.findOldestUncheckedVerdict(USER_ID, ASSIGN_DATE)).thenReturn(null);

        assertNull(service.getPendingVerdict(USER_ID));
    }

    @Test
    void buildsSuccessVerdictFromEvaluationData() {
        MissionVerdictRow row = verdictRow("SUCCESS", "9800", "12000", true);
        when(mapper.findOldestUncheckedVerdict(USER_ID, ASSIGN_DATE)).thenReturn(row);
        when(mapper.findResultsThroughDate(USER_ID, ASSIGN_DATE)).thenReturn(List.of(
                result(ASSIGN_DATE, "SUCCESS"),
                result(ASSIGN_DATE.minusDays(1), "SUCCESS"),
                result(ASSIGN_DATE.minusDays(2), "FAIL")));
        when(mapper.countUncheckedVerdicts(USER_ID, ASSIGN_DATE)).thenReturn(1);
        when(mapper.findVerdictTransactions(ASSIGNMENT_ID, USER_ID)).thenReturn(List.of());

        MissionVerdictDto verdict = service.getPendingVerdict(USER_ID);

        assertEquals("SUCCESS", verdict.getResult());
        assertEquals(new BigDecimal("2200"), verdict.getRemainAmount());
        assertEquals(BigDecimal.ZERO, verdict.getOverAmount());
        assertEquals(35, verdict.getPoints());
        assertEquals(5, verdict.getBonusPoints());
        assertEquals(2, verdict.getStreakDays());
        assertEquals(0, verdict.getPendingCount());
    }

    @Test
    void buildsFailVerdictWithoutPointsAndStopsStreak() {
        MissionVerdictRow row = verdictRow("FAIL", "15200", "12000", true);
        when(mapper.findOldestUncheckedVerdict(USER_ID, ASSIGN_DATE)).thenReturn(row);
        when(mapper.findResultsThroughDate(USER_ID, ASSIGN_DATE))
                .thenReturn(List.of(result(ASSIGN_DATE, "FAIL")));
        when(mapper.countUncheckedVerdicts(USER_ID, ASSIGN_DATE)).thenReturn(1);
        when(mapper.findVerdictTransactions(ASSIGNMENT_ID, USER_ID)).thenReturn(List.of());

        MissionVerdictDto verdict = service.getPendingVerdict(USER_ID);

        assertEquals(BigDecimal.ZERO, verdict.getRemainAmount());
        assertEquals(new BigDecimal("3200"), verdict.getOverAmount());
        assertEquals(0, verdict.getPoints());
        assertEquals(0, verdict.getBonusPoints());
        assertEquals(0, verdict.getStreakDays());
    }

    @Test
    void acknowledgesFinalizedVerdictAndReturnsStoredTimestamp() {
        when(mapper.countOwnedFinalizedVerdict(ASSIGNMENT_ID, USER_ID)).thenReturn(1);
        when(mapper.findResultCheckedAt(ASSIGNMENT_ID, USER_ID)).thenReturn(CHECKED_AT);

        MissionVerdictAcknowledgeDto result = service.acknowledge(USER_ID, ASSIGNMENT_ID);

        verify(mapper).acknowledgeVerdict(ASSIGNMENT_ID, USER_ID, CHECKED_AT);
        assertEquals(CHECKED_AT, result.getResultCheckedAt());
    }

    @Test
    void rejectsMissingForeignOrPendingVerdict() {
        when(mapper.countOwnedFinalizedVerdict(ASSIGNMENT_ID, USER_ID)).thenReturn(0);

        BusinessException error = assertThrows(
                BusinessException.class, () -> service.acknowledge(USER_ID, ASSIGNMENT_ID));

        assertEquals("NOT_FOUND", error.getCode());
    }

    private MissionVerdictRow verdictRow(
            String result, String currentAmount, String targetValue, boolean previousSuccess) {
        MissionVerdictRow row = new MissionVerdictRow();
        row.setAssignmentId(ASSIGNMENT_ID);
        row.setResult(result);
        row.setAssignDate(ASSIGN_DATE);
        row.setCategoryName("배달앱");
        row.setCurrentAmount(new BigDecimal(currentAmount));
        row.setTargetValue(new BigDecimal(targetValue));
        row.setBasePoints(35);
        row.setPreviousSuccess(previousSuccess);
        return row;
    }

    private WeeklyMissionResultDto result(LocalDate date, String result) {
        WeeklyMissionResultDto dto = new WeeklyMissionResultDto();
        dto.setDate(date);
        dto.setResult(result);
        return dto;
    }
}
