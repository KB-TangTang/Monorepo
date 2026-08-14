package com.kb.tangtang.report.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 월초 자동 생성과 제한된 복구 창의 실행 시각만 담당한다. */
@Component
public class MonthlyReportBatchScheduler {

    private final MonthlyReportBatchService batchService;

    public MonthlyReportBatchScheduler(MonthlyReportBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(cron = "${report.monthly.batch.cron}", zone = "${report.monthly.zone}")
    public void generatePreviousMonthReports() {
        batchService.generatePreviousMonthReports();
    }

    @Scheduled(cron = "${report.monthly.batch.recovery-cron}", zone = "${report.monthly.zone}")
    public void recoverPreviousMonthReports() {
        batchService.generatePreviousMonthReports();
    }
}
