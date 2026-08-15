package com.kb.tangtang.transaction.domain;

/** tbl_llm_categorization_job.status 허용값 (ck_lcj_status). */
public final class LlmJobStatus {
    public static final String PENDING = "PENDING";
    public static final String PROCESSING = "PROCESSING";
    public static final String COMPLETED = "COMPLETED";
    public static final String FAILED = "FAILED";

    private LlmJobStatus() {
    }
}
