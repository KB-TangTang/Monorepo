package com.kb.tangtang.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationDlqPayload;
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
 *   DLQ 대상은 DB 저장·메시지 변환·사용자 조회 실패다.
 *
 * ⚠ 이 클래스는 **예외를 밖으로 던지지 않는다.** 발행자(계좌 동기화)가 알림 때문에 죽으면 안 된다.
 */
@Component
public class NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(NotificationSender.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final SseEmitterRegistry registry;
    private final NotificationDlqMapper dlqMapper;

    public NotificationSender(SseEmitterRegistry registry, NotificationDlqMapper dlqMapper) {
        this.registry = registry;
        this.dlqMapper = dlqMapper;
    }

    /** 실패하면 DLQ 에 남긴다. 처음 보낼 때 쓰는 경로다. */
    public void send(Notification notification) {
        try {
            deliver(notification);
        } catch (Exception e) {
            log.error("알림 전송 실패 notificationId={} userId={}",
                    notification.getId(), notification.getUserId(), e);
            sendFailure(notification.getType(), payloadOf(notification), e.getMessage());
        }
    }

    /**
     * 실패해도 DLQ 에 새로 쌓지 않고 성공 여부만 알려준다. **DLQ 재처리**가 쓰는 경로다
     * (여기서 DLQ 에 또 넣으면 재시도할 때마다 행이 불어난다).
     *
     * @return 실패가 없었으면 true. 수신자가 접속해 있지 않은 것은 실패가 아니라 true 다 (NT_01_04)
     */
    public boolean trySend(Notification notification) {
        try {
            deliver(notification);
            return true;
        } catch (Exception e) {
            log.error("알림 재전송 실패 notificationId={} userId={}",
                    notification.getId(), notification.getUserId(), e);
            return false;
        }
    }

    /**
     * 접속 중인 모든 연결에 푸시한다.
     *
     * 내려보내는 모양은 REST 목록과 **완전히 같다**(NotificationDto). 도메인을 그대로 실어 보내면
     * 두 경로의 필드 이름·형식이 갈라진다.
     */
    private void deliver(Notification notification) {
        push(notification.getUserId(), "notification", NotificationDto.from(notification));
    }

    /**
     * 저장 없이 접속 중인 연결로 밀기만 한다 (이슈 #174).
     *
     * <p>일반 채팅 알림은 tbl_notification 에 남기지 않는다 — 채팅 로그는 채팅방이 담당하므로
     * 중복 저장을 피한다(DECISIONS.md 2026-08-15). 그래서 저장을 전제로 한 send/trySend 를 쓸 수 없다.
     *
     * <p>기존 deliver() 도 이 메서드를 호출한다. 두 경로의 전송 방식이 갈라지지 않게 하기 위함이다.
     */
    public void push(long userId, String eventName, Object payload) {
        for (SseEmitter emitter : registry.emittersOf(userId)) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (IOException | IllegalStateException e) {
                // 끊긴 연결이다. 실패가 아니라 정리 대상이다 (NT_01_04)
                //
                // ⚠ 여기를 catch(Exception) 으로 넓히지 말 것. 직렬화·변환 실패까지 "연결이 끊겼네" 로
                //   삼켜져 DLQ 에 아무것도 안 남는다. 잡을 예외는 이 두 종류뿐이다.
                registry.remove(userId, emitter);
            }
        }
    }

    /** 재처리에 필요한 최소 정보. 본문은 이미 tbl_notification 에 있다 */
    private String payloadOf(Notification n) {
        return toJson(NotificationDlqPayload.ofSaved(n));
    }

    /** DB 저장·변환·조회 실패를 DLQ 에 남긴다. 이 메서드도 예외를 던지지 않는다. */
    public void sendFailure(String eventType, String payloadJson, String errorMessage) {
        try {
            dlqMapper.insert(eventType, payloadJson, errorMessage);
        } catch (Exception e) {
            log.error("DLQ 적재마저 실패했다. eventType={} error={}", eventType, errorMessage, e);
        }
    }

    /** 직렬화가 깨져도 DLQ 적재 자체는 막지 않는다 — 최소한 실패 사실은 남겨야 한다 */
    static String toJson(NotificationDlqPayload payload) {
        try {
            return OBJECT_MAPPER.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("DLQ payload 직렬화 실패", e);
            return "{}";
        }
    }
}
