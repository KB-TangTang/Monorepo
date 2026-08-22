package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CardSyncDto {
    private final long cardId;
    private final String institutionCode;
    private final String institutionName;
    private final String cardNoMasked;
    private final String productName;
    private final String cardProductCode;
    private final String cardTypeCode;
    private final String cardStatusCode;
    private final String currency;
    private final String issuedAt;
}
