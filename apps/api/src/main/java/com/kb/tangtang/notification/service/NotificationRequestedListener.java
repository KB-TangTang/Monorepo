package com.kb.tangtang.notification.service;

import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationDlqPayload;
import com.kb.tangtang.notification.domain.NotificationRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 모든 도메인의 알림 요청을 처리하는 **단 하나의 리스너** (이슈 #68).
 *
 * 알림 종류가 늘어도 여기에 손댈 일이 없다. 발행하는 쪽이
 * {@link NotificationRequestedEvent} 를 던지고, 문구는 NotificationType 이 만든다.
 *
 * ⚠ @Async 를 붙인다. 발행자(계좌 동기화 등)가 알림 저장을 기다릴 이유가 없고,
 *   알림이 실패해도 발행자의 작업은 성공해야 한다.
 *   (동의 철회의 ConsentWithdrawnListener 는 반대로 @Async 를 붙이지 않는다 —
 *    그쪽은 발행자 트랜잭션 안에서 함께 커밋돼야 한다)
 */
@Component
public class NotificationRequestedListener {

    private final NotificationService notificationService;
    private final NotificationSender sender;

    public NotificationRequestedListener(NotificationService notificationService,
                                         NotificationSender sender) {
        this.notificationService = notificationService;
        this.sender = sender;
    }

    @Async
    @EventListener
    public void onNotificationRequested(NotificationRequestedEvent event) {
        /*
         * 문구를 먼저 만든다. 치환값이 빠지면 여기서 예외가 나고 알림은 저장되지 않는다 —
         * "{bankName} · 인증이 만료됐어요" 같은 반쪽짜리 문구를 사용자에게 보이지 않기 위함이다.
         *
         * ⚠ render 를 try 밖으로 뺄 수 없다(예외를 발행자에게 던지게 된다). 대신 content 를
         *   밖에 두어 **실패 시 DLQ payload 에 지금까지 만든 것을 담는다** — 재처리가 이걸로 알림을 만든다.
         */
        String content = null;
        try {
            content = event.type().render(event.params());
            Notification saved = notificationService.create(
                    event.userId(), event.type(), content, event.deepLinkUrl());
            sender.send(saved);
        } catch (Exception e) {
            /* 발행자를 죽이지 않는다. 실패는 DLQ 로만 남긴다 (NT_01_04) */
            sender.sendFailure(event.type().name(),
                    NotificationSender.toJson(NotificationDlqPayload.ofRequest(
                            event.userId(), event.type(), content, event.deepLinkUrl())),
                    e.getMessage());
        }
    }
}
