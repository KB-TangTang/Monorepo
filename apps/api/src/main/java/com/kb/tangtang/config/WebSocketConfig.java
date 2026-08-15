package com.kb.tangtang.config;

import com.kb.tangtang.common.websocket.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * 그룹 채팅 STOMP 배선 (이슈 #174).
 *
 * <p>⚠ <b>@Configuration 을 붙이지 않는다.</b> RootConfig 가 com.kb.tangtang 전체를 스캔해
 * 루트 컨텍스트에도 중복 등록된다. SwaggerConfig 와 같은 함정이다.
 * 이 클래스는 WebConfig#getServletConfigClasses() 로만 등록한다.
 *
 * <p>브로커는 인메모리 SimpleBroker 다. 단일 프로세스 모듈러 모놀리스라 외부 릴레이가 필요 없다.
 * SockJS 폴백은 넣지 않는다 — 대상 브라우저가 전부 네이티브 WebSocket 을 지원한다.
 */
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private StompAuthChannelInterceptor authChannelInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws/chat")
                // CORS 는 WebConfig 의 /api/** 설정과 별개다. 같은 오리진 목록을 유지한다
                .setAllowedOriginPatterns("https://*.vercel.app", "http://localhost:5173");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(authChannelInterceptor);
    }
}
