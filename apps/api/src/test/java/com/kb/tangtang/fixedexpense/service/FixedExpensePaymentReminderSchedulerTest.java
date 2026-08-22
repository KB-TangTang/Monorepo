package com.kb.tangtang.fixedexpense.service;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class FixedExpensePaymentReminderSchedulerTest {

    @Test
    void scheduledBatchDelegatesToPaymentReminderBatchService() {
        FixedExpensePaymentReminderBatchService batchService = mock(FixedExpensePaymentReminderBatchService.class);
        FixedExpensePaymentReminderScheduler scheduler = new FixedExpensePaymentReminderScheduler(batchService);

        scheduler.sendDuePaymentReminders();

        verify(batchService).sendDuePaymentReminders();
    }
}
