package com.kb.tangtang.transaction.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/** tbl_user_category_map 한 행. 사용자가 특정 가맹점에 직접 지정한 카테고리(전역 매핑보다 우선). */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserCategoryMap {
    private Long id;
    private Long userId;
    private String merchantNameNormalized;
    private Long categoryId;
}
