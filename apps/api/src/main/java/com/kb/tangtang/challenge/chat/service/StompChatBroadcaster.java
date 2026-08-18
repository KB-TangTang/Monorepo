package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.dto.ChatMessageDto;
import com.kb.tangtang.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 루트 컨텍스트에 사는 {@link ChatBroadcaster} 구현. 실제 전송기는 서블릿 컨텍스트가 꽂아 준다.
 *
 * <p><b>여기서 {@code SimpMessagingTemplate} 을 생성자로 주입받으면 안 된다.</b> 그 빈은 서블릿
 * (자식) 컨텍스트에만 있어 루트 컨텍스트 refresh 가 통째로 실패한다({@link ChatBroadcaster} 주석 참고).
 * 위임자는 {@link #bind(SimpMessagingTemplate)} 으로 늦게 채워진다.
 *
 * <p>바인딩 전에 브로드캐스트가 불리면 <b>조용히 넘기지 않고</b> 경고 로그와 함께 500 으로 실패한다.
 * no-op 으로 삼키면 "메시지는 저장됐는데 아무에게도 안 가는" 상태가 조용히 유지된다.
 */
@Component
public class StompChatBroadcaster implements ChatBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(StompChatBroadcaster.class);

    /** 서블릿 컨텍스트 기동 스레드가 쓰고 요청 스레드가 읽는다 — 가시성을 위해 volatile 이다 */
    private volatile SimpMessagingTemplate messagingTemplate;

    /**
     * 구독 목적지. {@code StompAuthChannelInterceptor} 의 SUBSCRIBE 허용 패턴
     * ({@code ^/sub/chat/(\d+)$})과 짝이다. <b>목적지 문자열은 이 메서드 하나만 만든다.</b>
     */
    public static String destination(long groupId) {
        return "/sub/chat/" + groupId;
    }

    /** 서블릿 컨텍스트가 기동하면서 한 번 호출한다 (WebSocketConfig#chatBroadcasterBinder) */
    public void bind(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        log.info("채팅 브로드캐스터에 STOMP 브로커를 바인딩했다");
    }

    @Override
    public void broadcast(long groupId, ChatMessageDto message) {
        SimpMessagingTemplate template = this.messagingTemplate;
        if (template == null) {
            // 서블릿 컨텍스트가 아직/영영 안 뜬 상태다. 삼키면 원인을 알 수 없는 "안 오는 메시지" 가 된다.
            log.error("STOMP 브로커가 바인딩되지 않아 메시지를 전달할 수 없다. groupId={} messageId={}",
                    groupId, message.getMessageId());
            throw new BusinessException("CHAT_BROADCAST_UNAVAILABLE", "메시지를 전달하지 못했어요.",
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
        template.convertAndSend(destination(groupId), message);
    }
}
