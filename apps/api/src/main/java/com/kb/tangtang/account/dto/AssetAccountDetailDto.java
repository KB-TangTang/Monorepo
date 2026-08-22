package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 자산 상세 목록(입출금·예적금·페이머니) 한 종류 분.
 * GET /api/assets/accounts?type= 응답.
 */
@Getter
@Builder
public class AssetAccountDetailDto {

    private final BigDecimal total;
    private final List<ConnectedAccountDto> accounts;
}
