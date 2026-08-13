package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.LlmCategorizationJobItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LlmCategorizationJobItemMapper {
    /** transaction_id 는 전역 UNIQUE. 이미 다른 작업에 등록돼 있으면 DuplicateKeyException. */
    int insert(LlmCategorizationJobItem item);

    /** 이 작업(job)에 속한 거래 id 목록. */
    List<Long> findTransactionIdsByJobId(@Param("jobId") Long jobId);
}
