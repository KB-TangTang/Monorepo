package com.kb.tangtang.fixedexpense.service;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/** 앱 기동 보정과 일일 고정지출 수명주기·최초 탐지의 실행 시각만 담당한다. */
@Component
public class FixedExpenseDetectionScheduler {

    private final FixedExpenseDetectionBatchService batchService;
    private final AtomicBoolean startupRecoveryCompleted = new AtomicBoolean(false);

    public FixedExpenseDetectionScheduler(FixedExpenseDetectionBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(cron = "${fixed.expense.detection.cron}",
            zone = "${fixed.expense.detection.zone}")
    public void runDailyFixedExpenseBatch() {
        batchService.runDailyFixedExpenseBatch();
    }

    @EventListener(ContextRefreshedEvent.class)
    public void recoverMonthlyFixedExpensesOnStartup() {
        if (startupRecoveryCompleted.compareAndSet(false, true)) {
            batchService.runDailyFixedExpenseBatch();
        }
    }
}
