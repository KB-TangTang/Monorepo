package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.domain.LlmCategorizationJob;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LlmCategorizationSchedulerTest {

    private LlmCategorizationJobMapper jobMapper;
    private LlmCategorizationProcessingService processingService;
    private LlmCategorizationScheduler scheduler;

    @BeforeEach
    void setUp() {
        jobMapper = mock(LlmCategorizationJobMapper.class);
        processingService = mock(LlmCategorizationProcessingService.class);
        scheduler = new LlmCategorizationScheduler(jobMapper, processingService, 5);
    }

    @Test
    @DisplayName("PENDING 작업을 최대 maxJobsPerTick 건 가져와 하나씩 처리한다")
    void processesAllPendingJobsUpToLimit() {
        when(jobMapper.findPending(5)).thenReturn(List.of(
                LlmCategorizationJob.builder().id(1L).build(),
                LlmCategorizationJob.builder().id(2L).build()));

        scheduler.pollAndProcess();

        verify(processingService).processJob(1L);
        verify(processingService).processJob(2L);
    }

    @Test
    @DisplayName("한 작업 처리 중 예외가 나도 나머지 작업은 계속 처리한다")
    void continuesProcessingAfterOneJobFails() {
        when(jobMapper.findPending(5)).thenReturn(List.of(
                LlmCategorizationJob.builder().id(1L).build(),
                LlmCategorizationJob.builder().id(2L).build()));
        doThrow(new RuntimeException("boom")).when(processingService).processJob(1L);

        assertDoesNotThrow(() -> scheduler.pollAndProcess());

        verify(processingService).processJob(1L);
        verify(processingService).processJob(2L);
    }

    @Test
    @DisplayName("PENDING 작업이 없으면 processJob 을 호출하지 않는다")
    void doesNothingWhenNoPendingJobs() {
        when(jobMapper.findPending(5)).thenReturn(List.of());

        scheduler.pollAndProcess();

        verify(processingService, times(0)).processJob(eq(1L));
    }
}
