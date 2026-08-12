package com.kb.tangtang.account.domain;

import java.util.List;

/**
 * 규칙 1~4단계로도 분류하지 못한 소비 거래를 LLM 분류 작업으로 등록해 달라는 요청.
 *
 * account 모듈이 발행하고 transaction 모듈이 받는다(ConsentWithdrawnEvent 와 같은 패턴 —
 * apps/api/AGENTS.md 모듈 경계). FinancialSyncServiceImpl 이 HTTP 응답을 기다리게 하지 않으려고
 * 이벤트로 분리했다 — 실제 작업 등록은 @Async 리스너가 한다.
 *
 * @param userId         대상 사용자
 * @param transactionIds LLM 분류 대상 거래 id 목록 (규칙 1~4단계 전부 미스, 환불 아님)
 */
public record LlmCategorizationRequestedEvent(Long userId, List<Long> transactionIds) {
}
