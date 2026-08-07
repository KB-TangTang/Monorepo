package com.kb.tangtang.notification.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** tbl_notification_dlq 한 행. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDlqRow {
    private Long dlqId;
    private String originalEventType;
    private String payloadJson;
    private String errorMessage;
    private int retryCount;
    private LocalDateTime regDate;
}
