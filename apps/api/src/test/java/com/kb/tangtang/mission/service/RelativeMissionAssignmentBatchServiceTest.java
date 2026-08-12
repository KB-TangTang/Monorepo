package com.kb.tangtang.mission.service;

import com.kb.tangtang.common.exception.BusinessException;
import com.kb.tangtang.mission.mapper.RelativeMissionAssignmentMapper;
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
    @Mock private RelativeMissionAssignmentService assignmentService;

    @Test
    void oneUserFailureDoesNotStopOtherUsers() {
        LocalDate date = LocalDate.of(2026, 8, 12);
        when(mapper.findActiveUserIds()).thenReturn(List.of(1L, 2L, 3L));
        when(assignmentService.assign(2L, date))
                .thenThrow(new BusinessException("MISSION_POOL_EMPTY", "미션 없음"));

        new RelativeMissionAssignmentBatchService(mapper, assignmentService).assignDailyMissions(date);

        verify(assignmentService).assign(1L, date);
        verify(assignmentService).assign(2L, date);
        verify(assignmentService).assign(3L, date);
    }
}
