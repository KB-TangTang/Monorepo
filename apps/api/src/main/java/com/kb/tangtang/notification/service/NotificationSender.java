package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 저장된 알림을 접속 중인 사용자에게 전달한다.
 *
 * ⚠ NT_01_04: **사용자의 SSE 미연결은 실패가 아니다.** DLQ 에 넣지 않는다.
 *   넣으면 로그인하지 않은 사용자 수만큼 DLQ 가 쌓인다.
 *   전송 중 연결이 끊기는 것(IOException)도 실패가 아니다 — 그 연결만 정리한다.
 *   DLQ 대상은 DB 저장·메시지 변환·사용자 조회 실패다.
 *
 * ⚠ 이 클래스는 **예외를 밖으로 던지지 않는다.** 발행자(계좌 동기화)가 알림 때문에 죽으면 안 된다.
 */
@Component
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);

    private final SseEmitterRegistry registry;
    private final NotificationDlqMapper dlqMapper;

    public NotificationSender(SseEmitterRegistry registry, NotificationDlqMapper dlqMapper) {
        this.registry = registry;
        this.dlqMapper = dlqMapper;
    }

    public void send(Notification notification) {
        for (SseEmitter emitter : registry.emittersOf(notification.getUserId())) {
            try {
                emitter.send(SseEmitter.event().name("notification").data(notification));
            } catch (Exception e) {
                /* 끊긴 연결이다. 실패가 아니라 정리 대상이다 */
                registry.remove(notification.getUserId(), emitter);
            }
        }
    }

    /** DB 저장·변환·조회 실패를 DLQ 에 남긴다. 이 메서드도 예외를 던지지 않는다. */
    public void sendFailure(String eventType, String payloadJson, String errorMessage) {
        try {
            dlqMapper.insert(eventType, payloadJson, errorMessage);
        } catch (Exception e) {
            log.error("DLQ 적재마저 실패했다. eventType={} error={}", eventType, errorMessage, e);
        }
    }
}
