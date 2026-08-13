package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.client.llm.CategoryAssignmentDto;
import com.kb.tangtang.transaction.client.llm.LlmClassificationClient;
import com.kb.tangtang.transaction.domain.Category;
import com.kb.tangtang.transaction.domain.CategorySource;
import com.kb.tangtang.transaction.domain.LlmJobStatus;
import com.kb.tangtang.transaction.domain.Transaction;
import com.kb.tangtang.transaction.mapper.CategoryMapper;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobItemMapper;
import com.kb.tangtang.transaction.mapper.LlmCategorizationJobMapper;
import com.kb.tangtang.transaction.mapper.TransactionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 이슈 #147 후속 — LLM 작업 1건 처리.
 *
 * 카테고리 반영은 반드시 TransactionMapper.updateCategory 하나로만 한다 — 재동기화용 update() 를
 * 쓰면 안 된다(그 경로가 카테고리를 지우는 버그였고 이미 고쳤다. 이슈 #147 계획 문서 참고).
 */
@Service
public class LlmCategorizationProcessingServiceImpl implements LlmCategorizationProcessingService {

    private final LlmCategorizationJobMapper jobMapper;
    private final LlmCategorizationJobItemMapper jobItemMapper;
    private final TransactionMapper transactionMapper;
    private final CategoryMapper categoryMapper;
    private final LlmClassificationClient client;
    private final Clock clock;

    @Autowired
    public LlmCategorizationProcessingServiceImpl(LlmCategorizationJobMapper jobMapper,
                                                   LlmCategorizationJobItemMapper jobItemMapper,
                                                   TransactionMapper transactionMapper,
                                                   CategoryMapper categoryMapper,
                                                   LlmClassificationClient client) {
        this(jobMapper, jobItemMapper, transactionMapper, categoryMapper, client, Clock.systemDefaultZone());
    }

    /** 테스트에서 시간을 고정하기 위한 생성자. */
    LlmCategorizationProcessingServiceImpl(LlmCategorizationJobMapper jobMapper,
                                           LlmCategorizationJobItemMapper jobItemMapper,
                                           TransactionMapper transactionMapper,
                                           CategoryMapper categoryMapper,
                                           LlmClassificationClient client,
                                           Clock clock) {
        this.jobMapper = jobMapper;
        this.jobItemMapper = jobItemMapper;
        this.transactionMapper = transactionMapper;
        this.categoryMapper = categoryMapper;
        this.client = client;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void processJob(long jobId) {
        int claimed = jobMapper.markProcessing(jobId, LocalDateTime.now(clock));
        if (claimed == 0) {
            /* 이미 다른 실행 주체가 이 작업을 가져갔다. 중복 처리하지 않는다. */
            return;
        }

        try {
            List<Long> transactionIds = jobItemMapper.findTransactionIdsByJobId(jobId);
            List<Transaction> transactions = transactionMapper.findByIds(transactionIds);
            List<Category> categories = categoryMapper.findAll();
            Set<Long> validCategoryIds = categories.stream().map(Category::getId).collect(Collectors.toSet());

            List<CategoryAssignmentDto> assignments = client.classify(transactions, categories);

            for (CategoryAssignmentDto assignment : assignments) {
                if (assignment.getCategoryId() == null) {
                    continue;
                }
                if (!validCategoryIds.contains(assignment.getCategoryId())) {
                    /* LLM 이 목록에 없는 id 를 지어냈다 — 이 건은 반영하지 않고 넘어간다. */
                    continue;
                }
                transactionMapper.updateCategory(
                        assignment.getTransactionId(), assignment.getCategoryId(), CategorySource.LLM);
            }

            jobMapper.markFinished(jobId, LlmJobStatus.COMPLETED, LocalDateTime.now(clock));
        } catch (RuntimeException e) {
            jobMapper.markFinished(jobId, LlmJobStatus.FAILED, LocalDateTime.now(clock));
            throw e;
        }
    }
}
