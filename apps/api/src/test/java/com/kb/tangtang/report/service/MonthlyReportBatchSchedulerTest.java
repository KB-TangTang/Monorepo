package com.kb.tangtang.report.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MonthlyReportBatchSchedulerTest {

    @Mock
    private MonthlyReportBatchService batchService;

    @Test
    void delegatesPrimaryScheduleToBatchService() {
        MonthlyReportBatchScheduler scheduler = new MonthlyReportBatchScheduler(batchService);

        scheduler.generatePreviousMonthReports();

        verify(batchService).generatePreviousMonthReports();
    }

    @Test
    void delegatesRecoveryScheduleToSameBoundedBatch() {
        MonthlyReportBatchScheduler scheduler = new MonthlyReportBatchScheduler(batchService);

        scheduler.recoverPreviousMonthReports();

        verify(batchService).generatePreviousMonthReports();
    }
}
