package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/assets/summary 응답.
 *
 * asOf 는 이 응답을 계산한 단일 기준 시각이다 — netWorth·trend 의 최근월 값·composition·assetGroups가
 * 전부 같은 시점의 라이브 잔액 조회 결과라서 화면 카드 간 금액이 어긋나지 않는다.
 */
@Getter
@Builder
public class AssetSummaryDto {

    private LocalDateTime asOf;
    private BigDecimal netWorth;
    private BigDecimal previousMonthNetWorth;
    private BigDecimal changeAmount;
    private BigDecimal changeRate;
    private List<AssetTrendItemDto> trend;
    private List<AssetCompositionItemDto> composition;
    private List<AssetGroupItemDto> assetGroups;
}
