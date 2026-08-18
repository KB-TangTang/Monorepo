package com.kb.tangtang.common.websocket;

import com.kb.tangtang.challenge.chat.service.ChatRoomAccessService;
import com.kb.tangtang.common.auth.JwtProvider;
import com.kb.tangtang.common.exception.BusinessException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
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
 * 그래서 토큰을 STOMP 접속 프레임(CONNECT 또는 STOMP)의 네이티브 헤더로 받는다.
 * 검증 자체는 JwtProvider 를 그대로 재사용해 인증 로직이 두 벌이 되지 않게 한다.
 *
 * <p>SUBSCRIBE 뿐 아니라 SEND 도 검증한다. 구독하지 않고 발행만 하는 클라이언트를 막아야 한다.
 *
 * <p><b>deny by default.</b> SimpleBroker 의 구독 destination 은 리터럴이 아니라
 * AntPathMatcher 패턴이다({@code DefaultSubscriptionRegistry}) — {@code /sub/chat/**} 같은
 * 와일드카드로 검증을 우회할 수 있다. 그래서 destination 이 {@code /sub}·{@code /pub} 로
 * 시작하는데 정확한 채팅 경로 패턴({@code /sub/chat/{id}}, {@code /pub/chat/{id}})에 매치하지
 * 않으면 무조건 거부한다 — "판단할 수 없으니 통과" 가 아니라 "판단할 수 없으니 거부" 다.
 * {@code /sub}·{@code /pub} 이외의 prefix({@code /user/**} 등)는 이 인터셉터가 관여할
 * 대상이 아니므로 그대로 통과시킨다.
 *
 * <p>액세스 토큰은 15분이다. 만료되면 프론트가 재연결하며 새 토큰으로 다시 접속한다 —
 * 세션 중간에 토큰을 교체하는 경로는 만들지 않는다.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final String BEARER = "Bearer ";
    private static final Pattern SUBSCRIBE_CHAT_DESTINATION = Pattern.compile("^/sub/chat/(\\d+)$");
    private static final Pattern SEND_CHAT_DESTINATION = Pattern.compile("^/pub/chat/(\\d+)$");

    private final JwtProvider jwtProvider;
    private final ChatRoomAccessService access;

    public StompAuthChannelInterceptor(JwtProvider jwtProvider, ChatRoomAccessService access) {
        this.jwtProvider = jwtProvider;
        this.access = access;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        /*
         * StompHeaderAccessor.wrap(message) 는 헤더만 복사한 "새" accessor 를 만든다.
         * SimpMessageHeaderAccessor#setUser() 는 헤더를 쓴 뒤, StompSubProtocolHandler 가
         * CONNECT 프레임의 원본 accessor 에만 심어 둔 userChangeCallback 을 호출해야 세션의
         * stompAuthentications 맵에 반영된다 — wrap() 이 만든 사본에는 그 콜백이 없어
         * 인증이 조용히 사라진다. getAccessor 로 원본 인스턴스를 얻어야 한다.
         * headers 가 MutableMessageHeaders 가 아니면(= 이미 immutable 로 굳은 메시지) null 이
         * 돌아오는데, clientInboundChannel 체인 끝의 ImmutableMessageChannelInterceptor 는
         * 우리 인터셉터보다 뒤에 붙으므로 이 시점의 헤더는 아직 mutable 이다.
         */
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }

        // STOMP 1.2 는 CONNECT 동의어로 STOMP 커맨드도 접속 프레임으로 인정한다
        // (StompSubProtocolHandler 도 둘을 동일하게 취급한다) — 하나만 검사하면 인증이 우회된다.
        if (command == StompCommand.CONNECT || command == StompCommand.STOMP) {
            long userId = authenticate(accessor);
            // 원본 accessor 에 직접 쓴다. 새 Message 를 만들어 반환하지 않는다 — 그러면 다시 사본이다.
            accessor.setUser(new StompPrincipal(userId));
            return message;
        }

        if (command == StompCommand.SUBSCRIBE) {
            verifyDestination(accessor, SUBSCRIBE_CHAT_DESTINATION);
        } else if (command == StompCommand.SEND) {
            verifyDestination(accessor, SEND_CHAT_DESTINATION);
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

    private void verifyDestination(StompHeaderAccessor accessor, Pattern chatDestination) {
        String destination = accessor.getDestination();
        if (destination == null) {
            // 판단할 근거가 없다 — 통과가 아니라 거부다
            throw new BusinessException("UNAUTHORIZED", "인증이 필요해요.");
        }
        if (!destination.startsWith("/sub") && !destination.startsWith("/pub")) {
            // 브로커·발행 prefix 가 아니면 이 인터셉터가 판단할 것이 없다 (예: /user/queue/errors)
            return;
        }
        Matcher matcher = chatDestination.matcher(destination);
        if (!matcher.matches()) {
            // /sub·/pub 로 시작하지만 정확한 채팅 경로가 아니다 — 패턴 목적지 우회 시도를 포함해 전부 거부
            throw new BusinessException("UNAUTHORIZED", "인증이 필요해요.");
        }
        Principal principal = accessor.getUser();
        if (principal == null) {
            throw new BusinessException("UNAUTHORIZED", "인증이 필요해요.");
        }
        access.verifyCanEnter(Long.parseLong(matcher.group(1)), resolveUserId(principal));
    }

    /** 규약(Principal#getName() == userId 문자열)을 어긴 principal 은 인증 실패로 취급한다. */
    private long resolveUserId(Principal principal) {
        try {
            return Long.parseLong(principal.getName());
        } catch (NumberFormatException ex) {
            throw new BusinessException("UNAUTHORIZED", "인증이 필요해요.");
        }
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
