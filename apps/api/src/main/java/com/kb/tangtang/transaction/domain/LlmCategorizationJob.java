package com.kb.tangtang.transaction.domain;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/** tbl_llm_categorization_job 한 행. 사용자별 transaction_date 오름차순 최대 20건 배치의 헤더. */
@Getter
@Setter
@Builder
@NoArgsConstructor
public class LlmCategorizationJob {
    private Long id;
    private Long userId;
    private String status;
    private Integer transactionCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public LlmCategorizationJob(Long id, Long userId, String status, Integer transactionCount,
                                 LocalDateTime startedAt, LocalDateTime finishedAt) {
        this.id = id;
        this.userId = userId;
        this.status = status;
        this.transactionCount = transactionCount;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }
}
