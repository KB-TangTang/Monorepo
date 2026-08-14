package com.kb.tangtang.transaction.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * tbl_merchant_category_map 한 행. 전역 가맹점→카테고리 매핑.
 * source 는 'MCC'/'KEYWORD'/'LLM' 중 하나이며(ck_mcm_source), tbl_transaction.category_source 값과는
 * 다른 어휘다 — 호출부가 변환한다(CategorySource 참고).
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantCategoryMap {
    private Long id;
    private String merchantNameNormalized;
    private Long categoryId;
    private String source;
}
