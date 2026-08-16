package com.kb.tangtang.challenge.chat.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Redis List 에 JSON 한 줄로 들어가는 메시지.
 *
 * <p>senderNickname 을 값에 박아 넣는다. 조회할 때마다 tbl_user 를 조인하면
 * Redis 단독 저장의 이점이 사라진다.
 *
 * <p>SYSTEM 메시지는 senderId 와 senderNickname 이 null 이다.
 */
public class ChatMessage {

    private final long messageId;
    private final ChatMessageType type;
    private final Long senderId;
    private final String senderNickname;
    private final String content;
    private final LocalDateTime sentAt;

    @JsonCreator
    public ChatMessage(@JsonProperty("messageId") long messageId,
                       @JsonProperty("type") ChatMessageType type,
                       @JsonProperty("senderId") Long senderId,
                       @JsonProperty("senderNickname") String senderNickname,
                       @JsonProperty("content") String content,
                       @JsonProperty("sentAt") LocalDateTime sentAt) {
        this.messageId = messageId;
        this.type = type;
        this.senderId = senderId;
        this.senderNickname = senderNickname;
        this.content = content;
        this.sentAt = sentAt;
    }

    public static ChatMessage of(long messageId, ChatMessageType type, Long senderId,
                                  String senderNickname, String content, LocalDateTime sentAt) {
        return new ChatMessage(messageId, type, senderId, senderNickname, content, sentAt);
    }

    public long getMessageId() { return messageId; }
    public ChatMessageType getType() { return type; }
    public Long getSenderId() { return senderId; }
    public String getSenderNickname() { return senderNickname; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
}
