package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
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
    private final MissionEvaluationBatchService evaluationBatchService;
    private final RelativeMissionAssignmentMapper assignmentMapper;
    private final ZoneId zoneId;
    private final AtomicBoolean startupRecoveryCompleted = new AtomicBoolean(false);

    public RelativeMissionAssignmentScheduler(
            RelativeMissionAssignmentBatchService batchService,
            MissionEvaluationBatchService evaluationBatchService,
            RelativeMissionAssignmentMapper assignmentMapper,
            @Value("${mission.assignment.zone}") String zoneId) {
        this.batchService = batchService;
        this.evaluationBatchService = evaluationBatchService;
        this.assignmentMapper = assignmentMapper;
        this.zoneId = ZoneId.of(zoneId);
    }

    @Scheduled(cron = "${mission.assignment.cron}", zone = "${mission.assignment.zone}")
    public void assignDailyMissions() {
        runDailyMissionBatch(LocalDate.now(zoneId));
    }

    @Scheduled(cron = "${mission.assignment.recovery-cron}", zone = "${mission.assignment.zone}")
    public void recoverMissingDailyMissions() {
        recoverDailyMissionBatches(LocalDate.now(zoneId));
    }

    @EventListener(ContextRefreshedEvent.class)
    public void recoverMissingDailyMissionsOnStartup() {
        if (startupRecoveryCompleted.compareAndSet(false, true)) {
            recoverDailyMissionBatches(LocalDate.now(zoneId));
        }
    }

    private void runDailyMissionBatch(LocalDate today) {
        evaluationBatchService.evaluateDailyMissions(today.minusDays(1), today.atStartOfDay());
        batchService.assignDailyMissions(today);
    }

    private void recoverDailyMissionBatches(LocalDate today) {
        LocalDate recoveryDate = assignmentMapper.findEarliestRecoveryDateBefore(today);
        while (recoveryDate != null && recoveryDate.isBefore(today)) {
            batchService.assignDailyMissions(recoveryDate, false);
            evaluationBatchService.evaluateDailyMissions(
                    recoveryDate, recoveryDate.plusDays(1).atStartOfDay(), false);
            recoveryDate = recoveryDate.plusDays(1);
        }
        batchService.assignDailyMissions(today);
    }
}
