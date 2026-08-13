package com.kb.tangtang.mission.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class RelativeMissionAssignmentSchedulerTest {

    @Test
    void scheduledRecoveryDelegatesToBatchService() {
        RelativeMissionAssignmentBatchService batchService = mock(RelativeMissionAssignmentBatchService.class);
        RelativeMissionAssignmentScheduler scheduler =
                new RelativeMissionAssignmentScheduler(batchService, "Asia/Seoul");

        scheduler.recoverMissingDailyMissions();

        verify(batchService).assignDailyMissions(any(LocalDate.class));
    }

    @Test
    void startupRecoveryRunsOnlyOnceWhenContextRefreshesMoreThanOnce() {
        RelativeMissionAssignmentBatchService batchService = mock(RelativeMissionAssignmentBatchService.class);
        RelativeMissionAssignmentScheduler scheduler =
                new RelativeMissionAssignmentScheduler(batchService, "Asia/Seoul");

        scheduler.recoverMissingDailyMissionsOnStartup();
        scheduler.recoverMissingDailyMissionsOnStartup();

        verify(batchService, times(1)).assignDailyMissions(any(LocalDate.class));
    }
}
