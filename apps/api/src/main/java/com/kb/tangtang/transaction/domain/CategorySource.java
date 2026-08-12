package com.kb.tangtang.transaction.domain;

/**
 * tbl_transaction.category_source 허용값 (ck_tx_category_source, db/migration/20260813_add_transaction_category_source.sql).
 * tbl_merchant_category_map.source 의 값(MCC/KEYWORD/LLM)과 문자열이 다르므로 절대 그대로 복사하지 않는다.
 */
public final class CategorySource {
    public static final String USER = "USER";
    public static final String RULE_MCC = "RULE_MCC";
    public static final String RULE_KEYWORD = "RULE_KEYWORD";
    public static final String LLM = "LLM";

    private CategorySource() {
    }
}
