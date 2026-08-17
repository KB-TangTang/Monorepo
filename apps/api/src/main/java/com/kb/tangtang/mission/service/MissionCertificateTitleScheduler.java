package com.kb.tangtang.mission.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매월 1일, 전월 마지막 미션 판정과 당월 미션 배정이 끝난 뒤 AI 타이틀을 생성한다. */
@Component
public class MissionCertificateTitleScheduler {

    private final MissionCertificateTitleBatchService batchService;

    public MissionCertificateTitleScheduler(MissionCertificateTitleBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(cron = "${mission.certificate.title.batch.cron}", zone = "${mission.assignment.zone}")
    public void generatePreviousMonthTitles() {
        batchService.generatePreviousMonthTitles();
    }

    @Scheduled(cron = "${mission.certificate.title.batch.recovery-cron}", zone = "${mission.assignment.zone}")
    public void recoverPreviousMonthTitles() {
        batchService.generatePreviousMonthTitles();
    }
}
