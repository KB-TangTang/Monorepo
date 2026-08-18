package com.kb.tangtang.account.client.stock;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    @DisplayName("갱신 전에는 needsRefresh가 true다")
    void needsRefreshBeforeFirstUpdate() {
        TossTokenHolder holder = new TossTokenHolder();
        holder.setClockForTest(fixed(0));

        assertTrue(holder.needsRefresh(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("만료까지 여유가 buffer보다 많이 남으면 needsRefresh는 false다")
    void needsRefreshFalseWhenFarFromExpiry() {
        TossTokenHolder holder = new TossTokenHolder();
        holder.setClockForTest(fixed(0));
        holder.update("tok-1", 86_400);   // 만료: NOW + 86400초

        holder.setClockForTest(fixed(80_000));   // 만료까지 6,400초(1시간=3,600초보다 많이 남음)
        assertFalse(holder.needsRefresh(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("만료가 buffer 안으로 다가오면 needsRefresh는 true다")
    void needsRefreshTrueWithinBuffer() {
        TossTokenHolder holder = new TossTokenHolder();
        holder.setClockForTest(fixed(0));
        holder.update("tok-1", 86_400);   // 만료: NOW + 86400초

        holder.setClockForTest(fixed(83_000));   // 만료까지 3,400초(1시간=3,600초보다 적게 남음)
        assertTrue(holder.needsRefresh(Duration.ofHours(1)));
    }
}
