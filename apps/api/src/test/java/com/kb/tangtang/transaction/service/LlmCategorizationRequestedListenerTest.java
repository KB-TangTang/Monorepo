package com.kb.tangtang.transaction.service;

import com.kb.tangtang.account.domain.LlmCategorizationRequestedEvent;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class LlmCategorizationRequestedListenerTest {

    @Test
    void delegatesToJobService() {
        LlmCategorizationJobService jobService = mock(LlmCategorizationJobService.class);
        LlmCategorizationRequestedListener listener = new LlmCategorizationRequestedListener(jobService);

        listener.onLlmCategorizationRequested(new LlmCategorizationRequestedEvent(1L, List.of(10L, 20L)));

        verify(jobService).registerPendingJobs(1L, List.of(10L, 20L));
    }

    @Test
    void jobServiceFailureDoesNotPropagate() {
        LlmCategorizationJobService jobService = mock(LlmCategorizationJobService.class);
        doThrow(new RuntimeException("DB 오류")).when(jobService).registerPendingJobs(1L, List.of(10L));
        LlmCategorizationRequestedListener listener = new LlmCategorizationRequestedListener(jobService);

        /* @Async 리스너 안에서 예외가 나도 호출자(이벤트 발행자)에게 전파되면 안 된다 — module-event 스킬. */
        assertDoesNotThrow(() ->
                listener.onLlmCategorizationRequested(new LlmCategorizationRequestedEvent(1L, List.of(10L))));
    }
}
