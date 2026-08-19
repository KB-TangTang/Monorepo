package com.kb.tangtang.account.client.stock;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 토스증권 현재가 조회 응답 한 종목분. */
@Getter
@Builder
public class TossPriceDto {
    private final String symbol;
    private final BigDecimal lastPrice;
    private final String currency;
    private final String timestamp;
}
