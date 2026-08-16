package com.kb.tangtang.report.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 일일 미션 판정 뒤에 지난달 개인 챌린지 성과와 확정 그룹 전적을 저장한다. */
@Component
public class ChallengeMonthlyReportScheduler {

    private final ChallengeMonthlyReportBatchService batchService;

    public ChallengeMonthlyReportScheduler(ChallengeMonthlyReportBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(cron = "${challenge.report.monthly.batch.cron}",
            zone = "${challenge.report.monthly.zone}")
    public void finalizePreviousMonthReports() {
        batchService.finalizePreviousMonthReports();
    }

    @Scheduled(cron = "${challenge.report.monthly.group-record-recovery.cron}",
            zone = "${challenge.report.monthly.zone}")
    public void refreshPreviousMonthEndGroupRecords() {
        batchService.refreshPreviousMonthEndGroupRecords();
    }
}
