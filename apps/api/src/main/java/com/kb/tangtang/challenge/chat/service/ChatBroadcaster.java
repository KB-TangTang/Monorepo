package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.dto.ChatMessageDto;

/**
 * 채팅 메시지를 구독자에게 밀어 주는 계약.
 *
 * <p><b>이 인터페이스가 존재하는 이유는 컨텍스트가 둘로 나뉘어 있기 때문이다.</b>
 * {@code SimpMessagingTemplate} 은 {@code @EnableWebSocketMessageBroker}(WebSocketConfig)가 만드는
 * 빈이고, WebSocketConfig 는 {@code WebConfig#getServletConfigClasses()} 로만 등록돼 **서블릿(자식)
 * 컨텍스트**에만 있다. 반면 {@code ChatMessageService} 는 {@code @Service} 라 RootConfig 의
 * 컴포넌트 스캔이 **루트(부모) 컨텍스트**에 만든다. 자식은 부모 빈을 주입받을 수 있지만 그 반대는
 * 불가능하므로, 루트 빈이 {@code SimpMessagingTemplate} 을 직접 주입받으면
 * {@code ContextLoaderListener} 단계에서 {@code NoSuchBeanDefinitionException} 이 나고
 * <b>war 배포 자체가 실패</b>한다(채팅뿐 아니라 서비스 전체가 뜨지 않는다).
 *
 * <p>그래서 루트에는 이 인터페이스만 두고, 서블릿 컨텍스트가 기동하면서
 * {@code SimpMessagingTemplate} 을 구현체({@link StompChatBroadcaster})에 바인딩한다
 * (WebSocketConfig#chatBroadcasterBinder).
 *
 * <p>회귀 방지 장치는 {@code RootContextWiringTest} 다 — 루트 컴포넌트가
 * {@code SimpMessagingTemplate} 을 다시 주입받으면 그 테스트가 실패한다.
 */
public interface ChatBroadcaster {

    /**
     * 그룹 채팅방 구독자에게 메시지를 보낸다.
     *
     * @throws com.kb.tangtang.common.exception.BusinessException 아직 브로커가 바인딩되지 않았을 때
     */
    void broadcast(long groupId, ChatMessageDto message);
}
