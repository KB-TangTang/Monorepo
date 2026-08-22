package com.kb.tangtang.fixedexpense.service;

import com.kb.tangtang.fixedexpense.domain.FixedExpenseCandidate;
import com.kb.tangtang.fixedexpense.mapper.FixedExpensePaymentReminderMapper;
import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedExpensePaymentReminderDevServiceTest {

    @Test
    @DisplayName("개발용 초기화는 현재 사용자의 결제 예정 알림과 발송 이력만 제거한다")
    void resetsOnlyCurrentUsersPaymentDueRecords() {
        RecordingPaymentReminderMapper reminderMapper = new RecordingPaymentReminderMapper();
        RecordingNotificationMapper notificationMapper = new RecordingNotificationMapper();
        reminderMapper.deleted = 3;
        notificationMapper.deleted = 2;

        FixedExpensePaymentReminderDevService.ResetResult result =
                new FixedExpensePaymentReminderDevService(reminderMapper, notificationMapper)
                        .resetPaymentDueReminders(7L);

        assertEquals(2, result.deletedNotifications());
        assertEquals(3, result.deletedReminderHistory());
        assertEquals("7|PAYMENT_DUE", notificationMapper.deleteArgs);
        assertEquals(7L, reminderMapper.deleteUserId);
    }

    private static class RecordingPaymentReminderMapper implements FixedExpensePaymentReminderMapper {
        int deleted;
        long deleteUserId;

        @Override public List<FixedExpenseCandidate> findPaymentReminderCandidates(LocalDate startDate,
                                                                                    LocalDate endDate) {
            return List.of();
        }
        @Override public int registerPaymentReminder(long candidateId, LocalDate expectedPaymentDate,
                                                     String notificationType) { return 0; }
        @Override public int deletePaymentRemindersByUser(long userId) {
            deleteUserId = userId;
            return deleted;
        }
    }

    private static class RecordingNotificationMapper implements NotificationMapper {
        int deleted;
        String deleteArgs;

        @Override public int insert(Notification notification) { return 0; }
        @Override public List<Notification> findPage(long userId, Long cursor, int size) { return List.of(); }
        @Override public Notification findById(long id, long userId) { return null; }
        @Override public int countUnread(long userId) { return 0; }
        @Override public int countUnreadSame(long userId, String type, String deepLinkUrl) { return 0; }
        @Override public int markRead(long id, long userId) { return 0; }
        @Override public int markAllRead(long userId) { return 0; }
        @Override public int deleteByUserAndType(long userId, String type) {
            deleteArgs = userId + "|" + type;
            return deleted;
        }
    }
}
