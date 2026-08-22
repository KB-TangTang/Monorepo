package com.kb.tangtang.transaction.service;

import java.util.List;

public interface LlmCategorizationJobService {
    /** 거래를 transaction_date 오름차순으로 정렬해 최대 20건씩 묶어 LLM 작업으로 등록한다. */
    void registerPendingJobs(long userId, List<Long> transactionIds);
}
