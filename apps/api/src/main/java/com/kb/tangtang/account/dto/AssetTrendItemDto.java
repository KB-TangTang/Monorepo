package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 최근 6개월 순자산 추이 한 점.
 * netWorth·totalDebt 는 tbl_asset_snapshot 에 해당 월 스냅샷이 없으면 null 이다(자산 스냅샷 미존재 처리 기준).
 * 가장 최근 달(baseDate 가 속한 달)만은 스냅샷 유무와 무관하게 라이브 값을 채운다.
 */
@Getter
@Builder
public class AssetTrendItemDto {

    private String yearMonth;
    private BigDecimal netWorth;
    private BigDecimal totalDebt;
}
