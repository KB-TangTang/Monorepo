package com.kb.tangtang.account.domain;

/**
 * 연결 계좌의 인증이 만료돼 재연동이 필요해졌을 때 발행한다.
 *
 * account 모듈이 notification 모듈을 직접 부르지 않기 위한 통로다.
 * (apps/api/AGENTS.md 모듈 경계 — ConsentWithdrawnEvent 와 같은 방식)
 */
public record AccountReconnectRequiredEvent(Long userId, Long accountId, String bankName) {
}
