package com.kb.tangtang.account.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FinancialSyncHistory {
    private Long userId;
    private String status;
    private String syncedSourcesJson;
    private String failedSource;
    private String failReason;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
