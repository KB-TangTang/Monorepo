package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.mapper.FixedExpenseDetectionMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/** 사용자별 트랜잭션 경계를 유지하면서 수명주기 검증 뒤 최초 탐지를 실행한다. */
@Service
@Log4j2
public class FixedExpenseDetectionBatchService {

    private final FixedExpenseDetectionMapper mapper;
    private final FixedExpenseLifecycleService lifecycleService;
    private final FixedExpenseDetectionService detectionService;

    public FixedExpenseDetectionBatchService(FixedExpenseDetectionMapper mapper,
                                              FixedExpenseLifecycleService lifecycleService,
                                              FixedExpenseDetectionService detectionService) {
        this.mapper = mapper;
        this.lifecycleService = lifecycleService;
        this.detectionService = detectionService;
    }

    public int runDailyFixedExpenseBatch() {
        int changedCount = 0;
        for (Long userId : mapper.findActiveUserIds()) {
            try {
                changedCount += lifecycleService.verifyForUser(userId);
                changedCount += detectionService.detectForUser(userId);
            } catch (RuntimeException exception) {
                log.error("고정지출 수명주기 검증 및 최초 탐지 실패. userId={}", userId, exception);
            }
        }
        log.info("고정지출 수명주기 검증 및 최초 탐지 배치 완료. changed={}", changedCount);
        return changedCount;
    }
}
