package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 계좌 재동기화 결과 (AC_02_04 / AC_02_06).
 */
@Getter
@Builder
public class ResyncResultDto {

    private final long accountId;
    private final String syncStatus;
    private final String lastSyncAt;
}
