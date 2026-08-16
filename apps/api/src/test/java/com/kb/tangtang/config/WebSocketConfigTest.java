package com.kb.tangtang.config;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatMessageType;
import com.kb.tangtang.challenge.chat.dto.ChatMessageDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * STOMP 로 나가는 JSON 이 REST 응답과 같은 날짜 형식인지 본다.
 *
 * <p>ServletConfig 는 REST 컨버터의 ObjectMapper 만 교체했다. convertAndSend 는 브로커의 별도
 * MappingJackson2MessageConverter 를 쓰므로, 그쪽을 맞추지 않으면 같은 sentAt 이 REST 에서는
 * ISO-8601 문자열, 소켓에서는 [2026,8,16,...] 숫자 배열로 나간다.
 */
class WebSocketConfigTest {

    private ChatMessageDto message() {
        return ChatMessageDto.from(ChatMessage.of(1L, ChatMessageType.TEXT, 3L, "절약왕", "안녕",
                LocalDateTime.of(2026, 8, 16, 12, 34, 56)));
    }

    @Test
    @DisplayName("STOMP 브로커 컨버터도 RootConfig 의 ObjectMapper 를 써서 날짜를 ISO-8601 로 쓴다")
    void brokerConverterWritesIsoDates() {
        // REST 경로(ServletConfig#extendMessageConverters)가 쓰는 것과 같은 ObjectMapper 다
        MappingJackson2MessageConverter converter = WebSocketConfig.jsonConverter(new RootConfig().objectMapper());

        Message<?> converted = converter.toMessage(message(), null);

        assertNotNull(converted, "ChatMessageDto 를 JSON 으로 변환하지 못했다");
        String json = new String((byte[]) converted.getPayload(), StandardCharsets.UTF_8);
        assertTrue(json.contains("\"sentAt\":\"2026-08-16T12:34:56\""),
                "sentAt 이 ISO-8601 문자열이어야 한다. 실제: " + json);
        assertTrue(!json.contains("[2026,"), "숫자 배열(WRITE_DATES_AS_TIMESTAMPS)로 나가면 안 된다: " + json);
    }
}
