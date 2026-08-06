package com.kb.tangtang.notification.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SseEmitterRegistryTest {

    @Test
    @DisplayName("같은 사용자가 탭을 두 개 열면 연결도 두 개다")
    void allowsMultipleConnectionsPerUser() {
        SseEmitterRegistry registry = new SseEmitterRegistry();

        registry.register(1L);
        registry.register(1L);

        assertEquals(2, registry.connectionCount(1L));
    }

    @Test
    @DisplayName("사용자끼리 섞이지 않는다")
    void isolatesUsers() {
        SseEmitterRegistry registry = new SseEmitterRegistry();

        registry.register(1L);
        registry.register(2L);

        assertEquals(1, registry.connectionCount(1L));
        assertEquals(1, registry.connectionCount(2L));
    }

    @Test
    @DisplayName("연결을 제거하면 목록에서 빠진다")
    void removesEmitter() {
        SseEmitterRegistry registry = new SseEmitterRegistry();
        SseEmitter emitter = registry.register(1L);

        registry.remove(1L, emitter);

        assertEquals(0, registry.connectionCount(1L));
    }

    @Test
    @DisplayName("접속한 적 없는 사용자는 빈 목록이다 — 이건 실패가 아니다 (NT_01_04)")
    void unknownUserHasNoEmitters() {
        assertEquals(0, new SseEmitterRegistry().emittersOf(999L).size());
    }
}
