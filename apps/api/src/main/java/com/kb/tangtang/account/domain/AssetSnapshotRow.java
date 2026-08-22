package com.kb.tangtang.account.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * tbl_asset_snapshot 에서 조회한 (year_month, net_worth, total_debt) 한 행.
 * net_worth 가 NULL 인 행은 매퍼 쿼리 단계에서 걸러진다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssetSnapshotRow {

    private String yearMonth;
    private BigDecimal netWorth;
    private BigDecimal totalDebt;
}
