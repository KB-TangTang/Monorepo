package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.mapper.MissionEvaluationMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MissionEvaluationBatchServiceTest {

    @Test
    void suppressesVerdictNotificationsForRecoveryEvaluation() {
        MissionEvaluationMapper mapper = mock(MissionEvaluationMapper.class);
        MissionEvaluationService evaluationService = mock(MissionEvaluationService.class);
        MissionEvaluationBatchService service =
                new MissionEvaluationBatchService(mapper, evaluationService);
        LocalDate assignDate = LocalDate.of(2026, 8, 12);
        LocalDateTime evaluatedAt = LocalDateTime.of(2026, 8, 13, 0, 0);
        when(mapper.findPendingAssignmentIds(assignDate)).thenReturn(List.of(11L, 12L));

        service.evaluateDailyMissions(assignDate, evaluatedAt, false);

        verify(evaluationService).evaluate(11L, evaluatedAt, false);
        verify(evaluationService).evaluate(12L, evaluatedAt, false);
    }

    @Test
    void evaluatesAllPastPendingMissionsForUser() {
        MissionEvaluationMapper mapper = mock(MissionEvaluationMapper.class);
        MissionEvaluationService evaluationService = mock(MissionEvaluationService.class);
        MissionEvaluationBatchService service =
                new MissionEvaluationBatchService(mapper, evaluationService);
        LocalDate today = LocalDate.of(2026, 8, 14);
        LocalDateTime evaluatedAt = LocalDateTime.of(2026, 8, 14, 9, 10);
        when(mapper.findPendingAssignmentIdsBefore(7L, today)).thenReturn(List.of(11L, 12L));

        service.evaluatePendingMissionsBefore(7L, today, evaluatedAt);

        verify(evaluationService).evaluate(11L, evaluatedAt, true);
        verify(evaluationService).evaluate(12L, evaluatedAt, true);
    }
}
