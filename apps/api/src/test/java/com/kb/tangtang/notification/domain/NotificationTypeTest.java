package com.kb.tangtang.notification.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationTypeTest {

    /*
     * 개수를 못 박는 단언이라 종류가 늘면 반드시 깨진다. 의도된 설계다.
     * 문구·제목은 NotificationType 이 소유하므로(발행자는 치환값만 넘긴다), 종류만 추가하고
     * 문구를 빠뜨리는 것을 이 테스트가 잡는다. 늘렸으면 이 숫자와 아래 설명을 함께 고친다.
     */
    @Test
    @DisplayName("알림 13종이 모두 정의돼 있다. 참고화면 6종 + 그룹챌린지 시작·미성립 2종(#152)"
            + " + 그룹챌린지 변론 등록 1종(#174) + 개인 미션 3종(#282) + 그룹챌린지 방장 삭제 1종(#352)")
    void hasAllTypes() {
        assertEquals(13, NotificationType.values().length);
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

    @Test
    @DisplayName("문구 템플릿의 {자리표시자} 를 발행자가 넘긴 값으로 채운다")
    void rendersTemplateWithParams() {
        assertEquals("국민은행 · 인증이 만료됐어요",
                NotificationType.ACCOUNT_RECONNECT.render(Map.of("bankName", "국민은행")));
    }

    @Test
    @DisplayName("자리표시자에 넘길 값이 없으면 문구를 반쯤 만들지 않고 거부한다")
    void rejectsMissingParam() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> NotificationType.ACCOUNT_RECONNECT.render(Map.of()));
        assertTrue(e.getMessage().contains("bankName"), "빠진 자리표시자 이름을 알려줘야 한다: " + e.getMessage());
    }

    @Test
    @DisplayName("문구가 아직 정해지지 않은 종류는 {content} 로 발행자 문구를 그대로 쓴다")
    void passesThroughContentWhenTemplateUndecided() {
        assertEquals("배달 재판 · 내 사건 유죄 4:2",
                NotificationType.GROUP_JUDGMENT.render(Map.of("content", "배달 재판 · 내 사건 유죄 4:2")));
    }

    @Test
    @DisplayName("종류마다 문구 템플릿이 있다 — 문구가 흩어지지 않게 여기에 모은다")
    void everyTypeHasContentTemplate() {
        for (NotificationType type : NotificationType.values()) {
            assertTrue(type.getContentTemplate() != null && !type.getContentTemplate().isBlank(),
                    type + " 의 문구 템플릿이 비어 있다");
        }
    }
}
