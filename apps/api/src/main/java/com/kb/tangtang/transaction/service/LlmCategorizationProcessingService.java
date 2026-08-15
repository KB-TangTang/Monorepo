package com.kb.tangtang.transaction.service;

public interface LlmCategorizationProcessingService {
    /**
     * PENDING 작업 하나를 처리한다: PROCESSING 전환 → LLM 분류 → category 반영 → COMPLETED/FAILED 마감.
     * 이미 다른 실행 주체가 이 작업을 가져갔으면(PROCESSING 전환 실패) 아무 것도 하지 않는다.
     * LLM 호출 등에서 예외가 나면 작업을 FAILED 로 마감한 뒤 예외를 그대로 다시 던진다 —
     * 호출부(스케줄러)가 로그를 남기고 다음 작업으로 넘어갈 수 있게.
     */
    void processJob(long jobId);
}
