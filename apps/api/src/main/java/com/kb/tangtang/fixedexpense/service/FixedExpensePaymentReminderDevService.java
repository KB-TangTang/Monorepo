package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.mapper.FixedExpensePaymentReminderMapper;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.mapper.NotificationMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 로컬에서 결제 예정 알림을 반복 검증하기 위한 초기화 서비스다. */
@Service
public class FixedExpensePaymentReminderDevService {

    private final FixedExpensePaymentReminderMapper paymentReminderMapper;
    private final NotificationMapper notificationMapper;

    public FixedExpensePaymentReminderDevService(FixedExpensePaymentReminderMapper paymentReminderMapper,
                                                 NotificationMapper notificationMapper) {
        this.paymentReminderMapper = paymentReminderMapper;
        this.notificationMapper = notificationMapper;
    }

    /** 다른 알림은 보존하고 PAYMENT_DUE 알림과 그 발송 이력만 현재 사용자 범위에서 제거한다. */
    @Transactional
    public ResetResult resetPaymentDueReminders(long userId) {
        int deletedNotifications = notificationMapper.deleteByUserAndType(
                userId, NotificationType.PAYMENT_DUE.name());
        int deletedReminderHistory = paymentReminderMapper.deletePaymentRemindersByUser(userId);
        return new ResetResult(deletedNotifications, deletedReminderHistory);
    }

    public record ResetResult(int deletedNotifications, int deletedReminderHistory) {
    }
}
