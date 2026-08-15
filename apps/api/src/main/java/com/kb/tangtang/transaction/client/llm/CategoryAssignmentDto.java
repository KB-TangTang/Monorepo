package com.kb.tangtang.transaction.client.llm;

import lombok.Builder;
import lombok.Getter;

/** LLM 이 판정한 거래 하나의 카테고리. categoryId 가 null 이면 "분류 불가"로 판정한 것. */
@Getter
@Builder
public class CategoryAssignmentDto {
    private Long transactionId;
    private Long categoryId;
}
