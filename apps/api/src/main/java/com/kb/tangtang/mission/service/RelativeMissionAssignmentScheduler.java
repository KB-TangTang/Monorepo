package com.kb.tangtang.mission.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class RelativeMissionAssignmentScheduler {

    private final RelativeMissionAssignmentBatchService batchService;
    private final ZoneId zoneId;

    public RelativeMissionAssignmentScheduler(
            RelativeMissionAssignmentBatchService batchService,
            @Value("${mission.assignment.zone}") String zoneId) {
        this.batchService = batchService;
        this.zoneId = ZoneId.of(zoneId);
    }

    @Scheduled(cron = "${mission.assignment.cron}", zone = "${mission.assignment.zone}")
    public void assignDailyMissions() {
        batchService.assignDailyMissions(LocalDate.now(zoneId));
    }
}
