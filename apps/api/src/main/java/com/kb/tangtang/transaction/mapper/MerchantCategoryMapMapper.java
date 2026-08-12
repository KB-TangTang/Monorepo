package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.MerchantCategoryMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 2순위(가맹점명 매칭)·3순위(MCC/업종명 매칭)에 공용으로 쓴다 — 두 순위 모두 같은 테이블·같은 컬럼을
 * 조회하지만 키로 넘기는 값(정규화된 가맹점명 vs 정규화된 업종명)이 다르다. 카테고리 소스(RULE_MCC 등)
 * 해석은 호출부(TransactionCategorizationServiceImpl)의 책임이다.
 */
@Mapper
public interface MerchantCategoryMapMapper {
    MerchantCategoryMap findByMerchantNameNormalized(@Param("merchantNameNormalized") String merchantNameNormalized);
}
