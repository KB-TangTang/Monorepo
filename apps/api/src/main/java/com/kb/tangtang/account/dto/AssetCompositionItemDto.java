package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 순자산 구성 도넛차트 한 조각. LOAN 은 amount 가 음수다. */
@Getter
@Builder
public class AssetCompositionItemDto {

    private String type;
    private String label;
    private BigDecimal amount;
}
