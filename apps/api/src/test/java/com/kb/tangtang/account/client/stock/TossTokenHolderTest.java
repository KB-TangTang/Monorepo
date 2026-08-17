package com.kb.tangtang.account.client.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TossTokenHolderTest {

    private static final Instant NOW = Instant.parse("2026-08-18T00:00:00Z");

    private Clock fixed(long plusSeconds) {
        return Clock.fixed(NOW.plusSeconds(plusSeconds), ZoneOffset.UTC);
    }

    @Test
    @DisplayName("갱신 전에는 토큰이 없다")
    void noTokenBeforeFirstUpdate() {
        TossTokenHolder holder = new TossTokenHolder();
        holder.setClockForTest(fixed(0));

        assertNull(holder.currentToken());
    }

    @Test
    @DisplayName("만료 전에는 갱신한 토큰을 그대로 돌려준다")
    void returnsTokenBeforeExpiry() {
        TossTokenHolder holder = new TossTokenHolder();
        holder.setClockForTest(fixed(0));
        holder.update("tok-1", 86_400);

        holder.setClockForTest(fixed(86_399));
        assertEquals("tok-1", holder.currentToken());
    }

    @Test
    @DisplayName("만료 시각이 지나면 null을 돌려준다")
    void returnsNullAfterExpiry() {
        TossTokenHolder holder = new TossTokenHolder();
        holder.setClockForTest(fixed(0));
        holder.update("tok-1", 86_400);

        holder.setClockForTest(fixed(86_400));
        assertNull(holder.currentToken());
    }

    @Test
    @DisplayName("다시 갱신하면 이전 토큰을 대체한다")
    void updateReplacesPreviousToken() {
        TossTokenHolder holder = new TossTokenHolder();
        holder.setClockForTest(fixed(0));
        holder.update("tok-1", 86_400);
        holder.update("tok-2", 86_400);

        assertEquals("tok-2", holder.currentToken());
    }
}
