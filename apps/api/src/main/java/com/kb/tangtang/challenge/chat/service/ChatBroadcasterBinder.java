package com.kb.tangtang.challenge.chat.service;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * 서블릿 컨텍스트의 {@link SimpMessagingTemplate} 을 루트 컨텍스트의 {@link StompChatBroadcaster}
 * 에 꽂아 주는 연결 고리.
 *
 * <p>{@code @Component} 를 붙이지 않는다 — 붙이면 RootConfig 의 컴포넌트 스캔이 루트 컨텍스트에도
 * 등록하려 들고, 거기엔 {@code SimpMessagingTemplate} 이 없어 다시 같은 사고가 난다.
 * WebSocketConfig 의 {@code @Bean} 메서드로만 만든다.
 */
public class ChatBroadcasterBinder implements InitializingBean {

    private final StompChatBroadcaster broadcaster;
    private final SimpMessagingTemplate messagingTemplate;

    public ChatBroadcasterBinder(StompChatBroadcaster broadcaster, SimpMessagingTemplate messagingTemplate) {
        this.broadcaster = broadcaster;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        broadcaster.bind(messagingTemplate);
    }
}
