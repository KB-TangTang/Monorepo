package com.kb.tangtang.account.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** tbl_card 한 행. */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class Card {
    private Long id;
    private Long userId;
    private String institutionCode;
    private String institutionName;
    private String cardNoMasked;
    private String productName;
    private String cardProductCode;
    private String cardTypeCode;
    private String cardStatusCode;
    private String currency;
    private LocalDate issuedAt;
    private LocalDateTime lastSyncAt;

    public Card(Long id, Long userId, String institutionCode, String institutionName,
                String cardNoMasked, String productName, String cardProductCode,
                String cardTypeCode, String cardStatusCode, String currency,
                LocalDate issuedAt, LocalDateTime lastSyncAt) {
        this.id = id;
        this.userId = userId;
        this.institutionCode = institutionCode;
        this.institutionName = institutionName;
        this.cardNoMasked = cardNoMasked;
        this.productName = productName;
        this.cardProductCode = cardProductCode;
        this.cardTypeCode = cardTypeCode;
        this.cardStatusCode = cardStatusCode;
        this.currency = currency;
        this.issuedAt = issuedAt;
        this.lastSyncAt = lastSyncAt;
    }
}
