package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 연결 해제 결과.
 */
@Getter
@Builder
public class DisconnectResultDto {

    private final long accountId;
    private final boolean disconnected;
}
