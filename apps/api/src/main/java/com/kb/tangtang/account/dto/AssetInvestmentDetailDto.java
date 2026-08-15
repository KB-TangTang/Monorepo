package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 자산 상세(투자) 화면. GET /api/assets/investments 응답. */
@Getter
@Builder
public class AssetInvestmentDetailDto {

    private final BigDecimal totalValuation;
    private final BigDecimal totalCost;
    private final LocalDateTime asOf;
    private final List<AssetInvestmentHoldingDto> holdings;
}
