package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.MerchantKeywordRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 4순위: 가맹점명/적요 부분매칭 키워드 규칙. */
@Mapper
public interface MerchantKeywordRuleMapper {
    /** haystack 에 keyword_normalized 가 부분 포함되는 규칙 중 가장 긴(구체적인) 것 하나. */
    MerchantKeywordRule findBestMatch(@Param("haystack") String normalizedHaystack);
}
