package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 연결 계좌 목록 + 다음 자동 동기화 예정 시각.
 */
@Getter
@Builder
public class ConnectedAccountListDto {

    private final java.util.List<ConnectedAccountDto> accounts;
    private final String nextAutoSyncAt;
}
