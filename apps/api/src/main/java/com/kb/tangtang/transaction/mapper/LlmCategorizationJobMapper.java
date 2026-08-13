package com.kb.tangtang.transaction.mapper;

import com.kb.tangtang.transaction.domain.LlmCategorizationJob;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface LlmCategorizationJobMapper {
    int insert(LlmCategorizationJob job);

    /** status='PENDING' 인 작업을 created_at 오름차순으로 최대 limit 건. */
    List<LlmCategorizationJob> findPending(@Param("limit") int limit);

    /** PENDING → PROCESSING 전환 + started_at 기록. */
    int markProcessing(@Param("id") Long id, @Param("startedAt") LocalDateTime startedAt);

    /** 작업 종료 처리(COMPLETED 또는 FAILED) + finished_at 기록. */
    int markFinished(@Param("id") Long id, @Param("status") String status,
                      @Param("finishedAt") LocalDateTime finishedAt);
}
