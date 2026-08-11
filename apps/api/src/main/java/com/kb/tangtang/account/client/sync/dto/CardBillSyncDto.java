package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CardBillSyncDto {
    private final long billId;
    private final String billingMonth;
    private final String dueDate;
    private final String billStatusCode;
    private final String billStatusName;
    private final BigDecimal totalAmount;
    private final BigDecimal paidAmount;
}
