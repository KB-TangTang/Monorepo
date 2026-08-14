package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.mapper.FixedExpenseDetectionMapper;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

/** 사용자별 트랜잭션 경계를 유지하면서 월간 고정지출 탐지를 실행한다. */
@Service
@Log4j2
public class FixedExpenseDetectionBatchService {

    private final FixedExpenseDetectionMapper mapper;
    private final FixedExpenseDetectionService detectionService;

    public FixedExpenseDetectionBatchService(FixedExpenseDetectionMapper mapper,
                                              FixedExpenseDetectionService detectionService) {
        this.mapper = mapper;
        this.detectionService = detectionService;
    }

    public int detectAllUsers() {
        int detectedCount = 0;
        for (Long userId : mapper.findActiveUserIds()) {
            try {
                detectedCount += detectionService.detectForUser(userId);
            } catch (RuntimeException exception) {
                log.error("월간 고정지출 탐지 실패. userId={}", userId, exception);
            }
        }
        log.info("월간 고정지출 탐지 배치 완료. detected={}", detectedCount);
        return detectedCount;
    }
}
