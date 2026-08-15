package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.UserCategoryMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 1순위: 사용자가 이 가맹점에 과거 직접 지정한 카테고리 (tbl_user_category_map). */
@Mapper
public interface UserCategoryMapMapper {

    UserCategoryMap findByUserAndMerchant(@Param("userId") long userId,
                                          @Param("merchantNameNormalized") String merchantNameNormalized);

    /**
     * 가맹점 규칙 upsert. tbl_user_category_map의 uk_ucm_user_merchant(user_id,
     * merchant_name_normalized) 유니크 키를 이용해 insert/update를 한 문장으로 겸한다.
     */
    int upsert(@Param("userId") long userId, @Param("merchantNameNormalized") String merchantNameNormalized,
               @Param("categoryId") Long categoryId);
}
