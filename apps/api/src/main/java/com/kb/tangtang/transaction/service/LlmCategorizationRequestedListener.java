package com.kb.tangtang.transaction.service;

import com.kb.tangtang.account.domain.LlmCategorizationRequestedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * LLM 분류 작업 등록 리스너. account 모듈이 발행한 이벤트를 받는다.
 *
 * ⚠ @Async 다. 발행자(FinancialSyncServiceImpl)가 작업 등록을 기다릴 이유가 없고, 등록이 실패해도
 * 발행자의 동기화 응답은 이미 나간 뒤라 실패를 전파할 대상이 없다 — 여기서 잡고 로그만 남긴다.
 */
@Component
public class LlmCategorizationRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(LlmCategorizationRequestedListener.class);

    private final LlmCategorizationJobService jobService;

    public LlmCategorizationRequestedListener(LlmCategorizationJobService jobService) {
        this.jobService = jobService;
    }

    @Async
    @EventListener
    public void onLlmCategorizationRequested(LlmCategorizationRequestedEvent event) {
        try {
            jobService.registerPendingJobs(event.userId(), event.transactionIds());
        } catch (Exception e) {
            log.error("LLM 분류 작업 등록 실패 userId={} transactionCount={}",
                    event.userId(), event.transactionIds().size(), e);
        }
    }
}
