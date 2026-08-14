package com.kb.tangtang.account.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * tbl_investment_holding 한 행.
 *
 * ⚠ marketCountry: 목서버 StockHoldingSyncDto 에 국가 정보가 없다. 현재 시드된 보유종목은 전부
 *   국내 종목(삼성전자·SK하이닉스)이라 고정값 "KR" 을 채운다 — 문서화된 단순화이지 임시방편이 아니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class InvestmentHolding {
    private Long id;
    private Long userId;
    private Long accountId;
    private String symbol;
    private String name;
    private String marketCountry;
    private String currency;
    private BigDecimal quantity;
    private BigDecimal averagePurchasePrice;
    private BigDecimal lastPrice;
    private BigDecimal purchaseAmount;
    private BigDecimal marketValue;
    private BigDecimal profitLossAmount;
    private BigDecimal profitLossRate;

    public InvestmentHolding(Long id, Long userId, Long accountId, String symbol, String name,
                              String marketCountry, String currency, BigDecimal quantity,
                              BigDecimal averagePurchasePrice, BigDecimal lastPrice,
                              BigDecimal purchaseAmount, BigDecimal marketValue,
                              BigDecimal profitLossAmount, BigDecimal profitLossRate) {
        this.id = id;
        this.userId = userId;
        this.accountId = accountId;
        this.symbol = symbol;
        this.name = name;
        this.marketCountry = marketCountry;
        this.currency = currency;
        this.quantity = quantity;
        this.averagePurchasePrice = averagePurchasePrice;
        this.lastPrice = lastPrice;
        this.purchaseAmount = purchaseAmount;
        this.marketValue = marketValue;
        this.profitLossAmount = profitLossAmount;
        this.profitLossRate = profitLossRate;
    }
}
