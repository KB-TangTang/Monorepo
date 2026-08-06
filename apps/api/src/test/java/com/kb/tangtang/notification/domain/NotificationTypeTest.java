package com.kb.tangtang.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTypeTest {

    @Test
    @DisplayName("참고화면의 알림 6종이 모두 정의돼 있다")
    void hasAllSixTypes() {
        assertEquals(6, NotificationType.values().length);
    }

    @Test
    @DisplayName("종류마다 기본 제목이 있다 — 발행자가 제목을 매번 쓰지 않게 한다")
    void everyTypeHasDefaultTitle() {
        for (NotificationType type : NotificationType.values()) {
            assertTrue(type.getDefaultTitle() != null && !type.getDefaultTitle().isBlank(),
                    type + " 의 기본 제목이 비어 있다");
        }
        assertEquals("계좌 재연동이 필요해요", NotificationType.ACCOUNT_RECONNECT.getDefaultTitle());
    }
}
