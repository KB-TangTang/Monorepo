package com.kb.tangtang.transaction.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** tbl_llm_categorization_job_item 한 행. transaction_id 는 전역 UNIQUE — 중복 등록 방지. */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LlmCategorizationJobItem {
    private Long id;
    private Long jobId;
    private Long transactionId;
}
