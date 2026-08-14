package com.kb.tangtang.account.client.sync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PooledScenarioKeyProviderTest {

    @Test
    @DisplayName("풀이 하나뿐이면 어떤 userId든 그 값을 돌려준다")
    void singlePool() {
        PooledScenarioKeyProvider provider = new PooledScenarioKeyProvider(List.of("1"));

        assertEquals("1", provider.resolve(1L));
        assertEquals("1", provider.resolve(999L));
    }

    @Test
    @DisplayName("풀이 여러 개면 userId % size 로 나눠 고른다")
    void multiplePool() {
        PooledScenarioKeyProvider provider = new PooledScenarioKeyProvider(List.of("1", "2", "3"));

        assertEquals("1", provider.resolve(3L));  // 3 % 3 = 0 -> "1"
        assertEquals("2", provider.resolve(4L));  // 4 % 3 = 1 -> "2"
        assertEquals("3", provider.resolve(5L));  // 5 % 3 = 2 -> "3"
    }

    @Test
    @DisplayName("풀이 비어 있으면 생성 시점에 예외를 던진다")
    void emptyPoolRejectedAtConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new PooledScenarioKeyProvider(List.of()));
    }
}
