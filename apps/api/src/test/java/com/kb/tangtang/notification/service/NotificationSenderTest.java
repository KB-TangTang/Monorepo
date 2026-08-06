package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationDlqRow;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

class NotificationSenderTest {

    private static class FakeDlqMapper implements NotificationDlqMapper {
        final List<String> inserted = new ArrayList<>();
        @Override public int insert(String eventType, String payloadJson, String errorMessage) {
            inserted.add(eventType); return 1;
        }
        @Override public List<NotificationDlqRow> findRetryable(int maxRetry) { return List.of(); }
        @Override public int increaseRetry(long dlqId) { return 1; }
        @Override public int delete(long dlqId) { return 1; }
    }

    private Notification sample() {
        return Notification.builder()
                .id(1L).userId(1L).type("ACCOUNT_RECONNECT").title("계좌 재연동이 필요해요")
                .content("국민은행").deepLinkUrl("/asset/accounts/9/reconnect")
                .createdAt(LocalDateTime.of(2026, 8, 6, 9, 0)).build();
    }

    @Test
    @DisplayName("수신자가 접속해 있지 않아도 DLQ 에 넣지 않는다 (NT_01_04)")
    void offlineUserIsNotAFailure() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        NotificationSender sender = new NotificationSender(new SseEmitterRegistry(), dlq);

        sender.send(sample());

        assertEquals(List.of(), dlq.inserted, "미연결은 실패가 아니다");
    }

    @Test
    @DisplayName("전송 중 연결이 끊겨도 DLQ 에 넣지 않고 해당 연결만 정리한다")
    void brokenConnectionIsNotAFailure() {
        SseEmitterRegistry registry = new SseEmitterRegistry() {
            @Override public List<SseEmitter> emittersOf(long userId) {
                return List.of(new SseEmitter() {
                    @Override public void send(SseEventBuilder builder) throws IOException {
                        throw new IOException("broken pipe");
                    }
                });
            }
        };
        FakeDlqMapper dlq = new FakeDlqMapper();

        assertDoesNotThrow(() -> new NotificationSender(registry, dlq).send(sample()));
        assertEquals(List.of(), dlq.inserted);
    }

    @Test
    @DisplayName("메시지 변환 실패는 DLQ 에 넣는다 — 그리고 예외를 밖으로 던지지 않는다")
    void conversionFailureGoesToDlq() {
        FakeDlqMapper dlq = new FakeDlqMapper();
        NotificationSender sender = new NotificationSender(new SseEmitterRegistry(), dlq);

        assertDoesNotThrow(() -> sender.sendFailure("NOTIFICATION_CREATE", "{}", "변환 실패"));

        assertEquals(List.of("NOTIFICATION_CREATE"), dlq.inserted);
    }
}
