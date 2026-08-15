package com.kb.tangtang.challenge.chat.service;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * STOMP 구독·연결 종료를 ChatSessionRegistry 에 반영한다.
 *
 * <p>이 이벤트는 서블릿(자식) 컨텍스트에 등록된 WebSocketConfig 의 STOMP 인프라가 발행한다.
 * 이 리스너는 {@code @Component} 로 RootConfig(부모 컨텍스트)에 등록되지만, Spring 의
 * {@code AbstractApplicationContext#publishEvent} 는 자식 컨텍스트에서 발행한 이벤트를
 * 부모 컨텍스트로도 전파하므로(부모→자식 방향은 전파되지 않는다) 여기서도 정상 수신된다.
 * 컨테이너를 띄워야 확인 가능한 동작이라 단위 테스트로는 검증하지 못했다 — 런타임 확인 필요.
 */
@Component
public class ChatSessionEventListener {

    private static final Pattern SUB_DESTINATION = Pattern.compile("^/sub/chat/(\\d+)$");

    private final ChatSessionRegistry registry;

    public ChatSessionEventListener(ChatSessionRegistry registry) {
        this.registry = registry;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        Principal principal = accessor.getUser();
        if (destination == null || principal == null) {
            return;
        }
        Matcher matcher = SUB_DESTINATION.matcher(destination);
        if (!matcher.matches()) {
            return;
        }
        registry.enter(accessor.getSessionId(),
                Long.parseLong(matcher.group(1)),
                Long.parseLong(principal.getName()));
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        registry.leave(event.getSessionId());
    }
}
