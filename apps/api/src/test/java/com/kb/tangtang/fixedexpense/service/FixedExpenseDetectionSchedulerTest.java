package com.kb.tangtang.fixedexpense.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class FixedExpenseDetectionSchedulerTest {

    @Test
    void scheduledDetectionDelegatesToBatchService() {
        FixedExpenseDetectionBatchService batchService = mock(FixedExpenseDetectionBatchService.class);
        FixedExpenseDetectionScheduler scheduler = new FixedExpenseDetectionScheduler(batchService);

        scheduler.detectMonthlyFixedExpenses();

        verify(batchService).detectAllUsers();
    }

    @Test
    void startupRecoveryRunsOnlyOnce() {
        FixedExpenseDetectionBatchService batchService = mock(FixedExpenseDetectionBatchService.class);
        FixedExpenseDetectionScheduler scheduler = new FixedExpenseDetectionScheduler(batchService);

        scheduler.recoverMonthlyFixedExpensesOnStartup();
        scheduler.recoverMonthlyFixedExpensesOnStartup();

        verify(batchService, times(1)).detectAllUsers();
    }
}
