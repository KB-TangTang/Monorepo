package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.dto.TodayMissionDto;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevMissionServiceTest {

    private static final long USER_ID = 7L;
    private static final LocalDate TODAY = LocalDate.of(2026, 8, 13);
    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-08-12T15:30:00Z"), ZoneId.of("UTC"));

    @Mock private RelativeMissionAssignmentMapper assignmentMapper;
    @Mock private RelativeMissionAssignmentService assignmentService;
    @Mock private TodayMissionService todayMissionService;

    @Test
    void reassignsTodayMissionInLocalEnvironment() {
        TodayMissionDto mission = new TodayMissionDto();
        mission.setMissionId(101L);
        when(todayMissionService.getTodayMission(USER_ID)).thenReturn(mission);
        DevMissionService service = new DevMissionService(
                assignmentMapper, assignmentService, todayMissionService, "local", CLOCK);

        TodayMissionDto result = service.reassignTodayMission(USER_ID);

        assertSame(mission, result);
        InOrder order = inOrder(assignmentMapper, assignmentService, todayMissionService);
        order.verify(assignmentMapper).deleteAssignment(USER_ID, TODAY);
        order.verify(assignmentMapper).resetAssignedSnapshots(USER_ID, TODAY);
        order.verify(assignmentService).assign(USER_ID, TODAY);
        order.verify(todayMissionService).getTodayMission(USER_ID);
    }

    @Test
    void rejectsReassignmentOutsideLocalEnvironment() {
        DevMissionService service = new DevMissionService(
                assignmentMapper, assignmentService, todayMissionService, "docker", CLOCK);

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.reassignTodayMission(USER_ID));

        assertEquals("DEV_API_DISABLED", exception.getCode());
        verifyNoInteractions(assignmentMapper, assignmentService, todayMissionService);
    }
}
