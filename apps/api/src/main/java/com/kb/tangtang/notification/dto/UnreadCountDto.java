package com.kb.tangtang.notification.dto;

import lombok.Builder;
import lombok.Getter;

/** 읽음 처리 응답. 프론트가 배지를 재조회하지 않도록 개수를 함께 돌려준다. */
@Getter
@Builder
public class UnreadCountDto {
    private final int unreadCount;
}
