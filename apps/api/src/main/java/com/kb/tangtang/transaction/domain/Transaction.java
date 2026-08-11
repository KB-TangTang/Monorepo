package com.kb.tangtang.transaction.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * tbl_transaction 한 행.
 *
 * codefTrKey 는 멱등키다. 포맷: "{sourceType}-{상품ID}-{원천거래식별자}"
 * (예: "CARD_CHECK-12-APV20260810001"). FinancialSyncService 가 조립해서 채운다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class Transaction {
    private Long id;
    private Long userId;
    private Long accountId;
    private String codefTrKey;
    private String merchantName;
    private String merchantNameNormalized;
    private BigDecimal amount;
    private String direction;
    private LocalDate trDate;
    private LocalTime trTime;
    private String description1;
    private String classification;
    private Long categoryId;
    private Boolean isExcludedFromSummary;
    private Long loanId;
    private String sourceType;
    private Long cardId;
    private String correlationId;
    private Long linkedTransactionId;
    private String originalApprovalNo;
    private String merchantCategoryCode;
    private String merchantCategoryName;
    private String rawJson;

    public Transaction(Long id, Long userId, Long accountId, String codefTrKey, String merchantName,
                        String merchantNameNormalized, BigDecimal amount, String direction,
                        LocalDate trDate, LocalTime trTime, String description1, String classification,
                        Long categoryId, Boolean isExcludedFromSummary, Long loanId, String sourceType,
                        Long cardId, String correlationId, Long linkedTransactionId,
                        String originalApprovalNo, String merchantCategoryCode,
                        String merchantCategoryName, String rawJson) {
        this.id = id;
        this.userId = userId;
        this.accountId = accountId;
        this.codefTrKey = codefTrKey;
        this.merchantName = merchantName;
        this.merchantNameNormalized = merchantNameNormalized;
        this.amount = amount;
        this.direction = direction;
        this.trDate = trDate;
        this.trTime = trTime;
        this.description1 = description1;
        this.classification = classification;
        this.categoryId = categoryId;
        this.isExcludedFromSummary = isExcludedFromSummary;
        this.loanId = loanId;
        this.sourceType = sourceType;
        this.cardId = cardId;
        this.correlationId = correlationId;
        this.linkedTransactionId = linkedTransactionId;
        this.originalApprovalNo = originalApprovalNo;
        this.merchantCategoryCode = merchantCategoryCode;
        this.merchantCategoryName = merchantCategoryName;
        this.rawJson = rawJson;
    }
}
