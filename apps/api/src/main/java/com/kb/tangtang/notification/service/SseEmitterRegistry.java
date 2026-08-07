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
        // compute 로 list 생성/조회와 emitter 추가를 원자적으로 처리한다.
        // register 와 remove 가 동시에 실행되면 새 emitter 가 빠지는 레이스 조건을 방지한다.
        emitters.compute(userId, (key, list) -> {
            CopyOnWriteArrayList<SseEmitter> target = (list == null) ? new CopyOnWriteArrayList<>() : list;
            target.add(emitter);
            return target;
        });
        // 콜백은 compute 블록 밖에서 등록해 deadlock 을 방지한다
        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));
        return emitter;
    }

    public List<SseEmitter> emittersOf(long userId) {
        CopyOnWriteArrayList<SseEmitter> list = emitters.get(userId);
        return list == null ? List.of() : List.copyOf(list);
    }

    public void remove(long userId, SseEmitter emitter) {
        // computeIfPresent 로 list 제거를 원자적으로 처리한다.
        // register() 의 compute() 와 쌍을 이루어 양방향 레이스를 방지한다:
        // - register 의 compute 블록 안에서 emitter 추가, remove 의 computeIfPresent 안에서 제거
        // 둘 중 하나라도 원자성을 잃으면 orphaned 연결이 생긴다
        emitters.computeIfPresent(userId, (key, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }

    public int connectionCount(long userId) {
        return emittersOf(userId).size();
    }

    /** 하트비트용. 순회 중 변경돼도 안전하도록 복사본을 준다 */
    public Map<Long, List<SseEmitter>> snapshot() {
        Map<Long, List<SseEmitter>> copy = new java.util.HashMap<>();
        emitters.forEach((userId, list) -> copy.put(userId, List.copyOf(list)));
        return copy;
    }
}
