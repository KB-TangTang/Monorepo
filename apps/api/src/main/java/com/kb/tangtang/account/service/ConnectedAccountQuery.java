package com.kb.tangtang.account.service;

import com.kb.tangtang.account.mapper.ConnectedAccountMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 모듈이 계좌 보유 여부만 물어볼 때 쓰는 **좁은 조회 창구**.
 *
 * <p>온보딩 게이트(user 모듈)가 "계좌를 연동했는가"를 알아야 하는데, 그것 하나 때문에
 * {@link AccountLinkService} 를 통째로 주입하면 금융 데이터 클라이언트·이벤트 발행기까지 딸려온다.
 * 읽기 전용 질문 하나만 노출해 결합을 최소로 둔다.
 * (apps/api/AGENTS.md 「모듈 간 직접 호출 최소화」 · DECISIONS.md 2026-08-11 (7))
 *
 * <p>⚠ 여기에 쓰기·연동 로직을 넣지 말 것. 늘어나면 모듈 경계가 사라진다.
 */
@Service
public class ConnectedAccountQuery {

    private final ConnectedAccountMapper mapper;

    public ConnectedAccountQuery(ConnectedAccountMapper mapper) {
        this.mapper = mapper;
    }

    /** 활성 연결 계좌가 하나라도 있는지. 온보딩 게이트의 판정 근거다. */
    @Transactional(readOnly = true)
    public boolean hasActiveAccount(long userId) {
        return !mapper.findActiveByUser(userId).isEmpty();
    }
}
