package com.kb.tangtang.challenge.chat.store;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.challenge.chat.domain.ChatSystemType;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
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

    /** append 는 이제 Lua 스크립트 한 번으로 끝난다. 목에서는 그 반환값만 흉내 낸다 */
    @SuppressWarnings("unchecked")
    private void whenScriptReturns(Long seq) {
        lenient().when(redisTemplate.execute(any(RedisScript.class), anyList(),
                any(Object.class), any(Object.class))).thenReturn(seq);
    }

    @SuppressWarnings("unchecked")
    private List<String> capturedKeys() {
        ArgumentCaptor<List<String>> captor = ArgumentCaptor.forClass(List.class);
        verify(redisTemplate).execute(any(RedisScript.class), captor.capture(),
                any(Object.class), any(Object.class));
        return captor.getValue();
    }

    /**
     * 스크립트에 넘어간 ARGV. [0]=번호 자리를 비운 JSON, [1]=잘라 낼 위치.
     *
     * <p>가변인자는 캡터가 원소별로 하나씩 받는다 — {@code Object[]} 로 잡으면 배열째 들어와
     * 캐스팅에서 깨진다.
     */
    @SuppressWarnings("unchecked")
    private List<String> capturedArgs() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(redisTemplate).execute(any(RedisScript.class), anyList(),
                captor.capture(), captor.capture());
        return captor.getAllValues().stream().map(String::valueOf).toList();
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
    @DisplayName("append 는 번호 발급과 저장을 스크립트 하나로 한다 (seq · messages 순서)")
    void appendRunsSingleScript() {
        whenScriptReturns(42L);

        ChatMessage saved = store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕");

        assertEquals(42L, saved.getMessageId());
        assertEquals(ChatMessageType.TEXT, saved.getType());
        assertEquals(List.of("chat:seq:7", "chat:messages:7"), capturedKeys(),
                "KEYS 순서가 스크립트(INCR KEYS[1] · RPUSH KEYS[2])와 반대면 방이 통째로 망가진다");
        verify(listOps, never()).rightPush(anyString(), anyString());
    }

    /*
     * 이 테스트가 이 파일에서 가장 중요하다.
     *
     * 스크립트는 JSON 앞부분을 잘라 내고 실제 번호를 끼워 넣는데, 자를 위치를 호출부가 넘긴다.
     * 그 위치가 한 글자만 어긋나도 **읽을 수 없는 JSON 이 방에 영구히 남는다** — List 는 중간
     * 삭제가 없어 되돌릴 수 없고, 그 방의 조회는 그때부터 통째로 실패한다.
     *
     * 목으로는 Lua 가 실행되지 않으므로, 스크립트가 하는 계산을 그대로 재현해 결과를 되읽는다.
     */
    @Test
    @DisplayName("스크립트가 잘라 낼 위치가 맞다 — 잘라 붙인 JSON 이 같은 메시지로 되읽힌다")
    void appendBodyIsCutAtTheRightOffset() throws Exception {
        whenScriptReturns(42L);

        store.append(GROUP_ID, ChatMessageType.SYSTEM, null, null, "판결이 확정됐어요",
                ChatSystemType.VERDICT_CONFIRMED, "/group-challenges/7/trial/55", "2026-재판-0055");

        List<String> args = capturedArgs();
        String body = args.get(0);
        int offset = Integer.parseInt(args.get(1));

        /* 스크립트 본문과 같은 조립: '{"messageId":' .. seq .. ',' .. string.sub(body, offset) */
        String stored = "{\"messageId\":42," + body.substring(offset - 1);

        ChatMessage read = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .readValue(stored, ChatMessage.class);

        assertEquals(42L, read.getMessageId());
        assertEquals("판결이 확정됐어요", read.getContent());
        assertEquals(ChatSystemType.VERDICT_CONFIRMED, read.getSystemType());
        assertEquals("2026-재판-0055", read.getCaseNo());
    }

    /*
     * 사용자가 채팅에 messageId 를 그대로 쳐도 스크립트는 본문을 뒤지지 않는다.
     * 문자열을 패턴으로 찾아 바꾸는 방식이었다면 여기서 엉뚱한 자리가 치환된다.
     */
    @Test
    @DisplayName("본문에 messageId 라고 적어도 잘라 내는 위치는 변하지 않는다")
    void appendIgnoresLookalikeContent() throws Exception {
        whenScriptReturns(7L);

        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "{\"messageId\":0,장난");

        List<String> args = capturedArgs();
        String stored = "{\"messageId\":7," + args.get(0).substring(Integer.parseInt(args.get(1)) - 1);

        ChatMessage read = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .readValue(stored, ChatMessage.class);

        assertEquals(7L, read.getMessageId());
        assertEquals("{\"messageId\":0,장난", read.getContent());
    }

    @Test
    @DisplayName("스크립트가 번호를 못 주면 저장하지 않고 500 을 낸다")
    void appendFailsWhenScriptReturnsNull() {
        whenScriptReturns(null);

        BusinessException e = assertThrows(BusinessException.class,
                () -> store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕"));

        assertEquals("CHAT_SEQ_FAILED", e.getCode());
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

    /*
     * 리뷰 I1: 예전에는 여기서 expire 를 "걸지 않고" 넘어갔다. 그러면 안 읽은 수 키가 만료 없이
     * 영구히 남는다. 앵커의 TTL 을 못 읽으면 하한(1일)으로라도 반드시 만료를 건다.
     */
    @Test
    @DisplayName("messages 키에 잔여 TTL 이 없으면(-1 무기한, -2 키 없음) 하한 TTL 로 만료를 건다")
    void increaseUnreadFallsBackToMinimumTtlWhenMessagesTtlMissing() {
        when(valueOps.increment("chat:unread:7:3")).thenReturn(1L);
        when(redisTemplate.getExpire("chat:messages:7", TimeUnit.SECONDS)).thenReturn(-1L, -2L);

        store.increaseUnread(GROUP_ID, List.of(3L));
        store.increaseUnread(GROUP_ID, List.of(3L));

        verify(redisTemplate, org.mockito.Mockito.times(2))
                .expire("chat:unread:7:3", Duration.ofDays(1));
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
        whenScriptReturns(1L);
        when(redisTemplate.getExpire("chat:members:7", TimeUnit.SECONDS)).thenReturn(3600L);

        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕");

        verify(redisTemplate).expire("chat:messages:7", Duration.ofSeconds(3600L));
        verify(redisTemplate).expire("chat:seq:7", Duration.ofSeconds(3600L));
    }

    @Test
    @DisplayName("append 는 INCR 결과가 2 이상이면 TTL 을 다시 걸지 않는다")
    void appendSkipsTtlOnSubsequentMessages() {
        whenScriptReturns(2L);

        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕");

        verify(redisTemplate, never()).getExpire(anyString(), any(TimeUnit.class));
        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    /*
     * 리뷰 I1: 예전에는 TTL 을 걸지 않고 넘어가 messages·seq 키가 영구히 남았다.
     * ttlUntil 이 음수를 돌려주면 members 키가 EXPIRE 로 즉시 삭제돼(-2) 실제로 밟히는 경로다.
     */
    @Test
    @DisplayName("append 는 members 키 잔여 TTL 이 0 이하면(-1 무기한, -2 키 없음) 하한 TTL 로 만료를 건다")
    void appendFallsBackToMinimumTtlWhenMembersTtlNotPositive() {
        whenScriptReturns(1L);
        when(redisTemplate.getExpire("chat:members:7", TimeUnit.SECONDS)).thenReturn(-1L, -2L);

        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕");
        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕2");

        verify(redisTemplate, org.mockito.Mockito.times(2))
                .expire("chat:messages:7", Duration.ofDays(1));
        verify(redisTemplate, org.mockito.Mockito.times(2))
                .expire("chat:seq:7", Duration.ofDays(1));
    }

    /*
     * 리뷰 I1 (경계 케이스): 종료일 + 2일이 이미 지난 방.
     * 예전에는 Duration 이 음수로 나가 EXPIRE 가 members 키를 즉시 지웠고, 그 직후 append 가
     * TTL 앵커를 잃어 messages·seq 를 영구 키로 만들었다. 이 브랜치엔 ACTIVE→CLOSED 전이가 없어
     * 종료일 지난 그룹이 계속 ACTIVE 로 남으므로 흔한 경로다.
     */
    @Test
    @DisplayName("종료일이 이미 지난 방도 음수가 아닌 TTL 을 건다")
    void cacheMembersClampsExpiredRoomTtlToPositive() {
        LocalDate longGone = LocalDate.now().minusDays(30);

        store.cacheMembers(GROUP_ID, Set.of(3L), longGone);

        org.mockito.ArgumentCaptor<Duration> ttlCaptor = org.mockito.ArgumentCaptor.forClass(Duration.class);
        verify(redisTemplate).expire(eqKey("chat:members:7"), ttlCaptor.capture());
        Duration ttl = ttlCaptor.getValue();
        assertTrue(!ttl.isNegative() && !ttl.isZero(), "음수·0 TTL 은 키를 즉시 삭제한다. 실제: " + ttl);
        assertEquals(Duration.ofDays(1), ttl);
    }

    @Test
    @DisplayName("종료일이 오늘이어도(하루도 안 남아도) 양수 TTL 을 건다")
    void cacheMembersKeepsPositiveTtlOnLastDay() {
        store.cacheMembers(GROUP_ID, Set.of(3L), LocalDate.now());

        org.mockito.ArgumentCaptor<Duration> ttlCaptor = org.mockito.ArgumentCaptor.forClass(Duration.class);
        verify(redisTemplate).expire(eqKey("chat:members:7"), ttlCaptor.capture());
        assertTrue(ttlCaptor.getValue().compareTo(Duration.ZERO) > 0);
    }

    @Test
    @DisplayName("저장 JSON 의 sentAt 은 숫자 배열이 아니라 ISO-8601 문자열이다")
    void appendStoresIsoTimestamp() {
        whenScriptReturns(1L);
        store.append(GROUP_ID, ChatMessageType.TEXT, 3L, "절약왕", "안녕");

        /* 저장은 스크립트가 한다. 검사 대상은 RPUSH 인자가 아니라 스크립트에 넘긴 JSON 이다 */
        String json = capturedArgs().get(0);
        assertTrue(json.matches(".*\"sentAt\":\"\\d{4}-\\d{2}-\\d{2}T[0-9:.]+\".*"),
                "sentAt 이 ISO 문자열이 아니다: " + json);
    }

    /*
     * 필드를 추가한 판을 배포했다가 되돌리는 경우다. 옛 코드가 새 필드를 모른다고 예외를 던지면
     * read 가 페이지 전체를 매핑하므로 그 방의 조회가 통째로 실패하고, List 는 중간 삭제가 없어
     * TTL 이 끝날 때까지 복구되지 않는다.
     */
    @Test
    @DisplayName("모르는 필드가 섞여 있어도 나머지를 읽는다 — 되돌린 배포가 방을 잠그지 않게")
    void readsMessagesWithUnknownFields() {
        String fromNewerVersion = "{\"messageId\":1,\"type\":\"SYSTEM\",\"content\":\"판결이 확정됐어요\","
                + "\"sentAt\":\"2026-08-19T09:00:00\",\"systemType\":\"VERDICT_CONFIRMED\","
                + "\"verdict\":{\"outcome\":\"GUILTY\",\"livesLost\":1}}";
        when(listOps.range("chat:messages:7", -50L, -1L)).thenReturn(List.of(fromNewerVersion));

        List<ChatMessage> messages = store.findRecent(GROUP_ID, 50);

        assertEquals(1, messages.size());
        assertEquals("판결이 확정됐어요", messages.get(0).getContent());
        assertEquals(ChatSystemType.VERDICT_CONFIRMED, messages.get(0).getSystemType());
    }

    @Test
    @DisplayName("예전에 저장된 숫자 배열 형식의 sentAt 도 그대로 읽힌다")
    void readsLegacyArrayTimestamp() {
        String legacy = "{\"messageId\":1,\"type\":\"TEXT\",\"senderId\":3,\"senderNickname\":\"절약왕\","
                + "\"content\":\"안녕\",\"sentAt\":[2026,8,16,14,43,11,138088000]}";
        when(listOps.range("chat:messages:7", -50L, -1L)).thenReturn(List.of(legacy));

        List<ChatMessage> messages = store.findRecent(GROUP_ID, 50);

        assertEquals(1, messages.size());
        assertEquals(2026, messages.get(0).getSentAt().getYear());
        assertEquals(43, messages.get(0).getSentAt().getMinute());
    }

    private static String eqKey(String key) {
        return org.mockito.ArgumentMatchers.eq(key);
    }
}
