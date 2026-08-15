package com.kb.tangtang.transaction.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 장부 목록 조회 전용 조인 결과 한 행. tbl_transaction 에 tbl_category/tbl_card/
 * tbl_connected_account 를 LEFT JOIN 해서 화면이 필요로 하는 표시용 원본 컬럼만 담는다.
 * TransactionQueryService 가 이 값을 TransactionListItemDto(부호 있는 amount, 결제수단 라벨)로 가공한다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class TransactionListRow {
    private Long id;
    private LocalDate trDate;
    private String merchantName;
    private String categoryName;
    private String classification;
    private BigDecimal amount;
    private boolean isRefund;
    private BigDecimal refundedAmount;
    private String direction;
    private String sourceType;
    /** tbl_card.card_type_code. 01=신용카드, 02=체크카드. source_type 이 카드가 아니면 null. */
    private String cardTypeCode;
    private String cardInstitutionName;
    private String accountBankName;

    public TransactionListRow(Long id, LocalDate trDate, String merchantName, String categoryName,
                               String classification, BigDecimal amount, boolean isRefund,
                               BigDecimal refundedAmount, String direction, String sourceType,
                               String cardTypeCode, String cardInstitutionName, String accountBankName) {
        this.id = id;
        this.trDate = trDate;
        this.merchantName = merchantName;
        this.categoryName = categoryName;
        this.classification = classification;
        this.amount = amount;
        this.isRefund = isRefund;
        this.refundedAmount = refundedAmount;
        this.direction = direction;
        this.sourceType = sourceType;
        this.cardTypeCode = cardTypeCode;
        this.cardInstitutionName = cardInstitutionName;
        this.accountBankName = accountBankName;
    }
}
