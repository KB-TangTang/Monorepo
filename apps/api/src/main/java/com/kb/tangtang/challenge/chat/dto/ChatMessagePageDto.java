package com.kb.tangtang.challenge.chat.dto;

import java.util.List;

/** hasMore 는 "위로 더 있는가" 다. 가장 오래된 메시지의 messageId 가 1 보다 크면 더 있다 */
public class ChatMessagePageDto {

    private final List<ChatMessageDto> messages;
    private final boolean hasMore;

    public ChatMessagePageDto(List<ChatMessageDto> messages, boolean hasMore) {
        this.messages = messages;
        this.hasMore = hasMore;
    }

    public List<ChatMessageDto> getMessages() { return messages; }
    public boolean isHasMore() { return hasMore; }
}
