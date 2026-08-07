package com.kb.tangtang.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** tbl_notification 한 행. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    private Long id;
    private Long userId;
    private String type;
    private String title;
    private String content;
    private String deepLinkUrl;
    private boolean isRead;
    private LocalDateTime createdAt;
}
