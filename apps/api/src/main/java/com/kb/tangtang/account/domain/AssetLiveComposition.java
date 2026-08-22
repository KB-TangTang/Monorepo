package com.kb.tangtang.account.domain;

import com.kb.tangtang.account.dto.AssetCompositionItemDto;
import com.kb.tangtang.account.dto.AssetGroupItemDto;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 어떤 시점의 라이브 순자산 계산 결과. {@link com.kb.tangtang.account.service.AssetCompositionCalculator} 가 만든다.
 * 요약 API 는 composition·assetGroups 까지 쓰고, 추이 API 는 netWorth·totalDebt 만 쓴다.
 */
@Getter
@Builder
public class AssetLiveComposition {

    private BigDecimal netWorth;
    private BigDecimal totalDebt;
    private List<AssetCompositionItemDto> composition;
    private List<AssetGroupItemDto> assetGroups;
}
