package com.kb.tangtang.challenge.chat.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ChatMessageStore.class);

    /** 판결 확정 배치가 챌린지 종료 다음날 돈다. 그 메시지가 도착할 방이 남아 있어야 한다 */
    private static final int TTL_DAYS_AFTER_END = 2;

    /**
     * TTL 하한.
     *
     * <p>종료일 + 2일이 이미 지난 방은 계산 결과가 <b>음수</b>가 되는데, 음수 TTL 로 EXPIRE 를 걸면
     * Redis 가 키를 즉시 지운다. TTL 앵커(members 키)가 사라지면 그 뒤의 append 가 messages·seq 에
     * TTL 을 복제하지 못해 <b>만료 없는 키가 영구히 쌓인다.</b> 이 브랜치에는 ACTIVE/JUDGING→CLOSED
     * 전이가 없어 종료일 지난 방이 계속 ACTIVE 로 남으므로 희귀 케이스가 아니다.
     *
     * <p>그래서 계산값이 0 이하이거나 앵커의 잔여 TTL 을 읽지 못한 경우 이 값으로 클램프한다.
     * "지금부터 하루 뒤 만료" 는 방을 쓰는 동안 대화가 사라지지 않으면서도 반드시 만료되는 값이다.
     */
    private static final Duration MIN_TTL = Duration.ofDays(1);

    /** 같은 방·같은 사용자에게 이 간격 안에서는 알림을 한 번만 보낸다 */
    private static final Duration NOTIFY_COOLDOWN = Duration.ofSeconds(30);

    /**
     * 타임스탬프를 숫자 배열이 아니라 ISO-8601 문자열로 저장한다.
     * 기본 설정이면 {@code sentAt} 이 {@code [2026,8,16,14,43,11,138088000]} 로 들어가
     * redis-cli 로 들여다볼 때 읽을 수 없고, 다른 언어에서 이 키를 읽을 때도 걸린다.
     * REST·STOMP 응답이 모두 ISO 문자열이라 저장 형식도 거기에 맞춘다.
     * 읽기는 배열 형식도 그대로 복원되므로 이미 저장된 메시지와 호환된다.
     */
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private final StringRedisTemplate redis;

    public ChatMessageStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /**
     * 방 개설. TTL 앵커는 members 키다(cacheMembers 참고).
     * seq·messages 키는 아직 존재하지 않아 여기서 expire 를 걸어도 Redis 가 no-op 으로 무시한다
     * (EXPIRE 는 없는 키에는 아무 효과가 없다). 두 키는 각자 처음 생성되는 append 에서 TTL 을 받는다.
     */
    public void initRoom(long groupId, Set<Long> memberIds, LocalDate endDate) {
        if (memberIds.isEmpty()) {
            return;
        }
        cacheMembers(groupId, memberIds, endDate);
    }

    /**
     * 참여자 캐시를 채우고 TTL 앵커로 쓴다(members 키). seq·messages 키는 append 에서 이 키의
     * 잔여 TTL 을 복제해 따라간다.
     */
    public void cacheMembers(long groupId, Set<Long> memberIds, LocalDate endDate) {
        if (memberIds.isEmpty()) {
            return;
        }
        String[] ids = memberIds.stream().map(String::valueOf).toArray(String[]::new);
        redis.opsForSet().add(ChatRedisKeys.members(groupId), ids);
        redis.expire(ChatRedisKeys.members(groupId), ttlUntil(endDate));
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
        if (seq == 1L) {
            // 이 방의 첫 메시지 — seq·messages 키가 INCR·RPUSH 로 방금 새로 생겼다.
            // append 는 verifyCanEnter → memberIdsOf 를 거친 뒤에만 호출되므로 members 키(TTL 앵커)는
            // 항상 살아 있다는 전제 위에서, 그 잔여 TTL 을 그대로 복제한다.
            // 앵커가 없거나(-2) 무기한(-1)이면 하한으로 대신 건다 — 여기서 건너뛰면 영구 키가 된다.
            Duration ttl = ttlFrom(ChatRedisKeys.members(groupId), groupId);
            redis.expire(ChatRedisKeys.messages(groupId), ttl);
            redis.expire(ChatRedisKeys.seq(groupId), ttl);
        }
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
     * 복제하면 된다. 잔여 TTL 이 -1(무기한)·-2(키 없음)면 하한({@link #MIN_TTL})으로 대신 건다 —
     * 걸지 않고 넘어가면 안 읽은 수 키가 영구히 남는다.
     */
    public void increaseUnread(long groupId, Collection<Long> userIds) {
        userIds.forEach(userId -> {
            Long count = redis.opsForValue().increment(ChatRedisKeys.unread(groupId, userId));
            if (count != null && count == 1L) {
                redis.expire(ChatRedisKeys.unread(groupId, userId),
                        ttlFrom(ChatRedisKeys.messages(groupId), groupId));
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

    /**
     * 앵커 키의 잔여 TTL 을 그대로 쓰되, 읽지 못했거나(-2 키 없음) 무기한(-1)이면 하한으로 대체한다.
     * 이 메서드는 <b>항상 양수 Duration</b> 을 돌려준다 — 호출부가 "TTL 을 안 거는" 분기를 갖지
     * 않게 하기 위함이다(만료 없는 키가 생기는 경로를 없앤다).
     */
    private Duration ttlFrom(String anchorKey, long groupId) {
        Long ttlSeconds = redis.getExpire(anchorKey, TimeUnit.SECONDS);
        if (ttlSeconds != null && ttlSeconds > 0) {
            return Duration.ofSeconds(ttlSeconds);
        }
        log.warn("채팅 TTL 앵커의 잔여 시간을 쓸 수 없어 하한({})으로 만료를 건다. groupId={} key={} ttl={}",
                MIN_TTL, groupId, anchorKey, ttlSeconds);
        return MIN_TTL;
    }

    /**
     * 종료 다음날 판결 메시지가 들어올 수 있도록 이틀을 더 준다.
     *
     * <p>이미 지난 종료일이면 음수가 나오는데, 그대로 EXPIRE 에 넘기면 키가 즉시 삭제된다
     * ({@link #MIN_TTL} 참고). 0 이하는 하한으로 올린다.
     */
    private Duration ttlUntil(LocalDate endDate) {
        Duration ttl = Duration.between(LocalDateTime.now(),
                endDate.plusDays(TTL_DAYS_AFTER_END).atTime(LocalTime.MAX));
        if (ttl.isZero() || ttl.isNegative()) {
            log.warn("종료일이 지난 채팅방의 TTL 이 {} 라 하한({})으로 올린다. endDate={}", ttl, MIN_TTL, endDate);
            return MIN_TTL;
        }
        return ttl;
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
