package com.kb.tangtang.mission.service;

import com.kb.tangtang.mission.dto.RelativeMissionAssignmentDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyMissionAssignmentServiceTest {

    @Mock private AbsoluteMissionPolicy absolutePolicy;
    @Mock private AbsoluteMissionAssignmentService absoluteService;
    @Mock private RelativeMissionAssignmentService relativeService;
    @Mock private MissionAnalysisSnapshotService snapshotService;

    @Test
    void monthlyInspectionTakesPriorityForEveryUser() {
        LocalDate date = LocalDate.of(2026, 8, 23);
        when(absolutePolicy.isMonthlyInspectionDate(date)).thenReturn(true);
        DailyMissionAssignmentService service = service();

        service.assign(7L, date);

        verify(absoluteService).assign(7L, date, AbsoluteMissionAssignmentService.MONTHLY_RANDOM);
        verifyNoInteractions(snapshotService, relativeService);
    }

    @Test
    void coldStartUserGetsDailyAbsoluteMission() {
        LocalDate date = LocalDate.of(2026, 8, 18);
        when(snapshotService.ensureRelativeMissionQualification(7L)).thenReturn(false);
        DailyMissionAssignmentService service = service();

        service.assign(7L, date);

        verify(absoluteService).assign(7L, date, AbsoluteMissionAssignmentService.COLD_START);
        verifyNoInteractions(relativeService);
    }

    @Test
    void qualifiedUserGetsRelativeMissionOnOrdinaryDay() {
        LocalDate date = LocalDate.of(2026, 8, 18);
        when(snapshotService.ensureRelativeMissionQualification(7L)).thenReturn(true);
        when(relativeService.assign(7L, date)).thenReturn(
                RelativeMissionAssignmentDto.builder().assigned(true).assignDate(date).build());
        DailyMissionAssignmentService service = service();

        service.assign(7L, date);

        verify(relativeService).assign(7L, date);
        verifyNoInteractions(absoluteService);
    }

    @Test
    void qualifiedUserGetsAbsoluteMissionWhenRecentRelativeDataIsInsufficient() {
        LocalDate date = LocalDate.of(2026, 8, 18);
        when(snapshotService.ensureRelativeMissionQualification(7L)).thenReturn(true);
        when(relativeService.assign(7L, date)).thenReturn(RelativeMissionAssignmentDto.skipped(date));
        DailyMissionAssignmentService service = service();

        service.assign(7L, date);

        verify(absoluteService).assign(
                7L, date, AbsoluteMissionAssignmentService.INSUFFICIENT_RELATIVE_DATA);
    }

    private DailyMissionAssignmentService service() {
        return new DailyMissionAssignmentService(
                absolutePolicy, absoluteService, relativeService, snapshotService);
    }
}
