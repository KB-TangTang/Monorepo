package com.kb.tangtang.challenge.chat.dto;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;

import java.time.LocalDateTime;

/** 프론트로 나가는 메시지 모양. STOMP 와 REST 가 같은 DTO 를 쓴다 */
public class ChatMessageDto {

    private final long messageId;
    private final String type;
    private final Long senderId;
    private final String senderNickname;
    private final String content;
    private final LocalDateTime sentAt;

    private ChatMessageDto(ChatMessage m) {
        this.messageId = m.getMessageId();
        this.type = m.getType().name();
        this.senderId = m.getSenderId();
        this.senderNickname = m.getSenderNickname();
        this.content = m.getContent();
        this.sentAt = m.getSentAt();
    }

    public static ChatMessageDto from(ChatMessage m) {
        return new ChatMessageDto(m);
    }

    public long getMessageId() { return messageId; }
    public String getType() { return type; }
    public Long getSenderId() { return senderId; }
    public String getSenderNickname() { return senderNickname; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
}
