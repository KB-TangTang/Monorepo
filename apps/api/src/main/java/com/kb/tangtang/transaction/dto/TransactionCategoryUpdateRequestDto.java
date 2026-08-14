package com.kb.tangtang.transaction.dto;

import lombok.Getter;
import lombok.Setter;

/** PATCH /api/transactions/{transactionId}/category 요청 본문. */
@Getter
@Setter
public class TransactionCategoryUpdateRequestDto {

    private Long categoryId;

    /** true면 tbl_user_category_map에도 upsert해 같은 가맹점의 이후 거래에 최우선 적용한다. */
    private boolean applyToMerchant;
}
