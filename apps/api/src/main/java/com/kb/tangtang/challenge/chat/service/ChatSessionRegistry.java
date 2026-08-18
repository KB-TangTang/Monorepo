package com.kb.tangtang.challenge.chat.service;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 방별로 지금 채팅방을 열어 둔 사용자.
 *
 * <p>두 곳에서 쓴다. 접속 중인 사람에게는 (1) 안 읽은 수를 올리지 않고 (2) 알림을 보내지 않는다.
 * 읽고 있는 사람에게 배지와 알림이 쌓이면 그게 버그로 보인다.
 *
 * <p>SseEmitterRegistry 와 같은 이유로 인메모리다 — 단일 프로세스 전제(ARCHITECTURE 2026-07-26).
 * 같은 사용자의 다중 탭을 허용하므로 세션 단위로 세고, 마지막 세션이 끊길 때 목록에서 뺀다.
 *
 * <p><b>동시성:</b> 여러 STOMP 세션 스레드가 동시에 enter/leave/activeUserIds 를 호출한다.
 * sessionId 를 키로 하는 {@link ConcurrentHashMap} 하나만 두고, 방별 접속자는 매 호출마다
 * 이 맵을 필터링해 만든다 — "맵 안의 컬렉션에 add/remove" 형태(ConcurrentHashMap&lt;Long, Set&lt;Long&gt;&gt;)를
 * 쓰지 않으므로 그 패턴에서 흔한 복합 연산 레이스(별도 동기화 없이 컴퓨팅한 컬렉션에 add 하다가
 * 다른 스레드의 remove 와 겹치는 문제, 마지막 원소가 빠졌는데 빈 Set 이 맵에 남아 누수되는 문제)가
 * 애초에 발생하지 않는다. activeUserIds 가 반환하는 HashSet 은 그 호출 안에서만 채워지는
 * 지역 변수라 다른 스레드와 공유되지 않으므로 HashSet 이어도 안전하다.
 */
@Component
public class ChatSessionRegistry {

    /** sessionId → 어느 방의 누구인가 */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public void enter(String sessionId, long groupId, long userId) {
        sessions.put(sessionId, new Session(groupId, userId));
    }

    public void leave(String sessionId) {
        sessions.remove(sessionId);
    }

    public Set<Long> activeUserIds(long groupId) {
        Set<Long> result = new HashSet<>();
        sessions.values().forEach(session -> {
            if (session.groupId == groupId) {
                result.add(session.userId);
            }
        });
        return result;
    }

    private static final class Session {

        private final long groupId;
        private final long userId;

        private Session(long groupId, long userId) {
            this.groupId = groupId;
            this.userId = userId;
        }
    }
}
