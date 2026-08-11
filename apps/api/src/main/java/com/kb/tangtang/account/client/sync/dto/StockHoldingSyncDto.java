package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class StockHoldingSyncDto {
    private final String productCode;
    private final String productName;
    private final BigDecimal quantity;
    private final BigDecimal averagePurchasePrice;
    private final BigDecimal lastPrice;
    private final BigDecimal purchaseAmount;
    private final BigDecimal marketValue;
    private final BigDecimal profitLossAmount;
    private final BigDecimal profitLossRate;
}
