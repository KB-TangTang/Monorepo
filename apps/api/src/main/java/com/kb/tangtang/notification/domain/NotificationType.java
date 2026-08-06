package com.kb.tangtang.notification.domain;

/**
 * 알림 종류. 값은 tbl_notification.type 컬럼(VARCHAR(30))에 그대로 들어간다.
 *
 * 2026-08-06 기준 실제로 이벤트가 발행되는 것은 ACCOUNT_RECONNECT 하나다.
 * 나머지는 발행 주체(challenge·mission·report·fixedexpense) 백엔드가 아직 없어
 * db/seed_notification_demo.sql 로만 들어간다. (DECISIONS.md 2026-08-06 (3))
 */
public enum NotificationType {

    ACCOUNT_RECONNECT("계좌 재연동이 필요해요"),
    GROUP_JUDGMENT("판결이 확정됐어요"),
    GROUP_TRIAL_OPENED("재판이 열렸어요"),
    MISSION_DEADLINE("오늘 미션 마감 임박"),
    MONTHLY_REPORT("판결문이 도착했어요"),
    PAYMENT_DUE("결제 예정 알림");

    private final String defaultTitle;

    NotificationType(String defaultTitle) {
        this.defaultTitle = defaultTitle;
    }

    public String getDefaultTitle() {
        return defaultTitle;
    }
}
