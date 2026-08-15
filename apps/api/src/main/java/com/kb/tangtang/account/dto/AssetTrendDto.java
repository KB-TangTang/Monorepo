package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/assets/trend 응답.
 *
 * asOf 는 이 응답을 계산한 단일 기준 시각이다 — trend 의 마지막(최신) 달 값은 이 시각의 라이브
 * 계산 결과이고, GET /api/assets/summary 의 asOf·netWorth 와 같은 계산 로직(AssetCompositionCalculator·
 * AssetNetWorthTrendService)을 공유한다.
 */
@Getter
@Builder
public class AssetTrendDto {

    private LocalDateTime asOf;
    private List<AssetTrendItemDto> trend;
}
