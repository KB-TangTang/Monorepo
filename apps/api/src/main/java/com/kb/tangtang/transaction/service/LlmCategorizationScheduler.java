package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.domain.LlmCategorizationJob;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 이슈 #147 후속 — PENDING 상태의 LLM 분류 작업을 주기적으로 집어 처리한다.
 * NotificationDlqRetryScheduler 와 같은 패턴: 한 작업이 실패해도 배치 전체가 멈추지 않는다.
 */
@Component
public class LlmCategorizationScheduler {

    private static final Logger log = LoggerFactory.getLogger(LlmCategorizationScheduler.class);

    private final LlmCategorizationJobMapper jobMapper;
    private final LlmCategorizationProcessingService processingService;
    private final int maxJobsPerTick;

    @Autowired
    public LlmCategorizationScheduler(LlmCategorizationJobMapper jobMapper,
                                      LlmCategorizationProcessingService processingService,
                                      @Value("${llm.categorization.poll.max-jobs-per-tick}") int maxJobsPerTick) {
        this.jobMapper = jobMapper;
        this.processingService = processingService;
        this.maxJobsPerTick = maxJobsPerTick;
    }

    @Scheduled(fixedDelayString = "${llm.categorization.poll.fixed-delay-ms}")
    public void pollAndProcess() {
        for (LlmCategorizationJob job : jobMapper.findPending(maxJobsPerTick)) {
            try {
                processingService.processJob(job.getId());
            } catch (Exception e) {
                /* 한 작업 때문에 배치 전체가 멈추면 안 된다 — 이미 processJob 내부에서 FAILED 로 마감했다. */
                log.error("LLM 분류 작업 처리 실패 jobId={}", job.getId(), e);
            }
        }
    }
}
