package com.kb.tangtang.notification.service;

import com.kb.tangtang.account.domain.AccountReconnectRequiredEvent;
import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.dto.NotificationListDto;
import com.kb.tangtang.notification.dto.UnreadCountDto;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import com.kb.tangtang.notification.domain.NotificationDlqRow;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AccountEventListenerTest {

    private static class RecordingService extends NotificationService {
        final List<String> created = new ArrayList<>();
        boolean explode = false;
        RecordingService() { super((com.kb.tangtang.notification.mapper.NotificationMapper) null); }
        @Override
        public Notification create(long userId, NotificationType type, String content, String deepLink) {
            if (explode) {
                throw new IllegalStateException("DB down");
            }
            created.add(userId + "|" + type + "|" + content + "|" + deepLink);
            return Notification.builder().id(1L).userId(userId).type(type.name()).build();
        }
        @Override public NotificationListDto list(long u, Long c, Integer s) { return null; }
        @Override public UnreadCountDto markRead(long u, long i) { return null; }
        @Override public UnreadCountDto markAllRead(long u) { return null; }
    }

    private static class FakeDlqMapper implements NotificationDlqMapper {
        final List<String> inserted = new ArrayList<>();
        @Override public int insert(String t, String p, String e) { inserted.add(t); return 1; }
        @Override public List<NotificationDlqRow> findRetryable(int m) { return List.of(); }
        @Override public int increaseRetry(long id) { return 1; }
        @Override public int delete(long id) { return 1; }
    }

    @Test
    @DisplayName("재연동 필요 이벤트를 받으면 알림 1건을 만든다")
    void createsNotificationOnEvent() {
        RecordingService service = new RecordingService();
        FakeDlqMapper dlq = new FakeDlqMapper();
        AccountEventListener listener = new AccountEventListener(
                service, new NotificationSender(new SseEmitterRegistry(), dlq));

        listener.onReconnectRequired(new AccountReconnectRequiredEvent(7L, 9L, "국민은행"));

        assertEquals(1, service.created.size());
        assertEquals("7|ACCOUNT_RECONNECT|국민은행 · 인증이 만료됐어요|/asset/accounts/9/reconnect",
                service.created.get(0));
    }

    @Test
    @DisplayName("알림 생성이 실패해도 발행자를 죽이지 않고 DLQ 로만 남긴다")
    void failureGoesToDlqWithoutThrowing() {
        RecordingService service = new RecordingService();
        service.explode = true;
        FakeDlqMapper dlq = new FakeDlqMapper();
        AccountEventListener listener = new AccountEventListener(
                service, new NotificationSender(new SseEmitterRegistry(), dlq));

        assertDoesNotThrow(() ->
                listener.onReconnectRequired(new AccountReconnectRequiredEvent(7L, 9L, "국민은행")));

        assertEquals(List.of("ACCOUNT_RECONNECT"), dlq.inserted);
    }
}
