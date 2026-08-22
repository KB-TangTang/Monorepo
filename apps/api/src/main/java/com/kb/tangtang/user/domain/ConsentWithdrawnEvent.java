package com.kb.tangtang.user.domain;

/**
 * 동의 철회 이벤트.
 *
 * user 모듈이 발행하고 account 모듈이 받는다. **모듈 간 직접 Service 호출을 피하기 위한 것**이다
 * (apps/api/AGENTS.md 모듈 경계). ConsentService 가 AccountLinkService 를 직접 부르면
 * user → account 의존이 생기고, 나중에 transaction·notification 도 같은 처리를 해야 할 때
 * ConsentService 가 계속 커진다.
 *
 * ⚠ @Async 를 쓰지 않는다. 철회와 연동 해제는 같은 트랜잭션에서 함께 성공하거나 함께 실패해야 한다.
 * 비동기로 돌리면 "동의는 철회됐는데 계좌는 그대로" 인 상태가 생긴다.
 *
 * @param userId      철회한 사용자
 * @param consentType 철회한 동의 항목 (ConsentType 이름)
 */
public record ConsentWithdrawnEvent(Long userId, String consentType) {
}
