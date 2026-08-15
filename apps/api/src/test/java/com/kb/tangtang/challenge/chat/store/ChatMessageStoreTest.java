package com.kb.tangtang.challenge.chat.store;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageStoreTest {

    private static final long GROUP_ID = 7L;

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ListOperations<String, String> listOps;
    @Mock private ValueOperations<String, String> valueOps;
    @Mock private SetOperations<String, String> setOps;

    private ChatMessageStore store;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOps);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOps);
        store = new ChatMessageStore(redisTemplate);
    }

    @Test
    @DisplayName("최근 조회는 List 의 뒤에서 limit 만큼 읽는다")
    void findRecentReadsTail() {
        when(listOps.range("chat:messages:7", -50L, -1L)).thenReturn(List.of());

        store.findRecent(GROUP_ID, 50);

        verify(listOps).range("chat:messages:7", -50L, -1L);
    }

    @Test
    @DisplayName("before 조회는 messageId 를 인덱스로 환산한다 (messageId N ↔ 인덱스 N-1)")
    void findBeforeTranslatesIdToIndex() {
        // messageId 21 의 바로 앞 5건 → 인덱스 15..19 (messageId 16..20)
        when(listOps.range("chat:messages:7", 15L, 19L)).thenReturn(List.of());

        store.findBefore(GROUP_ID, 21L, 5);

        verify(listOps).range("chat:messages:7", 15L, 19L);
    }

    @Test
    @DisplayName("before 조회가 List 앞쪽을 넘어가면 0 으로 자른다")
    void findBeforeClampsAtZero() {
        when(listOps.range("chat:messages:7", 0L, 1L)).thenReturn(List.of());

        store.findBefore(GROUP_ID, 3L, 50);

        verify(listOps).range("chat:messages:7", 0L, 1L);
    }

    @Test
    @DisplayName("after 조회는 messageId 다음 인덱스부터 읽는다")
    void findAfterStartsAtNextIndex() {
        // messageId 10 다음 3건 → 인덱스 10..12 (messageId 11..13)
        when(listOps.range("chat:messages:7", 10L, 12L)).thenReturn(List.of());

        store.findAfter(GROUP_ID, 10L, 3);

        verify(listOps).range("chat:messages:7", 10L, 12L);
    }

    @Test
    @DisplayName("append 는 seq 를 발급해 messageId 로 쓰고 JSON 을 RPUSH 한다")
    void appendIncrementsSeqAndPushes() {
        when(valueOps.increment("chat:seq:7")).thenReturn(42L);

        ChatMessage saved = store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕");

        assertEquals(42L, saved.getMessageId());
        assertEquals(ChatMessageType.TEXT, saved.getType());
        verify(listOps).rightPush(eqKey("chat:messages:7"), anyString());
    }

    @Test
    @DisplayName("안 읽은 수는 사용자마다 각자의 키를 올린다")
    void increaseUnreadUsesPerUserKey() {
        store.increaseUnread(GROUP_ID, List.of(3L, 9L));

        verify(valueOps).increment("chat:unread:7:3");
        verify(valueOps).increment("chat:unread:7:9");
    }

    @Test
    @DisplayName("INCR 결과가 1이면 messages 키의 잔여 TTL 로 unread 키에도 만료 시각을 맞춘다")
    void increaseUnreadSetsTtlWhenKeyIsFreshlyCreated() {
        when(valueOps.increment("chat:unread:7:3")).thenReturn(1L);
        when(redisTemplate.getExpire("chat:messages:7", TimeUnit.SECONDS)).thenReturn(3600L);

        store.increaseUnread(GROUP_ID, List.of(3L));

        verify(redisTemplate).expire("chat:unread:7:3", Duration.ofSeconds(3600L));
    }

    @Test
    @DisplayName("INCR 결과가 2 이상이면 이미 만료 시각이 있는 키이므로 expire 를 다시 걸지 않는다")
    void increaseUnreadSkipsTtlWhenKeyAlreadyExists() {
        when(valueOps.increment("chat:unread:7:3")).thenReturn(2L);

        store.increaseUnread(GROUP_ID, List.of(3L));

        verify(redisTemplate, never()).getExpire(anyString(), any(TimeUnit.class));
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("messages 키에 잔여 TTL 이 없으면(-1 무기한, -2 키 없음) expire 를 걸지 않는다")
    void increaseUnreadSkipsTtlWhenMessagesTtlMissing() {
        when(valueOps.increment("chat:unread:7:3")).thenReturn(1L);
        when(redisTemplate.getExpire("chat:messages:7", TimeUnit.SECONDS)).thenReturn(-1L, -2L);

        store.increaseUnread(GROUP_ID, List.of(3L));
        store.increaseUnread(GROUP_ID, List.of(3L));

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("cacheMembers 는 SADD 뒤 members 키에 endDate+2일 TTL 을 건다")
    void cacheMembersSetsTtlFromEndDate() {
        LocalDate endDate = LocalDate.now().plusDays(3);

        store.cacheMembers(GROUP_ID, Set.of(3L, 9L), endDate);

        verify(setOps).add(eqKey("chat:members:7"), any(String[].class));
        org.mockito.ArgumentCaptor<Duration> ttlCaptor = org.mockito.ArgumentCaptor.forClass(Duration.class);
        verify(redisTemplate).expire(eqKey("chat:members:7"), ttlCaptor.capture());
        Duration ttl = ttlCaptor.getValue();
        // endDate(+3일) + 2일 백스톱 ≈ 지금부터 5일. 시각 오차를 감안해 넓게 확인한다.
        assertTrue(ttl.compareTo(Duration.ofDays(4)) > 0 && ttl.compareTo(Duration.ofDays(6)) < 0);
    }

    @Test
    @DisplayName("initRoom 은 seq·messages 키에 expire 를 걸지 않고 members 키에만 TTL 을 건다")
    void initRoomDoesNotExpireSeqAndMessagesKeys() {
        LocalDate endDate = LocalDate.now().plusDays(2);

        store.initRoom(GROUP_ID, Set.of(3L), endDate);

        verify(redisTemplate, never()).expire(eqKey("chat:seq:7"), any(Duration.class));
        verify(redisTemplate, never()).expire(eqKey("chat:messages:7"), any(Duration.class));
        verify(redisTemplate).expire(eqKey("chat:members:7"), any(Duration.class));
    }

    @Test
    @DisplayName("append 는 INCR 결과가 1이면 members 키의 잔여 TTL 을 messages·seq 키에 복제한다")
    void appendCopiesMembersTtlOnFirstMessage() {
        when(valueOps.increment("chat:seq:7")).thenReturn(1L);
        when(redisTemplate.getExpire("chat:members:7", TimeUnit.SECONDS)).thenReturn(3600L);

        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕");

        verify(redisTemplate).expire("chat:messages:7", Duration.ofSeconds(3600L));
        verify(redisTemplate).expire("chat:seq:7", Duration.ofSeconds(3600L));
    }

    @Test
    @DisplayName("append 는 INCR 결과가 2 이상이면 TTL 을 다시 걸지 않는다")
    void appendSkipsTtlOnSubsequentMessages() {
        when(valueOps.increment("chat:seq:7")).thenReturn(2L);

        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕");

        verify(redisTemplate, never()).getExpire(anyString(), any(TimeUnit.class));
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    @DisplayName("append 는 members 키 잔여 TTL 이 0 이하면(-1 무기한, -2 키 없음) TTL 을 걸지 않는다")
    void appendSkipsTtlWhenMembersTtlNotPositive() {
        when(valueOps.increment("chat:seq:7")).thenReturn(1L);
        when(redisTemplate.getExpire("chat:members:7", TimeUnit.SECONDS)).thenReturn(-1L, -2L);

        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕");
        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕2");

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    private static String eqKey(String key) {
        return org.mockito.ArgumentMatchers.eq(key);
    }
}
