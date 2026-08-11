package com.kb.tangtang.account.client.sync.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class StockAssetSyncDto {
    private final long accountId;
    private final String institutionName;
    private final String currency;
    private final BigDecimal cashBalance;
    private final BigDecimal totalMarketValue;
    private final List<StockHoldingSyncDto> holdings;
}
