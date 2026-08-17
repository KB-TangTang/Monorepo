package com.kb.tangtang.account.client.stock;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 토스 액세스 토큰을 들고 있는 단일 저장소.
 *
 * {@link TossAuthScheduler} 가 주기적으로 갱신해서 채워 넣고, 시세 조회 쪽(추후 구현)은
 * {@link #currentToken()} 만 읽는다 — 토큰 발급·갱신 로직을 몰라도 된다.
 */
@Component
public class TossTokenHolder {

    /** 테스트에서만 교체한다(패키지 전용 세터) — 나머지 의존성이 없어 생성자 주입은 과하다. */
    private volatile Clock clock = Clock.systemUTC();

    private final AtomicReference<State> state = new AtomicReference<>();

    public void update(String accessToken, long expiresInSeconds) {
        state.set(new State(accessToken, Instant.now(clock).plusSeconds(expiresInSeconds)));
    }

    /** 유효한 토큰이 없으면(첫 갱신 전이거나 만료) null. 호출부가 "시세 조회 잠시 불가"로 다룬다. */
    public String currentToken() {
        State current = state.get();
        if (current == null || !Instant.now(clock).isBefore(current.expiresAt)) {
            return null;
        }
        return current.accessToken;
    }

    void setClockForTest(Clock clock) {
        this.clock = clock;
    }

    private static final class State {
        private final String accessToken;
        private final Instant expiresAt;

        private State(String accessToken, Instant expiresAt) {
            this.accessToken = accessToken;
            this.expiresAt = expiresAt;
        }
    }
}
