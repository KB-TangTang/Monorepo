package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.LlmCategorizationJob;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LlmCategorizationJobMapper {
    int insert(LlmCategorizationJob job);
}
