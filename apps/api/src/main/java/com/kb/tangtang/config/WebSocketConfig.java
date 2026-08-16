package com.kb.tangtang.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kb.tangtang.challenge.chat.service.ChatBroadcasterBinder;
import com.kb.tangtang.challenge.chat.service.StompChatBroadcaster;
import com.kb.tangtang.common.websocket.StompAuthChannelInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.List;

/**
 * 그룹 채팅 STOMP 배선 (이슈 #174).
 *
 * <p>⚠ <b>@Configuration 을 붙이지 않는다.</b> RootConfig 가 com.kb.tangtang 전체를 스캔해
 * 루트 컨텍스트에도 중복 등록된다. SwaggerConfig 와 같은 함정이다.
 * 이 클래스는 WebConfig#getServletConfigClasses() 로만 등록한다.
 *
 * <p>브로커는 인메모리 SimpleBroker 다. 단일 프로세스 모듈러 모놀리스라 외부 릴레이가 필요 없다.
 * SockJS 폴백은 넣지 않는다 — 대상 브라우저가 전부 네이티브 WebSocket 을 지원한다.
 *
 * <p>이 설정을 루트로 옮겨 문제를 피하려 하지 말 것: {@code stompWebSocketHandlerMapping} 이
 * 서블릿 컨텍스트에 있어야 DispatcherServlet 이 {@code /ws/chat} 을 라우팅하고,
 * {@code SimpAnnotationMethodMessageHandler} 는 자기 컨텍스트에서만 {@code @MessageMapping} 빈을
 * 찾는데 {@code ChatStompController} 는 {@code @Controller} 라 이 자식 컨텍스트에 있다.
 */
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private StompAuthChannelInterceptor authChannelInterceptor;

    /**
     * RootConfig 의 ObjectMapper 빈(JavaTimeModule 등록 + WRITE_DATES_AS_TIMESTAMPS off).
     * 서블릿 컨텍스트는 부모 빈을 주입받을 수 있다 — ServletConfig 와 같은 경로다.
     */
    @Autowired
    private ObjectMapper objectMapper;

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

    /**
     * STOMP 로 나가는 JSON 을 REST 응답과 같은 형식으로 맞춘다.
     *
     * <p>ServletConfig 는 REST 컨버터의 ObjectMapper 만 교체했다. {@code convertAndSend} 는 브로커의
     * 별도 컨버터를 쓰므로 그대로 두면 같은 {@code sentAt} 이 REST 에서는
     * {@code "2026-08-16T12:34:56"}, 소켓에서는 {@code [2026,8,16,...]} 숫자 배열로 나간다 —
     * 프론트가 두 경로를 같은 어댑터로 다룰 수 없게 된다.
     *
     * <p>{@code false} 를 반환해 기본 목록을 다시 붙이지 않는다. 여기서 넣는 3종이 스프링 기본 구성
     * ({@code AbstractMessageBrokerConfiguration#brokerMessageConverter})과 순서까지 같고,
     * Jackson 컨버터만 우리 ObjectMapper 를 쓴다.
     */
    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        messageConverters.add(new StringMessageConverter());
        messageConverters.add(new ByteArrayMessageConverter());
        messageConverters.add(jsonConverter(objectMapper));
        return false;
    }

    /** 테스트가 직접 부를 수 있도록 분리해 둔다 (WebSocketConfigTest) */
    static MappingJackson2MessageConverter jsonConverter(ObjectMapper objectMapper) {
        DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
        // 기본 구성과 동일하게 content-type 을 application/json 으로 붙여 준다
        resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        converter.setContentTypeResolver(resolver);
        return converter;
    }

    /**
     * 루트 컨텍스트의 브로드캐스터에 이 컨텍스트의 {@link SimpMessagingTemplate} 을 꽂는다.
     *
     * <p>자식은 부모 빈({@link StompChatBroadcaster})을 주입받을 수 있어 가능한 방향이다.
     * 반대 방향(루트가 SimpMessagingTemplate 주입)은 기동 실패다 — ChatBroadcaster 주석 참고.
     */
    @Bean
    public ChatBroadcasterBinder chatBroadcasterBinder(SimpMessagingTemplate brokerMessagingTemplate,
                                                       StompChatBroadcaster chatBroadcaster) {
        return new ChatBroadcasterBinder(chatBroadcaster, brokerMessagingTemplate);
    }
}
