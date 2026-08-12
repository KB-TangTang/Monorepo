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

    private void registerBatch(long userId, List<Transaction> chunk) {
        LlmCategorizationJob job = LlmCategorizationJob.builder()
                .userId(userId)
                .status(LlmJobStatus.PENDING)
                .transactionCount(chunk.size())
                .build();
        jobMapper.insert(job);

        for (Transaction tx : chunk) {
            LlmCategorizationJobItem item = LlmCategorizationJobItem.builder()
                    .jobId(job.getId())
                    .transactionId(tx.getId())
                    .build();
            try {
                jobItemMapper.insert(item);
            } catch (DuplicateKeyException e) {
                /* 재동기화로 이미 다른(PENDING/PROCESSING) 작업에 등록된 거래 — 중복 등록하지 않고 넘어간다. */
            }
        }
    }
}
