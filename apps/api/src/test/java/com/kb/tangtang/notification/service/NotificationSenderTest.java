package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationDlqRow;
import com.kb.tangtang.notification.dto.NotificationDto;
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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

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

    /** 이 emitter 하나만 붙어 있는 레지스트리를 만든다 */
    private SseEmitterRegistry registryEmitting(SseEmitter emitter) {
        return new SseEmitterRegistry() {
            @Override public List<SseEmitter> emittersOf(long userId) {
                return List.of(emitter);
            }
        };
    }

    @Test
    @DisplayName("전송 중 연결이 끊겨도 DLQ 에 넣지 않고 해당 연결만 정리한다")
    void brokenConnectionIsNotAFailure() {
        SseEmitterRegistry registry = registryEmitting(new SseEmitter() {
            @Override public void send(SseEventBuilder builder) throws IOException {
                throw new IOException("broken pipe");
            }
        });
        FakeDlqMapper dlq = new FakeDlqMapper();

        assertDoesNotThrow(() -> new NotificationSender(registry, dlq).send(sample()));
        assertEquals(List.of(), dlq.inserted);
    }

    /** 이미 완료된 emitter 는 IllegalStateException 을 던진다. 이것도 끊긴 연결이지 실패가 아니다 */
    @Test
    @DisplayName("이미 완료된 연결(IllegalStateException)도 DLQ 대상이 아니다")
    void completedEmitterIsNotAFailure() {
        SseEmitterRegistry registry = registryEmitting(new SseEmitter() {
            @Override public void send(SseEventBuilder builder) {
                throw new IllegalStateException("ResponseBodyEmitter is already set complete");
            }
        });
        FakeDlqMapper dlq = new FakeDlqMapper();

        assertDoesNotThrow(() -> new NotificationSender(registry, dlq).send(sample()));
        assertEquals(List.of(), dlq.inserted);
    }

    /**
     * ⚠ 여기가 회귀의 핵심이다. 전에는 catch(Exception) 하나로 **모든** 예외를 "끊긴 연결"로 삼켜서
     *   직렬화·변환 실패가 DLQ 에 남지 않고 사라졌다 (NT_01_04 위반).
     */
    @Test
    @DisplayName("전송 중 변환 실패는 연결 문제와 달리 DLQ 로 간다")
    void serializationFailureDuringSendGoesToDlq() {
        SseEmitterRegistry registry = registryEmitting(new SseEmitter() {
            @Override public void send(SseEventBuilder builder) {
                throw new IllegalArgumentException("직렬화할 수 없는 값");
            }
        });
        FakeDlqMapper dlq = new FakeDlqMapper();

        assertDoesNotThrow(() -> new NotificationSender(registry, dlq).send(sample()));
        assertEquals(List.of("ACCOUNT_RECONNECT"), dlq.inserted, "변환 실패는 실패다 — DLQ 에 남아야 한다");
    }

    @Test
    @DisplayName("SSE 로 내려보내는 것은 도메인이 아니라 REST 와 같은 NotificationDto 다")
    void pushesTheSameShapeAsRest() {
        List<Object> sent = new ArrayList<>();
        SseEmitterRegistry registry = registryEmitting(new SseEmitter() {
            @Override public void send(SseEventBuilder builder) {
                builder.build().forEach(part -> {
                    if (!(part.getData() instanceof String)) {
                        sent.add(part.getData());
                    }
                });
            }
        });

        new NotificationSender(registry, new FakeDlqMapper()).send(sample());

        assertEquals(1, sent.size(), "이벤트 데이터가 하나 실려야 한다");
        assertInstanceOf(NotificationDto.class, sent.get(0));
        NotificationDto dto = (NotificationDto) sent.get(0);
        assertEquals("2026-08-06T09:00:00", dto.getCreatedAt());
        assertEquals("ACCOUNT_RECONNECT", dto.getType());
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
