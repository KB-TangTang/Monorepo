package com.kb.tangtang.account.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** tbl_card_bill 한 행. */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class CardBill {
    private Long id;
    private Long cardId;
    private String billingMonth;
    private LocalDate dueDate;
    private String billStatusCode;
    private String billStatusName;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private String rawJson;

    public CardBill(Long id, Long cardId, String billingMonth, LocalDate dueDate,
                     String billStatusCode, String billStatusName, BigDecimal totalAmount,
                     BigDecimal paidAmount, String rawJson) {
        this.id = id;
        this.cardId = cardId;
        this.billingMonth = billingMonth;
        this.dueDate = dueDate;
        this.billStatusCode = billStatusCode;
        this.billStatusName = billStatusName;
        this.totalAmount = totalAmount;
        this.paidAmount = paidAmount;
        this.rawJson = rawJson;
    }
}
