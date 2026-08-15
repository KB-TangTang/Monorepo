package com.kb.tangtang.account.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 자산 종류별 목록 카드 한 줄. LOAN 은 amount 가 음수다(composition 과 동일 부호 규칙). */
@Getter
@Builder
public class AssetGroupItemDto {

    private String type;
    private String label;
    private int count;
    private BigDecimal amount;
}
