package com.kb.tangtang.account.client.stock;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
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

    /**
     * 토큰이 없거나(첫 갱신 전) buffer 안으로 만료가 다가왔으면 true.
     *
     * TossAuthScheduler 가 이 메서드로 "지금 갱신이 필요한지"만 자주(예: 5분마다) 확인하고,
     * 실제 토스 호출은 필요할 때만 한다 — 그래야 갱신이 한 번 실패해도 다음 확인 주기(몇 분 뒤)에
     * 바로 재시도되고, 23시간을 꽉 채운 고정 주기처럼 하루 종일 방치되지 않는다(QA 지적사항).
     */
    public boolean needsRefresh(Duration buffer) {
        State current = state.get();
        return current == null || !Instant.now(clock).isBefore(current.expiresAt.minus(buffer));
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
