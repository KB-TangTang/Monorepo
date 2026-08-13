package com.kb.tangtang.account.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.kb.tangtang.account.mapper.ConnectedAccountMapper;

/**
 * 이슈 #199 — 계좌·카드 연동이 있는 사용자를 주기적으로 자동 동기화한다.
 * LlmCategorizationScheduler/NotificationDlqRetryScheduler 와 같은 패턴: 한 사용자가 실패해도
 * 배치 전체가 멈추지 않는다.
 *
 * ⚠ 이 클래스는 "누구를, 몇 명까지" 만 결정한다. 실제 동기화 로직은 전혀 갖지 않고
 *   FinancialSyncServiceImpl.sync() 를 그대로 재사용한다 — 수동 트리거(POST /sync)와 완전히
 *   같은 파이프라인이다. 별도 잠금·선점 테이블이 없는 이유: @Scheduled(fixedDelay) 는 이전
 *   실행이 끝나야 다음이 시작돼 이 메서드 자체는 절대 중첩 실행되지 않고, sync() 내부의 저장은
 *   자연키 upsert 라 수동 트리거와 배치가 같은 사용자를 우연히 겹쳐 돌려도 안전하다
 *   (FinancialSyncServiceImpl 클래스 Javadoc 「동시 insert 경합」 참고).
 */
@Component
public class FinancialSyncBatchScheduler {

    private static final Logger log = LoggerFactory.getLogger(FinancialSyncBatchScheduler.class);

    private final ConnectedAccountMapper connectedAccountMapper;
    private final FinancialSyncService financialSyncService;
    private final boolean enabled;
    private final int maxUsersPerTick;

    @Autowired
    public FinancialSyncBatchScheduler(ConnectedAccountMapper connectedAccountMapper,
                                       FinancialSyncService financialSyncService,
                                       @Value("${financial.sync.batch.enabled}") boolean enabled,
                                       @Value("${financial.sync.batch.max-users-per-tick}") int maxUsersPerTick) {
        this.connectedAccountMapper = connectedAccountMapper;
        this.financialSyncService = financialSyncService;
        this.enabled = enabled;
        this.maxUsersPerTick = maxUsersPerTick;
    }

    @Scheduled(fixedDelayString = "${financial.sync.batch.fixed-delay-ms}")
    public void runBatch() {
        if (!enabled) {
            return;
        }
        for (Long userId : connectedAccountMapper.findUserIdsDueForSync(maxUsersPerTick)) {
            try {
                financialSyncService.sync(userId);
            } catch (Exception e) {
                /* 한 사용자 때문에 배치 전체가 멈추면 안 된다 — sync() 내부에서 이미 실패 이력을 남겼다. */
                log.error("배치 동기화 실패 userId={}", userId, e);
            }
        }
    }
}
