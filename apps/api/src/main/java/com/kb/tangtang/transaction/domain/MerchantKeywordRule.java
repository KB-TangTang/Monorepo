package com.kb.tangtang.transaction.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** tbl_merchant_keyword_rule 한 행. keywordNormalized 가 길수록 더 구체적인 것으로 보고 우선한다. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MerchantKeywordRule {
    private Long id;
    private String keyword;
    private String keywordNormalized;
    private Long categoryId;
}
