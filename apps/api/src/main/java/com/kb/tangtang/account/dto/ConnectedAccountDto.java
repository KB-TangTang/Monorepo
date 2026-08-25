package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

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
    /** 카드 행 전용(#467). `01`=신용, `02`=체크. 화면이 잔액 자리에 카드 종류를 그린다. */
    private final String cardTypeCode;
    private final BigDecimal balance;
    private final String syncStatus;
    private final String lastSyncAt;
    private final String syncFailReason;
    private final String expiresAt;
    /** tbl_connected_account 행이 아닌 대출 표시 행은 개별 해제·재조회 대상이 아니다. */
    @Builder.Default
    private final boolean manageable = true;
}
