package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.domain.LlmCategorizationJob;
import com.kb.tangtang.transaction.domain.LlmCategorizationJobItem;
import com.kb.tangtang.transaction.domain.LlmJobStatus;
import com.kb.tangtang.transaction.domain.Transaction;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobItemMapper;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobMapper;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * 이슈 #147 — LLM 분류 작업 등록. {@link LlmCategorizationRequestedListener} 가 @Async 로 호출한다.
 * 초기 동기화의 과거 거래까지 고려해, 사용자별 transaction_date 오름차순 최대 20건 배치로 나눈다.
 */
@Service
public class LlmCategorizationJobServiceImpl implements LlmCategorizationJobService {

    private static final int BATCH_SIZE = 20;

    private final TransactionMapper transactionMapper;
    private final LlmCategorizationJobMapper jobMapper;
    private final LlmCategorizationJobItemMapper jobItemMapper;

    public LlmCategorizationJobServiceImpl(TransactionMapper transactionMapper,
                                           LlmCategorizationJobMapper jobMapper,
                                           LlmCategorizationJobItemMapper jobItemMapper) {
        this.transactionMapper = transactionMapper;
        this.jobMapper = jobMapper;
        this.jobItemMapper = jobItemMapper;
    }

    @Override
    @Transactional
    public void registerPendingJobs(long userId, List<Long> transactionIds) {
        if (transactionIds == null || transactionIds.isEmpty()) {
            return;
        }

        List<Transaction> ordered = transactionMapper.findByIds(transactionIds).stream()
                .sorted(Comparator.comparing(Transaction::getTrDate))
                .collect(java.util.stream.Collectors.toList());

        for (int i = 0; i < ordered.size(); i += BATCH_SIZE) {
            List<Transaction> chunk = ordered.subList(i, Math.min(i + BATCH_SIZE, ordered.size()));
            registerBatch(userId, chunk);
        }
    }

    /**
     * 청크 하나를 작업 한 건으로 등록한다.
     *
     * ⚠ 항목이 하나도 안 붙으면 방금 만든 작업 행을 지운다(이슈 #199 최종 리뷰).
     *   어떤 거래가 UNIQUE 제약에 걸리는지는 insert 를 해 봐야만 알 수 있어(작업 행 먼저 → 항목 시도)
     *   순서를 뒤집을 수 없다. 그런데 배치 스케줄러가 30분마다 도는 지금은, 규칙으로 영영 분류되지
     *   않는 거래들이 매 틱 같은 청크로 다시 올라와 항목은 전부 걸러지고 **빈 PENDING 작업만** 무한히
     *   쌓인다. 성공한 항목 수를 세서 0이면 되돌린다.
     */
    private void registerBatch(long userId, List<Transaction> chunk) {
        LlmCategorizationJob job = LlmCategorizationJob.builder()
                .userId(userId)
                .status(LlmJobStatus.PENDING)
                .transactionCount(chunk.size())
                .build();
        jobMapper.insert(job);

        int registered = 0;
        for (Transaction tx : chunk) {
            LlmCategorizationJobItem item = LlmCategorizationJobItem.builder()
                    .jobId(job.getId())
                    .transactionId(tx.getId())
                    .build();
            try {
                jobItemMapper.insert(item);
                registered++;
            } catch (DuplicateKeyException e) {
                /* 재동기화로 이미 다른(PENDING/PROCESSING) 작업에 등록된 거래 — 중복 등록하지 않고 넘어간다. */
            }
        }

        if (registered == 0) {
            jobMapper.delete(job.getId());
        }
    }
}
