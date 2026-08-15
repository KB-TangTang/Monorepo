package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationSenderPushTest {

    @Mock private SseEmitterRegistry registry;
    @Mock private NotificationDlqMapper dlqMapper;
    @Mock private SseEmitter emitter;

    @Test
    @DisplayName("push 는 DB 를 건드리지 않고 SSE 로만 보낸다")
    void pushDoesNotTouchDatabase() throws IOException {
        when(registry.emittersOf(3L)).thenReturn(List.of(emitter));
        NotificationSender sender = new NotificationSender(registry, dlqMapper);

        sender.push(3L, "chat", Map.of("groupId", 7L));

        verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        verify(dlqMapper, never()).insert(any(), any(), any());
    }

    @Test
    @DisplayName("push 는 끊긴 연결을 정리하고 DLQ 에 넣지 않는다")
    void pushCleansUpBrokenConnection() throws IOException {
        when(registry.emittersOf(3L)).thenReturn(List.of(emitter));
        doThrow(new IOException("broken")).when(emitter).send(any(SseEmitter.SseEventBuilder.class));
        NotificationSender sender = new NotificationSender(registry, dlqMapper);

        sender.push(3L, "chat", Map.of());

        verify(registry).remove(3L, emitter);
        verify(dlqMapper, never()).insert(any(), any(), any());
    }
}
