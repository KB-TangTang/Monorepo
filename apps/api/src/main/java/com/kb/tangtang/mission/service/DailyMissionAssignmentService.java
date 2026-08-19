package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.dto.RelativeMissionAssignmentDto;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class DailyMissionAssignmentService {

    private final AbsoluteMissionPolicy absolutePolicy;
    private final AbsoluteMissionAssignmentService absoluteService;
    private final RelativeMissionAssignmentService relativeService;
    private final MissionAnalysisSnapshotService snapshotService;

    public DailyMissionAssignmentService(AbsoluteMissionPolicy absolutePolicy,
                                         AbsoluteMissionAssignmentService absoluteService,
                                         RelativeMissionAssignmentService relativeService,
                                         MissionAnalysisSnapshotService snapshotService) {
        this.absolutePolicy = absolutePolicy;
        this.absoluteService = absoluteService;
        this.relativeService = relativeService;
        this.snapshotService = snapshotService;
    }

    public RelativeMissionAssignmentDto assign(long userId, LocalDate assignDate) {
        if (absolutePolicy.isMonthlyInspectionDate(assignDate)) {
            return absoluteService.assign(userId, assignDate, AbsoluteMissionAssignmentService.MONTHLY_RANDOM);
        }
        if (!snapshotService.ensureRelativeMissionQualification(userId)) {
            return absoluteService.assign(userId, assignDate, AbsoluteMissionAssignmentService.COLD_START);
        }
        RelativeMissionAssignmentDto relativeAssignment = relativeService.assign(userId, assignDate);
        if (relativeAssignment.isAssigned()) {
            return relativeAssignment;
        }
        return absoluteService.assign(
                userId, assignDate, AbsoluteMissionAssignmentService.INSUFFICIENT_RELATIVE_DATA);
    }
}
