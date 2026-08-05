package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 연결 계좌 관리 목록의 한 행 (AC_01_04).
 */
@Getter
@Builder
public class ConnectedAccountDto {

    private final long accountId;
    private final String bankCode;
    private final String bankName;
    private final String shortLabel;
    private final String accountName;
    private final String accountNoMasked;
    private final String accountType;
    private final java.math.BigDecimal balance;
    private final String syncStatus;
    private final String lastSyncAt;
    private final String syncFailReason;
    private final String expiresAt;
}
