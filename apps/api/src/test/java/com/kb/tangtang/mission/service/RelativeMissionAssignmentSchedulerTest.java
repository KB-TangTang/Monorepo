package com.kb.tangtang.mission.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.inOrder;

class RelativeMissionAssignmentSchedulerTest {

    @Test
    void scheduledRecoveryDelegatesToBatchService() {
        RelativeMissionAssignmentBatchService batchService = mock(RelativeMissionAssignmentBatchService.class);
        MissionEvaluationBatchService evaluationBatchService = mock(MissionEvaluationBatchService.class);
        RelativeMissionAssignmentScheduler scheduler =
                new RelativeMissionAssignmentScheduler(batchService, evaluationBatchService, "Asia/Seoul");

        scheduler.recoverMissingDailyMissions();

        verify(batchService).assignDailyMissions(any(LocalDate.class));
        verify(evaluationBatchService).evaluateDailyMissions(any(LocalDate.class), any());
    }

    @Test
    void startupRecoveryRunsOnlyOnceWhenContextRefreshesMoreThanOnce() {
        RelativeMissionAssignmentBatchService batchService = mock(RelativeMissionAssignmentBatchService.class);
        MissionEvaluationBatchService evaluationBatchService = mock(MissionEvaluationBatchService.class);
        RelativeMissionAssignmentScheduler scheduler =
                new RelativeMissionAssignmentScheduler(batchService, evaluationBatchService, "Asia/Seoul");

        scheduler.recoverMissingDailyMissionsOnStartup();
        scheduler.recoverMissingDailyMissionsOnStartup();

        verify(batchService, times(1)).assignDailyMissions(any(LocalDate.class));
        verify(evaluationBatchService, times(1)).evaluateDailyMissions(any(LocalDate.class), any());
    }

    @Test
    void evaluatesYesterdayBeforeAssigningTodayMission() {
        RelativeMissionAssignmentBatchService assignmentBatch = mock(RelativeMissionAssignmentBatchService.class);
        MissionEvaluationBatchService evaluationBatch = mock(MissionEvaluationBatchService.class);
        RelativeMissionAssignmentScheduler scheduler =
                new RelativeMissionAssignmentScheduler(assignmentBatch, evaluationBatch, "Asia/Seoul");

        scheduler.assignDailyMissions();

        var order = inOrder(evaluationBatch, assignmentBatch);
        order.verify(evaluationBatch).evaluateDailyMissions(any(LocalDate.class), any());
        order.verify(assignmentBatch).assignDailyMissions(any(LocalDate.class));
    }
}
