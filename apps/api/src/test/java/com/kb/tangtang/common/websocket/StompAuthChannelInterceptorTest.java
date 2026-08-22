package com.kb.tangtang.common.websocket;

import com.kb.tangtang.challenge.chat.service.ChatRoomAccessService;
import com.kb.tangtang.common.auth.JwtProvider;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import java.security.Principal;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StompAuthChannelInterceptorTest {

    @Mock private JwtProvider jwtProvider;
    @Mock private ChatRoomAccessService access;
    @Mock private MessageChannel channel;

    @InjectMocks private StompAuthChannelInterceptor interceptor;

    private Message<byte[]> frame(StompCommand command, String authorization, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        accessor.setLeaveMutable(true);
        if (authorization != null) {
            accessor.setNativeHeader("Authorization", authorization);
        }
        if (destination != null) {
            accessor.setDestination(destination);
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    @Test
    @DisplayName("CONNECT 에서 토큰을 검증하고 userId 를 세션에 심는다")
    void authenticatesOnConnect() {
        when(jwtProvider.parseUserId("valid-token")).thenReturn(3L);

        Message<?> result = interceptor.preSend(frame(StompCommand.CONNECT, "Bearer valid-token", null), channel);

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertNotNull(accessor.getUser());
        assertEquals("3", accessor.getUser().getName());
    }

    /**
     * C1 회귀 테스트. 실제 STOMP 세션에서 CONNECT 프레임의 Principal 이 이후 프레임에
     * 전파되는 유일한 경로는 {@code StompSubProtocolHandler} 가 CONNECT 프레임의 "원본"
     * accessor 인스턴스에 심어 둔 {@code userChangeCallback} 이다({@code SimpMessageHeaderAccessor
     * #setUser} 가 헤더를 쓴 뒤 이 콜백을 호출하면 세션의 stompAuthentications 맵에 반영된다).
     * {@code StompHeaderAccessor.wrap(message)} 로 만든 사본에는 이 콜백이 없어 조용히
     * 인증이 사라진다 — 이 테스트는 그 콜백이 실제로 호출되는지, 즉 preSend 가 사본이 아니라
     * 원본 accessor 에 setUser 를 호출하는지를 검증한다.
     */
    @Test
    @DisplayName("CONNECT 인증 결과가 원본 accessor 의 userChangeCallback 으로 전파된다")
    void propagatesPrincipalViaOriginalAccessorCallback() {
        when(jwtProvider.parseUserId("valid-token")).thenReturn(3L);

        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        accessor.setNativeHeader("Authorization", "Bearer valid-token");
        AtomicReference<Principal> captured = new AtomicReference<>();
        accessor.setUserChangeCallback(captured::set);
        accessor.setLeaveMutable(true);
        Message<byte[]> connectFrame = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        interceptor.preSend(connectFrame, channel);

        assertNotNull(captured.get());
        assertEquals("3", captured.get().getName());
    }

    @Test
    @DisplayName("CONNECT 에 토큰이 없으면 거부한다")
    void rejectsConnectWithoutToken() {
        assertThrows(BusinessException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, null, null), channel));
    }

    @Test
    @DisplayName("Bearer 접두가 없으면 거부한다")
    void rejectsMalformedAuthorizationHeader() {
        assertThrows(BusinessException.class,
                () -> interceptor.preSend(frame(StompCommand.CONNECT, "valid-token", null), channel));
    }

    @Test
    @DisplayName("STOMP 커맨드(STOMP 1.2 의 CONNECT 동의어)에 토큰이 없으면 거부한다")
    void rejectsStompCommandWithoutToken() {
        assertThrows(BusinessException.class,
                () -> interceptor.preSend(frame(StompCommand.STOMP, null, null), channel));
    }

    /*
     * 아래 SUBSCRIBE/SEND 테스트들은 principal 을 프레임에 직접 심어 둔다. 실제 STOMP 세션에서는
     * CONNECT 때 심은 Principal 이 StompSubProtocolHandler(스프링 내부 클래스, 이 인터셉터
     * 밖의 세션 상태 관리)에 의해 이후 프레임에 전파되는데, 이 클래스는 각 preSend 호출을
     * "완전히 새로운 Message" 로 구성하는 단위 테스트 구조상 그 전파 경로 자체를 재현할 수 없다
     * (WebSocket 세션을 띄우는 통합 테스트가 필요하며, 이 프로젝트는 그런 테스트를 만들지 않는다 —
     * AGENTS.md). CONNECT→전파 경로는 위 propagatesPrincipalViaOriginalAccessorCallback 이
     * 별도로 검증한다. 아래 테스트들은 "이미 인증된 세션에서 SUBSCRIBE/SEND 프레임이 들어왔을 때"
     * 인터셉터의 목적지 검증 로직만을 독립적으로 검증한다.
     */

    @Test
    @DisplayName("SUBSCRIBE 는 destination 의 groupId 로 권한을 검증한다")
    void verifiesGroupOnSubscribe() {
        Message<?> connected = interceptor.preSend(connectedFrame(3L), channel);
        assertNotNull(connected);

        interceptor.preSend(subscribeFrame(3L, "/sub/chat/7"), channel);

        verify(access).verifyCanEnter(7L, 3L);
    }

    @Test
    @DisplayName("참여자가 아니면 SUBSCRIBE 를 거부한다")
    void rejectsSubscribeForNonMember() {
        doThrow(new BusinessException("CHAT_NOT_MEMBER", "참여자가 아니에요."))
                .when(access).verifyCanEnter(7L, 3L);

        assertThrows(BusinessException.class,
                () -> interceptor.preSend(subscribeFrame(3L, "/sub/chat/7"), channel));
    }

    @Test
    @DisplayName("SEND 도 같은 검증을 거친다 — 구독 없이 보내는 클라이언트를 막는다")
    void verifiesGroupOnSend() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId("session-1");
        accessor.setDestination("/pub/chat/7");
        accessor.setUser(() -> "3");
        accessor.setLeaveMutable(true);

        interceptor.preSend(MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), channel);

        verify(access).verifyCanEnter(7L, 3L);
    }

    @Test
    @DisplayName("C2: SUBSCRIBE 패턴 목적지(/sub/chat/**)는 인가를 우회하지 못하고 거부된다")
    void rejectsPatternSubscribeDestination() {
        assertThrows(BusinessException.class,
                () -> interceptor.preSend(subscribeFrame(3L, "/sub/chat/**"), channel));

        verify(access, never()).verifyCanEnter(anyLong(), anyLong());
    }

    @Test
    @DisplayName("I1: SEND 로 /sub 목적지에 직접 발행하는 것은 거부된다 — 브로커 직발행 우회 차단")
    void rejectsSendToSubPrefix() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setSessionId("session-1");
        accessor.setDestination("/sub/chat/7");
        accessor.setUser(() -> "3");
        accessor.setLeaveMutable(true);

        assertThrows(BusinessException.class, () -> interceptor.preSend(
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), channel));

        verify(access, never()).verifyCanEnter(anyLong(), anyLong());
    }

    @Test
    @DisplayName("destination 이 없는 SUBSCRIBE 는 거부한다 — 판단 불가는 통과가 아니라 거부다")
    void rejectsSubscribeWithoutDestination() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        accessor.setUser(() -> "3");
        accessor.setLeaveMutable(true);

        assertThrows(BusinessException.class, () -> interceptor.preSend(
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), channel));
    }

    @Test
    @DisplayName("/sub·/pub 이외의 prefix(/user/queue/errors 등)는 이 인터셉터가 관여하지 않고 통과시킨다")
    void passesThroughOtherPrefixDestination() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        accessor.setDestination("/user/queue/errors");
        accessor.setUser(() -> "3");
        accessor.setLeaveMutable(true);

        assertNotNull(interceptor.preSend(
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), channel));

        verify(access, never()).verifyCanEnter(anyLong(), anyLong());
    }

    private Message<byte[]> connectedFrame(long userId) {
        when(jwtProvider.parseUserId("t")).thenReturn(userId);
        return frame(StompCommand.CONNECT, "Bearer t", null);
    }

    private Message<byte[]> subscribeFrame(long userId, String destination) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        accessor.setDestination(destination);
        accessor.setUser(() -> String.valueOf(userId));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
