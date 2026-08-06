package com.kb.tangtang.notification.service;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * userId → 열려 있는 SSE 연결.
 *
 * 단일 프로세스 모듈러 모놀리스 전제라 외부 저장소가 필요 없다 (ARCHITECTURE 2026-07-26).
 * 같은 사용자의 다중 연결(탭 여러 개)을 허용한다.
 */
@Component
public class SseEmitterRegistry {

    /** 30분. 프록시가 먼저 끊어도 프론트가 재연결하므로 길게 잡지 않는다 */
    public static final long TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(long userId) {
        SseEmitter emitter = new SseEmitter(TIMEOUT_MS);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));
        return emitter;
    }

    public List<SseEmitter> emittersOf(long userId) {
        return List.copyOf(emitters.getOrDefault(userId, new CopyOnWriteArrayList<>()));
    }

    public void remove(long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        if (list == null) {
            return;
        }
        list.remove(emitter);
        if (list.isEmpty()) {
            emitters.remove(userId);
        }
    }

    public int connectionCount(long userId) {
        return emittersOf(userId).size();
    }
}
