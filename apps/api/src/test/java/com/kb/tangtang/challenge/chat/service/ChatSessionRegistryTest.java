package com.kb.tangtang.challenge.chat.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSessionRegistryTest {

    private ChatSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ChatSessionRegistry();
    }

    @Test
    @DisplayName("입장하면 방의 접속자 목록에 들어간다")
    void tracksEnteredUser() {
        registry.enter("s1", 7L, 3L);

        assertEquals(Set.of(3L), registry.activeUserIds(7L));
    }

    @Test
    @DisplayName("나가면 목록에서 빠진다")
    void removesOnLeave() {
        registry.enter("s1", 7L, 3L);

        registry.leave("s1");

        assertTrue(registry.activeUserIds(7L).isEmpty());
    }

    @Test
    @DisplayName("같은 사용자가 탭 두 개로 들어와도 한 세션이 끊기면 나머지는 남는다")
    void keepsUserWhileAnotherSessionAlive() {
        registry.enter("s1", 7L, 3L);
        registry.enter("s2", 7L, 3L);

        registry.leave("s1");

        assertEquals(Set.of(3L), registry.activeUserIds(7L));
    }

    @Test
    @DisplayName("모르는 세션을 지워도 터지지 않는다")
    void ignoresUnknownSession() {
        registry.leave("nope");

        assertTrue(registry.activeUserIds(7L).isEmpty());
    }

    @Test
    @DisplayName("접속자가 없는 방은 빈 집합이다")
    void emptyForUnknownGroup() {
        assertTrue(registry.activeUserIds(99L).isEmpty());
    }

    @Test
    @DisplayName("여러 스레드가 동시에 입장·퇴장해도 예외 없이 최종 상태가 일관된다")
    void handlesConcurrentEnterAndLeave() throws InterruptedException {
        int userCount = 50;
        // 풀 크기를 작업 수보다 작게 두면 아직 시작하지 못한 작업이 ready.countDown() 을 못 해
        // ready.await() 가 영원히 끝나지 않는다(데드락). 모든 작업이 동시에 뜰 수 있도록
        // 풀 크기를 작업 수와 맞춘다.
        ExecutorService pool = Executors.newFixedThreadPool(userCount);
        CountDownLatch ready = new CountDownLatch(userCount);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger failures = new AtomicInteger();

        for (int i = 0; i < userCount; i++) {
            long userId = i;
            String sessionId = "s" + i;
            pool.submit(() -> {
                try {
                    ready.countDown();
                    start.await();
                    registry.enter(sessionId, 7L, userId);
                    registry.activeUserIds(7L);
                    if (userId % 2 == 0) {
                        registry.leave(sessionId);
                    }
                } catch (Exception e) {
                    failures.incrementAndGet();
                }
            });
        }

        ready.await();
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));

        assertEquals(0, failures.get());
        // 홀수 userId(25개)는 leave 하지 않았으니 남아 있어야 한다
        assertEquals(25, registry.activeUserIds(7L).size());
    }
}
