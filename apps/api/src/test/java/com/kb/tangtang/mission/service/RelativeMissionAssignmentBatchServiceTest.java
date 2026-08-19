package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
import com.kb.tangtang.mission.dto.RelativeMissionAssignmentDto;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelativeMissionAssignmentBatchServiceTest {

    @Mock private RelativeMissionAssignmentMapper mapper;
    @Mock private DailyMissionAssignmentService assignmentService;
    @Mock private ApplicationEventPublisher events;

    @Test
    void oneUserFailureDoesNotStopOtherUsers() {
        LocalDate date = LocalDate.of(2026, 8, 12);
        when(mapper.findUnassignedChallengeConsentedUserIds(date)).thenReturn(List.of(1L, 2L, 3L));
        when(assignmentService.assign(2L, date))
                .thenThrow(new BusinessException("MISSION_POOL_EMPTY", "미션 없음"));

        new RelativeMissionAssignmentBatchService(mapper, assignmentService, events).assignDailyMissions(date);

        verify(assignmentService).assign(1L, date);
        verify(assignmentService).assign(2L, date);
        verify(assignmentService).assign(3L, date);
    }

    @Test
    void sendsNotificationOnlyForNewAssignment() {
        LocalDate date = LocalDate.of(2026, 8, 12);
        when(mapper.findUnassignedChallengeConsentedUserIds(date)).thenReturn(List.of(1L));
        when(assignmentService.assign(1L, date)).thenReturn(RelativeMissionAssignmentDto.builder()
                .assigned(true).missionTitle("배달비 절약").build());

        new RelativeMissionAssignmentBatchService(mapper, assignmentService, events).assignDailyMissions(date);

        verify(events).publishEvent((Object) argThat(event -> {
            NotificationRequestedEvent request = (NotificationRequestedEvent) event;
            return request.userId() == 1L
                    && request.params().get("missionTitle").equals("배달비 절약")
                    && request.deepLinkUrl().equals("/mission/personal?date=2026-08-12");
        }));
    }
}
