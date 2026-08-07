package com.kb.tangtang.notification.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kb.tangtang.notification.domain.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

/**
 * 알림 목록의 항목 하나. createdAt 은 ISO-8601 문자열로 내려 프론트가 그대로 파싱한다.
 *
 * ⚠ REST 목록과 SSE 푸시가 **같은 이 타입**을 내려보낸다. 도메인(Notification)을 그대로
 *   직렬화하면 두 경로의 필드 이름·형식이 갈라진다 (실제로 갈라져 있었다).
 *   변환은 {@link #from(Notification)} 한 곳뿐이다.
 */
@Getter
@Builder
public class NotificationDto {
    private final Long id;
    private final String type;
    private final String title;
    private final String content;
    private final String deepLinkUrl;

    /*
     * ⚠ boolean isRead 는 Lombok 이 isRead() 를 만들고, Jackson 은 그 getter 를 "read" 로 읽는다.
     *   프론트는 item.isRead 를 본다 → 이름을 고정하지 않으면 항상 undefined 다.
     *   getter 에 직접 붙여야 한다. 필드에만 붙이면 "isRead"(필드)와 "read"(getter)가
     *   서로 다른 프로퍼티로 잡혀 둘 다 나간다.
     */
    @Getter(onMethod_ = @JsonProperty("isRead"))
    private final boolean isRead;

    private final String createdAt;

    /** 도메인 → 응답 변환. REST·SSE 가 공유하는 유일한 매핑이다. */
    public static NotificationDto from(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .title(n.getTitle())
                .content(n.getContent())
                .deepLinkUrl(n.getDeepLinkUrl())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt() == null ? null
                        : n.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
}
