package com.kb.tangtang.account.service;

import com.kb.tangtang.account.domain.ProgressStatus;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 진행 상태 보관소 검증.
 * DB 를 쓰지 않는 설계라(설계서 §7.3) TTL·소유자 확인이 이 클래스의 책임 전부다.
 */
class LinkProgressStoreTest {

    private static final Instant NOW = Instant.parse("2026-08-05T09:00:00Z");

    private static Map<String, String> targets() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("0004", "KB국민은행");
        map.put("0088", "신한은행");
        return map;
    }

    private static LinkProgress progress(long userId, String connectionId, Instant createdAt) {
        return new LinkProgress(userId, connectionId, targets(), createdAt);
    }

    @Test
    @DisplayName("보관한 진행 상태를 소유자가 다시 찾는다")
    void putAndGet() {
        LinkProgressStore store = new LinkProgressStore(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        store.put(progress(1L, "conn-1", NOW));

        assertEquals("conn-1", store.get(1L, "conn-1").getConnectionId());
    }

    @Test
    @DisplayName("남의 connectionId 는 찾을 수 없다 - 진행 상황이 새면 안 된다")
    void otherUserCannotRead() {
        LinkProgressStore store = new LinkProgressStore(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        store.put(progress(1L, "conn-1", NOW));

        BusinessException e = assertThrows(BusinessException.class, () -> store.get(2L, "conn-1"));
        assertEquals("CONNECTION_NOT_FOUND", e.getCode());
    }

    @Test
    @DisplayName("TTL 이 지난 진행 상태는 정리된다")
    void evictsExpired() {
        LinkProgressStore store = new LinkProgressStore(Clock.fixed(NOW, ZoneId.of("Asia/Seoul")));
        store.put(progress(1L, "old", NOW.minus(LinkProgressStore.TTL).minusSeconds(1)));
        store.put(progress(1L, "fresh", NOW));

        assertThrows(BusinessException.class, () -> store.get(1L, "old"));
        assertEquals("fresh", store.get(1L, "fresh").getConnectionId());
        assertEquals(1, store.size());
    }

    @Test
    @DisplayName("실패도 끝난 것으로 세어 진행률이 멈추지 않는다")
    void failedCountsAsFinished() {
        LinkProgress progress = progress(1L, "conn-1", NOW);

        assertEquals(0, progress.percent());
        assertFalse(progress.isDone());

        progress.mark("0004", ProgressStatus.DONE);
        assertEquals(50, progress.percent());

        progress.mark("0088", ProgressStatus.FAILED);
        assertEquals(100, progress.percent());
        assertTrue(progress.isDone());
    }

    @Test
    @DisplayName("다음 조회 대상은 선택한 순서대로 나온다")
    void nextWaitingKeepsOrder() {
        LinkProgress progress = progress(1L, "conn-1", NOW);

        assertEquals("0004", progress.nextWaiting());
        progress.mark("0004", ProgressStatus.DONE);
        assertEquals("0088", progress.nextWaiting());
        progress.mark("0088", ProgressStatus.DONE);
        assertNull(progress.nextWaiting());
    }
}
