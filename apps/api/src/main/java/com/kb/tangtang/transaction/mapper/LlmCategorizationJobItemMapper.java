package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.LlmCategorizationJobItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmCategorizationJobItemMapper {
    /** transaction_id 는 전역 UNIQUE. 이미 다른 작업에 등록돼 있으면 DuplicateKeyException. */
    int insert(LlmCategorizationJobItem item);
}
