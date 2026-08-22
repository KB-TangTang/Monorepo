package com.kb.tangtang.notification.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 열려 있는 SSE 연결에 15초마다 주석 프레임을 보낸다.
 *
 * 프록시(nginx·Vercel)는 일정 시간 데이터가 없으면 연결을 끊는다.
 * 주석은 SSE 규격상 클라이언트가 무시하므로 이벤트로 오해되지 않는다.
 *
 * 실패한 연결은 정리 대상이지 DLQ 대상이 아니다 (NT_01_04).
 */
@Component
public class SseHeartbeat {

    private final SseEmitterRegistry registry;

    public SseHeartbeat(SseEmitterRegistry registry) {
        this.registry = registry;
    }

    @Scheduled(fixedDelay = 15_000)
    public void ping() {
        for (Map.Entry<Long, ?> ignored : registry.snapshot().entrySet()) {
            long userId = (Long) ignored.getKey();
            for (SseEmitter emitter : registry.emittersOf(userId)) {
                try {
                    emitter.send(SseEmitter.event().comment("ping"));
                } catch (Exception e) {
                    registry.remove(userId, emitter);
                }
            }
        }
    }
}
