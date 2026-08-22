package com.kb.tangtang.fixedexpense.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 기존 고정지출 일일 배치와 같은 시각·시간대로 결제 예정 알림을 실행한다. */
@Component
public class FixedExpensePaymentReminderScheduler {

    private final FixedExpensePaymentReminderBatchService batchService;

    public FixedExpensePaymentReminderScheduler(FixedExpensePaymentReminderBatchService batchService) {
        this.batchService = batchService;
    }

    @Scheduled(cron = "${fixed.expense.detection.cron}",
            zone = "${fixed.expense.detection.zone}")
    public void sendDuePaymentReminders() {
        batchService.sendDuePaymentReminders();
    }
}
