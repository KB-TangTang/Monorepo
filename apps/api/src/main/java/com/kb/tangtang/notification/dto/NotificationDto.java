package com.kb.tangtang.notification.dto;

import lombok.Builder;
import lombok.Getter;

/** 알림 목록의 항목 하나. createdAt 은 ISO-8601 문자열로 내려 프론트가 그대로 파싱한다. */
@Getter
@Builder
public class NotificationDto {
    private final Long id;
    private final String type;
    private final String title;
    private final String content;
    private final String deepLinkUrl;
    private final boolean isRead;
    private final String createdAt;
}
