package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

class RelativeMissionAssignmentSchedulerTest {

    @Test
    void scheduledRecoveryDelegatesToBatchService() {
        RelativeMissionAssignmentBatchService batchService = mock(RelativeMissionAssignmentBatchService.class);
        MissionEvaluationBatchService evaluationBatchService = mock(MissionEvaluationBatchService.class);
        RelativeMissionAssignmentMapper assignmentMapper = mock(RelativeMissionAssignmentMapper.class);
        RelativeMissionAssignmentScheduler scheduler =
                new RelativeMissionAssignmentScheduler(
                        batchService, evaluationBatchService, assignmentMapper, "Asia/Seoul");

        scheduler.recoverMissingDailyMissions();

        verify(batchService).assignDailyMissions(any(LocalDate.class));
        verify(assignmentMapper).findEarliestRecoveryDateBefore(any(LocalDate.class));
    }

    @Test
    void startupRecoveryRunsOnlyOnceWhenContextRefreshesMoreThanOnce() {
        RelativeMissionAssignmentBatchService batchService = mock(RelativeMissionAssignmentBatchService.class);
        MissionEvaluationBatchService evaluationBatchService = mock(MissionEvaluationBatchService.class);
        RelativeMissionAssignmentMapper assignmentMapper = mock(RelativeMissionAssignmentMapper.class);
        RelativeMissionAssignmentScheduler scheduler =
                new RelativeMissionAssignmentScheduler(
                        batchService, evaluationBatchService, assignmentMapper, "Asia/Seoul");

        scheduler.recoverMissingDailyMissionsOnStartup();
        scheduler.recoverMissingDailyMissionsOnStartup();

        verify(batchService, times(1)).assignDailyMissions(any(LocalDate.class));
        verify(assignmentMapper, times(1)).findEarliestRecoveryDateBefore(any(LocalDate.class));
    }

    @Test
    void evaluatesYesterdayBeforeAssigningTodayMission() {
        RelativeMissionAssignmentBatchService assignmentBatch = mock(RelativeMissionAssignmentBatchService.class);
        MissionEvaluationBatchService evaluationBatch = mock(MissionEvaluationBatchService.class);
        RelativeMissionAssignmentMapper assignmentMapper = mock(RelativeMissionAssignmentMapper.class);
        RelativeMissionAssignmentScheduler scheduler =
                new RelativeMissionAssignmentScheduler(
                        assignmentBatch, evaluationBatch, assignmentMapper, "Asia/Seoul");

        scheduler.assignDailyMissions();

        var order = inOrder(evaluationBatch, assignmentBatch);
        order.verify(evaluationBatch).evaluateDailyMissions(any(LocalDate.class), any());
        order.verify(assignmentBatch).assignDailyMissions(any(LocalDate.class));
    }

    @Test
    void recoversMissingDatesOldestFirstBeforeAssigningToday() {
        RelativeMissionAssignmentBatchService assignmentBatch = mock(RelativeMissionAssignmentBatchService.class);
        MissionEvaluationBatchService evaluationBatch = mock(MissionEvaluationBatchService.class);
        RelativeMissionAssignmentMapper assignmentMapper = mock(RelativeMissionAssignmentMapper.class);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Seoul"));
        LocalDate firstMissingDate = today.minusDays(2);
        when(assignmentMapper.findEarliestRecoveryDateBefore(today)).thenReturn(firstMissingDate);
        RelativeMissionAssignmentScheduler scheduler = new RelativeMissionAssignmentScheduler(
                assignmentBatch, evaluationBatch, assignmentMapper, "Asia/Seoul");

        scheduler.recoverMissingDailyMissions();

        var order = inOrder(assignmentBatch, evaluationBatch);
        order.verify(assignmentBatch).assignDailyMissions(firstMissingDate, false);
        order.verify(evaluationBatch).evaluateDailyMissions(
                firstMissingDate, firstMissingDate.plusDays(1).atStartOfDay(), false);
        order.verify(assignmentBatch).assignDailyMissions(firstMissingDate.plusDays(1), false);
        order.verify(evaluationBatch).evaluateDailyMissions(
                firstMissingDate.plusDays(1), today.atStartOfDay(), false);
        order.verify(assignmentBatch).assignDailyMissions(today);
    }
}
