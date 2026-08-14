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

    /**
     * 항목이 하나도 안 붙은 빈 작업을 지운다(이슈 #199 최종 리뷰).
     * 배치가 30분마다 같은 거래를 다시 등록하려 하면 항목은 전부 UNIQUE 위반으로 걸러지는데
     * 작업 행만 매 틱 남아 PENDING 이 무한히 쌓였다. 항목이 0건일 때만 부른다.
     */
    int delete(@Param("id") Long id);
}
