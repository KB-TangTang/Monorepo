package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.dto.NotificationDto;
import com.kb.tangtang.notification.mapper.NotificationDlqMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * 저장된 알림을 접속 중인 사용자에게 전달한다.
 *
 * ⚠ NT_01_04: **사용자의 SSE 미연결은 실패가 아니다.** DLQ 에 넣지 않는다.
 *   넣으면 로그인하지 않은 사용자 수만큼 DLQ 가 쌓인다.
 *   전송 중 연결이 끊기는 것(IOException·IllegalStateException)도 실패가 아니다 — 그 연결만 정리한다.
 *   DLQ 대상은 DB 저장·메시지 변환·사용자 조회 실패다. 그래서 catch 를 둘로 나눠 둔다.
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

    /**
     * 접속 중인 모든 연결에 푸시한다.
     *
     * 내려보내는 모양은 REST 목록과 **완전히 같다**(NotificationDto). 도메인을 그대로 실어 보내면
     * 두 경로의 필드 이름·형식이 갈라진다.
     */
    public void send(Notification notification) {
        try {
            NotificationDto payload = NotificationDto.from(notification);

            for (SseEmitter emitter : registry.emittersOf(notification.getUserId())) {
                try {
                    emitter.send(SseEmitter.event().name("notification").data(payload));
                } catch (IOException | IllegalStateException e) {
                    /*
                     * 끊긴(혹은 이미 완료된) 연결이다. 실패가 아니라 정리 대상이다 (NT_01_04).
                     * ⚠ 여기를 catch(Exception) 으로 넓히지 말 것 — 직렬화·변환 실패까지
                     *   "연결이 끊겼네" 로 삼켜져 DLQ 에 아무것도 남지 않는다.
                     */
                    registry.remove(notification.getUserId(), emitter);
                } catch (Exception e) {
                    /* 직렬화·메시지 변환 실패. 이건 진짜 실패다 → DLQ */
                    log.error("알림 전송 실패 notificationId={} userId={}",
                            notification.getId(), notification.getUserId(), e);
                    sendFailure(notification.getType(), payloadOf(notification), e.getMessage());
                }
            }
        } catch (Exception e) {
            /* 변환 자체가 깨졌거나 예상 못한 실패다. 발행자에게는 절대 던지지 않는다 */
            log.error("알림 전송 준비 실패 notificationId={} userId={}",
                    notification.getId(), notification.getUserId(), e);
            sendFailure(notification.getType(), payloadOf(notification), e.getMessage());
        }
    }

    /** DLQ 에 남길 최소 식별자. 본문은 이미 tbl_notification 에 있다 */
    private String payloadOf(Notification n) {
        return "{\"notificationId\":" + n.getId() + ",\"userId\":" + n.getUserId() + "}";
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
