package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 즉시 조회 결과의 기관 한 줄 (AC_02_03).
 */
@Getter
@Builder
public class RefreshInstitutionDto {

    private final String bankCode;
    private final String bankName;
    private final String shortLabel;
    private final int newTransactionCount;
    /** 이 기관 계좌들의 잔액 **합계**. 첫 계좌 값이 아니다. */
    private final java.math.BigDecimal balance;
    /** 이 기관 조회 결과 (SyncStatus). 실패하면 화면이 그 사실을 말해야 한다. */
    private final String syncStatus;
}
