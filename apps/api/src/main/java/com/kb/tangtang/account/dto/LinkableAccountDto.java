package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 연결할 수 있는 계좌 한 건 (AC_01_03).
 * accountNoMasked 는 서버가 마스킹한 값이다 — 원본은 내려보내지 않는다.
 */
@Getter
@Builder
public class LinkableAccountDto {

    private final long accountId;
    private final String bankCode;
    private final String bankName;
    private final String accountType;
    private final String accountName;
    private final String accountNoMasked;
    private final String currency;
    private final java.math.BigDecimal balance;
    private final boolean alreadyLinked;
}
