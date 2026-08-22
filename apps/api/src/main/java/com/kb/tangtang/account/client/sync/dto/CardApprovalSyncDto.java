package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class CardApprovalSyncDto {
    private final long approvalId;
    private final String approvalNo;
    private final String approvedAt;
    private final String approvalTypeCode;
    private final String merchantName;
    private final String merchantCategoryCode;
    private final String merchantCategoryName;
    private final BigDecimal approvedAmount;
    private final String rawJson;
}
