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
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
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
    @DisplayName("destination 이 채팅 경로가 아니면 통과시킨다")
    void passesThroughNonChatDestination() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setSessionId("session-1");
        accessor.setDestination("/sub/other/7");
        accessor.setUser(() -> "3");
        accessor.setLeaveMutable(true);

        assertNotNull(interceptor.preSend(
                MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders()), channel));
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
