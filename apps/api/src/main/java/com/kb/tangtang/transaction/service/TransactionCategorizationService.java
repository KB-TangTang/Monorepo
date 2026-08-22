package com.kb.tangtang.transaction.service;

import com.kb.tangtang.transaction.dto.RuleCategorizationResultDto;

import java.util.List;

public interface TransactionCategorizationService {
    /**
     * 규칙 1~4단계(사용자 매핑 → 공용 매핑 → MCC/업종명 → 키워드)로 거래를 분류한다.
     * account 모듈(FinancialSyncServiceImpl)이 동기 직접 호출한다 — 모듈 경계 원칙의 명시적 예외
     * (ruleCategorizedCount 를 같은 HTTP 응답에 담아야 해서 이벤트로는 불가능하다).
     *
     * @param transactionIds 이번 동기화 호출에서 upsert 된 거래 id 목록
     * @return 규칙으로 분류된 건수, 그리고 규칙으로도 분류하지 못해 LLM 대상이 되는 거래 id 목록
     */
    RuleCategorizationResultDto categorizeRuleBased(long userId, List<Long> transactionIds);
}
