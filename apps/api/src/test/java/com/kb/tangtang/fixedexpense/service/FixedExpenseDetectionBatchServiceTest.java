package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.mapper.FixedExpenseDetectionMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FixedExpenseDetectionBatchServiceTest {

    @Test
    void continuesWithOtherUsersWhenOneDetectionFails() {
        FixedExpenseDetectionMapper mapper = mock(FixedExpenseDetectionMapper.class);
        FixedExpenseLifecycleService lifecycleService = mock(FixedExpenseLifecycleService.class);
        FixedExpenseDetectionService detectionService = mock(FixedExpenseDetectionService.class);
        when(mapper.findActiveUserIds()).thenReturn(List.of(1L, 2L));
        when(lifecycleService.verifyForUser(1L)).thenReturn(1);
        when(detectionService.detectForUser(1L)).thenReturn(2);
        doThrow(new IllegalStateException("temporary failure"))
                .when(lifecycleService).verifyForUser(2L);

        int changed = new FixedExpenseDetectionBatchService(mapper, lifecycleService, detectionService)
                .runDailyFixedExpenseBatch();

        assertEquals(3, changed);
        verify(lifecycleService).verifyForUser(1L);
        verify(lifecycleService).verifyForUser(2L);
        verify(detectionService).detectForUser(1L);
        verify(detectionService, never()).detectForUser(2L);
    }
}
