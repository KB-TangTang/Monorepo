package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.mapper.FixedExpenseDetectionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FixedExpenseDetectionBatchServiceTest {

    @Test
    void continuesWithOtherUsersWhenOneDetectionFails() {
        FixedExpenseDetectionMapper mapper = mock(FixedExpenseDetectionMapper.class);
        FixedExpenseDetectionService detectionService = mock(FixedExpenseDetectionService.class);
        when(mapper.findActiveUserIds()).thenReturn(List.of(1L, 2L));
        when(detectionService.detectForUser(1L)).thenReturn(2);
        doThrow(new IllegalStateException("temporary failure"))
                .when(detectionService).detectForUser(2L);

        int detected = new FixedExpenseDetectionBatchService(mapper, detectionService)
                .detectAllUsers();

        assertEquals(2, detected);
        verify(detectionService).detectForUser(1L);
        verify(detectionService).detectForUser(2L);
    }
}
