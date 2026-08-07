package com.kb.tangtang.notification.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * tbl_notification_dlq.payload_json 의 모양. **재처리에 필요한 최소 정보만** 담는다.
 *
 * 실패 지점에 따라 채워지는 필드가 다르다.
 * <ul>
 *   <li>알림 저장 전에 실패 → {@code userId · type · content · deepLinkUrl}
 *       (재처리 때 알림을 새로 만든다)</li>
 *   <li>저장은 됐고 푸시만 실패 → {@code notificationId · userId}
 *       (재처리 때 그 알림을 다시 푸시한다. 본문은 tbl_notification 에 이미 있다)</li>
 * </ul>
 *
 * 둘 다 없으면 다시 만들 수 없는 행이다 — 재처리기가 재시도 횟수만 올려 대상에서 빠지게 둔다.
 * 모르는 필드는 무시한다. 예전 형식(자유 형식 문자열)으로 쌓인 행이 배치를 깨지 않게 하기 위함이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record NotificationDlqPayload(
        Long notificationId,
        Long userId,
        String type,
        String content,
        String deepLinkUrl) {

    /** 저장까지 실패한 경우 — 알림을 새로 만들어야 한다 */
    public static NotificationDlqPayload ofRequest(Long userId, NotificationType type,
                                                   String content, String deepLinkUrl) {
        return new NotificationDlqPayload(null, userId, type == null ? null : type.name(),
                content, deepLinkUrl);
    }

    /** 저장은 됐고 푸시만 실패한 경우 — 그 알림을 다시 푸시하면 된다 */
    public static NotificationDlqPayload ofSaved(Notification notification) {
        return new NotificationDlqPayload(notification.getId(), notification.getUserId(),
                notification.getType(), null, null);
    }

    /** 이미 저장된 알림을 가리키는가 */
    public boolean pointsToSavedNotification() {
        return notificationId != null && userId != null;
    }

    /** 알림을 새로 만들 수 있는가 */
    public boolean canRebuild() {
        return notificationId == null && userId != null && type != null && content != null;
    }
}
