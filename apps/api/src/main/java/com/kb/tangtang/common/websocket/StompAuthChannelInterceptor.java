package com.kb.tangtang.common.websocket;

import com.kb.tangtang.challenge.chat.service.ChatRoomAccessService;
import com.kb.tangtang.common.auth.JwtProvider;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP 프레임 인증·권한.
 *
 * <p><b>기존 JwtAuthInterceptor 를 쓸 수 없다.</b> 그 인터셉터는 /api/** 전용이고,
 * 브라우저 WebSocket API 로는 핸드셰이크에 Authorization 헤더를 넣을 수 없다.
 * 그래서 토큰을 STOMP CONNECT 프레임의 네이티브 헤더로 받는다.
 * 검증 자체는 JwtProvider 를 그대로 재사용해 인증 로직이 두 벌이 되지 않게 한다.
 *
 * <p>SUBSCRIBE 뿐 아니라 SEND 도 검증한다. 구독하지 않고 발행만 하는 클라이언트를 막아야 한다.
 *
 * <p>액세스 토큰은 15분이다. 만료되면 프론트가 재연결하며 새 토큰으로 다시 CONNECT 한다 —
 * 세션 중간에 토큰을 교체하는 경로는 만들지 않는다.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";
    private static final Pattern CHAT_DESTINATION = Pattern.compile("^/(sub|pub)/chat/(\\d+)$");

    private final JwtProvider jwtProvider;
    private final ChatRoomAccessService access;

    public StompAuthChannelInterceptor(JwtProvider jwtProvider, ChatRoomAccessService access) {
        this.jwtProvider = jwtProvider;
        this.access = access;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        if (command == StompCommand.CONNECT) {
            long userId = authenticate(accessor);
            accessor.setUser(new StompPrincipal(userId));
            accessor.setLeaveMutable(true);
            return MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        }

        if (command == StompCommand.SUBSCRIBE || command == StompCommand.SEND) {
            verifyDestination(accessor);
        }
        return message;
    }

    private long authenticate(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader("Authorization");
        if (headers == null || headers.isEmpty()) {
            throw new BusinessException("UNAUTHORIZED", "인증이 필요해요.");
        }
        String raw = headers.get(0);
        if (raw == null || !raw.startsWith(BEARER)) {
            throw new BusinessException("UNAUTHORIZED", "인증이 필요해요.");
        }
        return jwtProvider.parseUserId(raw.substring(BEARER.length()));
    }

    private void verifyDestination(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        Matcher matcher = CHAT_DESTINATION.matcher(destination);
        if (!matcher.matches()) {
            // 채팅 경로가 아니면 이 인터셉터가 판단할 것이 없다
            return;
        }
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new BusinessException("UNAUTHORIZED", "인증이 필요해요.");
        }
        access.verifyCanEnter(Long.parseLong(matcher.group(2)), Long.parseLong(principal.getName()));
    }

    /** Principal#getName() 이 userId 문자열이다. 이후 컨트롤러·리스너가 이 규약에 기댄다 */
    public static final class StompPrincipal implements Principal {

        private final long userId;

        public StompPrincipal(long userId) {
            this.userId = userId;
        }

        @Override
        public String getName() {
            return String.valueOf(userId);
        }

        public long getUserId() {
            return userId;
        }
    }
}
