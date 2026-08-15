package com.kb.tangtang.account.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * tbl_connected_account 를 account_type 별로 묶은 집계 한 행.
 * SECURITIES 는 tbl_investment_holding.market_value 합계를 쓴다 (report 모듈 sumActiveTotalAssets 와 동일 규칙).
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AssetGroupRow {

    private String accountType;
    private int count;
    private BigDecimal amount;
}
