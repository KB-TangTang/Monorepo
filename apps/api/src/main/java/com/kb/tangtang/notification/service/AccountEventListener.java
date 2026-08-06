package com.kb.tangtang.notification.service;

import com.kb.tangtang.account.domain.AccountReconnectRequiredEvent;
import com.kb.tangtang.notification.domain.Notification;
import com.kb.tangtang.notification.domain.NotificationType;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * account 모듈의 이벤트를 받아 알림을 만든다.
 *
 * ⚠ @Async 를 붙인다. 계좌 동기화 응답이 알림 저장을 기다릴 이유가 없고,
 *   알림이 실패해도 동기화는 성공해야 한다.
 *   (동의 철회의 ConsentWithdrawnListener 는 반대로 @Async 를 붙이지 않는다 —
 *    그쪽은 발행자 트랜잭션 안에서 함께 커밋돼야 한다)
 */
@Component
public class AccountEventListener {

    private final NotificationService notificationService;
    private final NotificationSender sender;

    public AccountEventListener(NotificationService notificationService, NotificationSender sender) {
        this.notificationService = notificationService;
        this.sender = sender;
    }

    @Async
    @EventListener
    public void onReconnectRequired(AccountReconnectRequiredEvent event) {
        try {
            Notification saved = notificationService.create(
                    event.userId(),
                    NotificationType.ACCOUNT_RECONNECT,
                    event.bankName() + " · 인증이 만료됐어요",
                    "/asset/accounts/" + event.accountId() + "/reconnect");
            sender.send(saved);
        } catch (Exception e) {
            /* 발행자(계좌 동기화)를 죽이지 않는다. 실패는 DLQ 로만 남긴다 (NT_01_04) */
            sender.sendFailure(NotificationType.ACCOUNT_RECONNECT.name(),
                    "{\"userId\":" + event.userId() + ",\"accountId\":" + event.accountId() + "}",
                    e.getMessage());
        }
    }
}
