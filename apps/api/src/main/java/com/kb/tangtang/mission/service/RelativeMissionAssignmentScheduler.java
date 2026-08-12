package com.kb.tangtang.mission.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class RelativeMissionAssignmentScheduler {

    private final RelativeMissionAssignmentBatchService batchService;
    private final ZoneId zoneId;
    private final AtomicBoolean startupRecoveryCompleted = new AtomicBoolean(false);

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

    @Scheduled(cron = "${mission.assignment.recovery-cron}", zone = "${mission.assignment.zone}")
    public void recoverMissingDailyMissions() {
        batchService.assignDailyMissions(LocalDate.now(zoneId));
    }

    @EventListener(ContextRefreshedEvent.class)
    public void recoverMissingDailyMissionsOnStartup() {
        if (startupRecoveryCompleted.compareAndSet(false, true)) {
            batchService.assignDailyMissions(LocalDate.now(zoneId));
        }
    }
}
