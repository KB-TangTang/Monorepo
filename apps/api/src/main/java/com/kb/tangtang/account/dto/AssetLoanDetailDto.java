package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** 자산 상세(대출) 화면. GET /api/assets/loans 응답. */
@Getter
@Builder
public class AssetLoanDetailDto {

    private final BigDecimal total;
    private final List<AssetLoanItemDto> loans;
}
