package com.kb.tangtang.challenge.chat.service;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.challenge.chat.dto.ChatMessageDto;
import com.kb.tangtang.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StompChatBroadcasterTest {

    private static final long GROUP_ID = 7L;

    @Mock private SimpMessagingTemplate messagingTemplate;

    private final StompChatBroadcaster broadcaster = new StompChatBroadcaster();

    private ChatMessageDto message() {
        return ChatMessageDto.from(new ChatMessage(1L, ChatMessageType.TEXT, 3L, "절약왕",
                "안녕", LocalDateTime.of(2026, 8, 16, 12, 34, 56)));
    }

    @Test
    @DisplayName("바인딩 후에는 /sub/chat/{groupId} 로 보낸다")
    void sendsToChatDestination() {
        broadcaster.bind(messagingTemplate);
        ChatMessageDto dto = message();

        broadcaster.broadcast(GROUP_ID, dto);

        verify(messagingTemplate).convertAndSend("/sub/chat/7", dto);
    }

    /*
     * 서블릿 컨텍스트가 뜨기 전(또는 배선이 사라졌을 때) 조용한 no-op 이 되면 "저장은 됐는데
     * 아무에게도 안 가는" 상태가 원인 없이 유지된다. 즉시 드러나야 한다.
     */
    @Test
    @DisplayName("바인딩 전에 브로드캐스트하면 조용히 넘기지 않고 500 으로 실패한다")
    void failsLoudlyWhenNotBound() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> broadcaster.broadcast(GROUP_ID, message()));

        assertEquals("CHAT_BROADCAST_UNAVAILABLE", ex.getCode());
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getHttpStatus());
        verify(messagingTemplate, never()).convertAndSend(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Object.class));
    }

    @Test
    @DisplayName("바인더가 서블릿 컨텍스트의 템플릿을 꽂아 준다")
    void binderWiresTemplate() {
        new ChatBroadcasterBinder(broadcaster, messagingTemplate).afterPropertiesSet();

        broadcaster.broadcast(GROUP_ID, message());

        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/sub/chat/7"),
                org.mockito.ArgumentMatchers.any(Object.class));
    }
}
