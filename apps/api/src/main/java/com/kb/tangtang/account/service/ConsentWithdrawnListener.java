package com.kb.tangtang.account.service;

import com.kb.tangtang.user.domain.ConsentWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 금융데이터 수집 동의 철회 → 계좌 연동 해제.
 *
 * user 모듈이 발행한 이벤트를 account 모듈이 받는다. ConsentService 가 AccountLinkService 를
 * 직접 부르지 않게 하기 위한 구조다 (apps/api/AGENTS.md 모듈 경계).
 *
 * ⚠ @Async 를 붙이지 않는다. 발행자의 트랜잭션 안에서 함께 커밋돼야
 * "동의는 철회됐는데 계좌는 그대로" 인 상태가 생기지 않는다.
 *
 * 이슈 #13 이 남긴 TODO(#12) 를 닫는다.
 */
@Component
public class ConsentWithdrawnListener {

    private final AccountLinkService accountLinkService;

    public ConsentWithdrawnListener(AccountLinkService accountLinkService) {
        this.accountLinkService = accountLinkService;
    }

    @EventListener
    public void onConsentWithdrawn(ConsentWithdrawnEvent event) {
        accountLinkService.disconnectAll(event.userId());
    }
}
