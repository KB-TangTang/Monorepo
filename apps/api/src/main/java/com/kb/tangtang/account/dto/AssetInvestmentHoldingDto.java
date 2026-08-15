package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 자산 상세(투자) 목록의 보유 종목 한 행. tbl_investment_holding 한 행을 그대로 옮긴다. */
@Getter
@Builder
public class AssetInvestmentHoldingDto {

    private final long accountId;
    private final String symbol;
    private final String name;
    private final String marketCountry;
    private final String currency;
    private final BigDecimal quantity;
    private final BigDecimal averagePurchasePrice;
    private final BigDecimal lastPrice;
    private final BigDecimal purchaseAmount;
    private final BigDecimal marketValue;
    private final BigDecimal profitLossAmount;
    private final BigDecimal profitLossRate;
}
