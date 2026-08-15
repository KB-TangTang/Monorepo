package com.kb.tangtang.challenge.chat.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 채팅 메시지·안 읽은 수 Redis 접근. MySQL 테이블은 만들지 않는다(DECISIONS.md 2026-08-15).
 *
 * <p><b>messageId 와 List 인덱스의 관계가 이 클래스의 핵심이다.</b>
 * seq 는 1부터 INCR 되고 List 는 append 전용(중간 삭제 없음)이라
 * <b>messageId = N 인 메시지는 항상 인덱스 N-1</b> 에 있다. 페이징이 이 대응에 기대고 있다.
 * 메시지를 중간에서 지우는 기능을 넣는다면 이 전제가 깨진다.
 */
@Component
public class ChatMessageStore {

    /** 판결 확정 배치가 챌린지 종료 다음날 돈다. 그 메시지가 도착할 방이 남아 있어야 한다 */
    private static final int TTL_DAYS_AFTER_END = 2;

    /** 같은 방·같은 사용자에게 이 간격 안에서는 알림을 한 번만 보낸다 */
    private static final Duration NOTIFY_COOLDOWN = Duration.ofSeconds(30);

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private final StringRedisTemplate redis;

    public ChatMessageStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 방 개설. TTL 은 여기서 한 번만 걸고 메시지 추가로 갱신하지 않는다 */
    public void initRoom(long groupId, Set<Long> memberIds, LocalDate endDate) {
        if (memberIds.isEmpty()) {
            return;
        }
        String[] ids = memberIds.stream().map(String::valueOf).toArray(String[]::new);
        redis.opsForSet().add(ChatRedisKeys.members(groupId), ids);
        Duration ttl = ttlUntil(endDate);
        redis.expire(ChatRedisKeys.members(groupId), ttl);
        redis.expire(ChatRedisKeys.seq(groupId), ttl);
        redis.expire(ChatRedisKeys.messages(groupId), ttl);
    }

    /** CLOSED 전이 시 즉시 삭제한다. TTL 은 백스톱일 뿐이다 */
    public void deleteRoom(long groupId) {
        List<String> keys = new ArrayList<>(List.of(
                ChatRedisKeys.messages(groupId),
                ChatRedisKeys.seq(groupId),
                ChatRedisKeys.members(groupId)));
        memberIds(groupId).forEach(userId -> keys.add(ChatRedisKeys.unread(groupId, userId)));
        redis.delete(keys);
    }

    public ChatMessage append(long groupId, ChatMessageType type,
                              Long senderId, String senderNickname, String content) {
        Long seq = redis.opsForValue().increment(ChatRedisKeys.seq(groupId));
        if (seq == null) {
            // 사용자 입력 문제가 아니라 Redis 연결·명령 실패이므로 400 이 아닌 500 이다.
            throw new BusinessException("CHAT_SEQ_FAILED", "메시지 번호를 발급하지 못했어요.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        ChatMessage message = new ChatMessage(seq, type, senderId, senderNickname,
                content, LocalDateTime.now());
        redis.opsForList().rightPush(ChatRedisKeys.messages(groupId), toJson(message));
        return message;
    }

    public List<ChatMessage> findRecent(long groupId, int limit) {
        return read(groupId, -(long) limit, -1L);
    }

    /** beforeId 바로 앞 limit 건. messageId N → 인덱스 N-1 이므로 끝 인덱스는 N-2 다 */
    public List<ChatMessage> findBefore(long groupId, long beforeId, int limit) {
        long end = beforeId - 2;
        if (end < 0) {
            return List.of();
        }
        long start = Math.max(0, beforeId - 1 - limit);
        return read(groupId, start, end);
    }

    /** afterId 다음 limit 건. messageId N 의 다음은 인덱스 N 이다 */
    public List<ChatMessage> findAfter(long groupId, long afterId, int limit) {
        long start = Math.max(0, afterId);
        return read(groupId, start, start + limit - 1);
    }

    /**
     * INCR 결과가 1일 때(=이 방·이 사용자 키가 방금 새로 생겼을 때)만 만료 시각을 맞춘다.
     * 기준은 messages 키의 잔여 TTL 이다. initRoom 에서 받은 endDate 를 여기까지 다시 끌고 오지
     * 않기 위해서다 — 방이 살아 있는 한 messages 키가 이미 그 만료 시각을 알고 있으니 그것을 그대로
     * 복제하면 된다. 잔여 TTL 이 -1(무기한)·-2(키 없음)면 아무것도 하지 않는다.
     */
    public void increaseUnread(long groupId, Collection<Long> userIds) {
        userIds.forEach(userId -> {
            Long count = redis.opsForValue().increment(ChatRedisKeys.unread(groupId, userId));
            if (count != null && count == 1L) {
                Long ttlSeconds = redis.getExpire(ChatRedisKeys.messages(groupId), TimeUnit.SECONDS);
                if (ttlSeconds != null && ttlSeconds > 0) {
                    redis.expire(ChatRedisKeys.unread(groupId, userId), Duration.ofSeconds(ttlSeconds));
                }
            }
        });
    }

    public int unreadOf(long groupId, long userId) {
        String raw = redis.opsForValue().get(ChatRedisKeys.unread(groupId, userId));
        return raw == null ? 0 : Integer.parseInt(raw);
    }

    public void clearUnread(long groupId, long userId) {
        redis.delete(ChatRedisKeys.unread(groupId, userId));
    }

    public Set<Long> memberIds(long groupId) {
        Set<String> raw = redis.opsForSet().members(ChatRedisKeys.members(groupId));
        if (raw == null) {
            return Set.of();
        }
        return raw.stream().map(Long::valueOf).collect(Collectors.toCollection(HashSet::new));
    }

    /** true 면 알림을 보내도 된다. 30초 안의 후속 메시지는 false 가 되어 배지만 올라간다 */
    public boolean tryAcquireNotifyCooldown(long groupId, long userId) {
        Boolean acquired = redis.opsForValue()
                .setIfAbsent(ChatRedisKeys.notifyCooldown(groupId, userId), "1", NOTIFY_COOLDOWN);
        return Boolean.TRUE.equals(acquired);
    }

    private List<ChatMessage> read(long groupId, long start, long end) {
        List<String> raw = redis.opsForList().range(ChatRedisKeys.messages(groupId), start, end);
        if (raw == null) {
            return List.of();
        }
        return raw.stream().map(this::fromJson).collect(Collectors.toList());
    }

    /** 종료 다음날 판결 메시지가 들어올 수 있도록 이틀을 더 준다 */
    private Duration ttlUntil(LocalDate endDate) {
        return Duration.between(LocalDateTime.now(),
                endDate.plusDays(TTL_DAYS_AFTER_END).atTime(LocalTime.MAX));
    }

    private String toJson(ChatMessage message) {
        try {
            return MAPPER.writeValueAsString(message);
        } catch (Exception e) {
            // 직렬화 실패는 사용자 입력이 아니라 서버 내부 문제이므로 400 이 아닌 500 이다.
            throw new BusinessException("CHAT_SERIALIZE_FAILED", "메시지를 저장하지 못했어요.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ChatMessage fromJson(String json) {
        try {
            return MAPPER.readValue(json, ChatMessage.class);
        } catch (Exception e) {
            // 역직렬화 실패는 사용자 입력이 아니라 서버 내부 문제이므로 400 이 아닌 500 이다.
            throw new BusinessException("CHAT_DESERIALIZE_FAILED", "메시지를 읽지 못했어요.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
