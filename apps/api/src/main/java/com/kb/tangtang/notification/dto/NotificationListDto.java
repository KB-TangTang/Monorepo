package com.kb.tangtang.notification.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** GET /api/notifications 응답. nextCursor 가 null 이면 더 없다. */
@Getter
@Builder
public class NotificationListDto {
    private final List<NotificationDto> items;
    private final String nextCursor;
    private final int unreadCount;
}
