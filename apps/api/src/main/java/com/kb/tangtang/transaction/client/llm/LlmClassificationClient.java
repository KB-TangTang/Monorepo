package com.kb.tangtang.transaction.client.llm;

import com.kb.tangtang.transaction.domain.Category;
import com.kb.tangtang.transaction.domain.Transaction;

import java.util.List;

/**
 * LLM 에게 거래 목록을 표준 카테고리 중 하나로 분류해 달라고 요청한다.
 * 어느 LLM 제공자를 쓰는지는 구현체(OpenAiClassificationClient 등)의 책임이다.
 */
public interface LlmClassificationClient {
    /**
     * @param transactions 분류 대상 거래 목록(최대 20건 — 호출부가 배치 크기를 보장한다)
     * @param categories   선택 가능한 표준 카테고리 전체 목록
     * @return transactions 와 같은 수만큼의 판정 결과. 확신이 없으면 categoryId 는 null.
     */
    List<CategoryAssignmentDto> classify(List<Transaction> transactions, List<Category> categories);
}
