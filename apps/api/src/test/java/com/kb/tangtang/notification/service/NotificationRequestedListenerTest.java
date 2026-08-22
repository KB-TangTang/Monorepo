package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationDlqRow;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import com.kb.tangtang.notification.domain.NotificationType;
import com.kb.tangtang.notification.dto.NotificationListDto;
import com.kb.tangtang.notification.dto.UnreadCountDto;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import com.kb.tangtang.notification.mapper.NotificationMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 범용 알림 리스너 (이슈 #68).
 *
 * 이 리스너 하나가 모든 도메인의 알림 요청을 처리한다 — 알림 종류가 늘어도
 * notification 모듈에 리스너를 새로 만들 일이 없어야 한다.
 */
class NotificationRequestedListenerTest {

    private static class RecordingService extends NotificationService {
        final List<String> created = new ArrayList<>();
        boolean explode = false;
        boolean duplicate = false;

        RecordingService() {
            super((NotificationMapper) null);
        }

        @Override
        public Notification create(long userId, NotificationType type, String content, String deepLink) {
            if (explode) {
                throw new IllegalStateException("DB down");
            }
            created.add(userId + "|" + type + "|" + content + "|" + deepLink);
            return Notification.builder().id(1L).userId(userId).type(type.name()).build();
        }

        @Override
        public Optional<Notification> createUnlessDuplicate(long userId, NotificationType type,
                                                            String content, String deepLink) {
            return duplicate ? Optional.empty()
                    : Optional.of(create(userId, type, content, deepLink));
        }

        @Override public NotificationListDto list(long u, Long c, Integer s) { return null; }
        @Override public UnreadCountDto markRead(long u, long i) { return null; }
        @Override public UnreadCountDto markAllRead(long u) { return null; }
    }

    private static class FakeDlqMapper implements NotificationDlqMapper {
        final List<String> insertedTypes = new ArrayList<>();
        final List<String> insertedErrors = new ArrayList<>();

        @Override
        public int insert(String type, String payload, String error) {
            insertedTypes.add(type);
            insertedErrors.add(error);
            return 1;
        }

        @Override public List<NotificationDlqRow> findRetryable(int m) { return List.of(); }
        @Override public int increaseRetry(long id) { return 1; }
        @Override public int delete(long id) { return 1; }
    }

    private static NotificationRequestedListener listenerWith(RecordingService service,
                                                              FakeDlqMapper dlq) {
        return new NotificationRequestedListener(
                service, new NotificationSender(new SseEmitterRegistry(), dlq));
    }

    @Test
    @DisplayName("알림 요청 이벤트를 받으면 템플릿을 채워 알림 1건을 만든다")
    void createsNotificationFromTemplate() {
        RecordingService service = new RecordingService();
        listenerWith(service, new FakeDlqMapper()).onNotificationRequested(
                new NotificationRequestedEvent(7L, NotificationType.ACCOUNT_RECONNECT,
                        Map.of("bankName", "국민은행"), "/asset/accounts/9/reconnect"));

        assertEquals(1, service.created.size());
        assertEquals("7|ACCOUNT_RECONNECT|국민은행 · 인증이 만료됐어요|/asset/accounts/9/reconnect",
                service.created.get(0));
    }

    @Test
    @DisplayName("문구가 미정인 종류는 발행자가 넘긴 content 를 그대로 쓴다 — 도메인이 notification 을 고치지 않아도 된다")
    void usesPublisherContentWhenTemplateUndecided() {
        RecordingService service = new RecordingService();
        listenerWith(service, new FakeDlqMapper()).onNotificationRequested(
                new NotificationRequestedEvent(3L, NotificationType.MISSION_DEADLINE,
                        Map.of("content", "무지출 미션 · 자정까지"), "/mission/personal"));

        assertEquals("3|MISSION_DEADLINE|무지출 미션 · 자정까지|/mission/personal",
                service.created.get(0));
    }

    @Test
    @DisplayName("알림 생성이 실패해도 발행자를 죽이지 않고 DLQ 로만 남긴다")
    void failureGoesToDlqWithoutThrowing() {
        RecordingService service = new RecordingService();
        service.explode = true;
        FakeDlqMapper dlq = new FakeDlqMapper();

        assertDoesNotThrow(() -> listenerWith(service, dlq).onNotificationRequested(
                new NotificationRequestedEvent(7L, NotificationType.ACCOUNT_RECONNECT,
                        Map.of("bankName", "국민은행"), "/asset/accounts/9/reconnect")));

        assertEquals(List.of("ACCOUNT_RECONNECT"), dlq.insertedTypes);
    }

    @Test
    @DisplayName("치환값이 빠지면 반쯤 만든 문구를 저장하지 않고 DLQ 로 보낸다")
    void missingParamDoesNotCreateNotification() {
        RecordingService service = new RecordingService();
        FakeDlqMapper dlq = new FakeDlqMapper();

        assertDoesNotThrow(() -> listenerWith(service, dlq).onNotificationRequested(
                new NotificationRequestedEvent(7L, NotificationType.ACCOUNT_RECONNECT,
                        Map.of(), "/asset/accounts/9/reconnect")));

        assertTrue(service.created.isEmpty(), "문구를 못 만들었으면 알림을 저장하면 안 된다");
        assertEquals(List.of("ACCOUNT_RECONNECT"), dlq.insertedTypes);
        assertTrue(dlq.insertedErrors.get(0).contains("bankName"),
                "무엇이 빠졌는지 DLQ 에 남아야 한다: " + dlq.insertedErrors.get(0));
    }

    @Test
    @DisplayName("같은 알림이 이미 안 읽은 채로 있으면 만들지 않는다 — 반복 호출로 알림이 쌓이지 않게")
    void skipsDuplicateNotification() {
        RecordingService service = new RecordingService();
        service.duplicate = true;
        FakeDlqMapper dlq = new FakeDlqMapper();

        listenerWith(service, dlq).onNotificationRequested(
                new NotificationRequestedEvent(7L, NotificationType.ACCOUNT_RECONNECT,
                        Map.of("bankName", "국민은행"), "/asset/accounts/9/reconnect"));

        assertTrue(service.created.isEmpty(), "중복이면 저장하지 않는다");
        assertEquals(List.of(), dlq.insertedTypes, "중복 건너뛰기는 실패가 아니다 — DLQ 대상이 아니다");
    }

    @Test
    @DisplayName("결제 예정 알림은 결제 주기 이력으로 중복을 막으므로 이전 안 읽음 알림이 있어도 저장한다")
    void createsPaymentDueNotificationEvenWhenGenericUnreadDuplicateExists() {
        RecordingService service = new RecordingService();
        service.duplicate = true;

        listenerWith(service, new FakeDlqMapper()).onNotificationRequested(
                new NotificationRequestedEvent(7L, NotificationType.PAYMENT_DUE,
                        Map.of("content", "넷플릭스 · 7일 후 결제 예정"), "/asset/fixed-expenses/101"));

        assertEquals(List.of("7|PAYMENT_DUE|넷플릭스 · 7일 후 결제 예정|/asset/fixed-expenses/101"),
                service.created);
    }
}
