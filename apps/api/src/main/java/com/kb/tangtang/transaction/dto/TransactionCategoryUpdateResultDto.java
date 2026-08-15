package com.kb.tangtang.transaction.dto;

import lombok.Builder;
import lombok.Getter;

/** 카테고리 수정 후 프론트가 상태를 갱신하는 데 필요한 최소 정보. */
@Getter
@Builder
public class TransactionCategoryUpdateResultDto {

    private Long transactionId;
    private Long categoryId;
    private String categorySource;
    private boolean merchantRuleApplied;
}
