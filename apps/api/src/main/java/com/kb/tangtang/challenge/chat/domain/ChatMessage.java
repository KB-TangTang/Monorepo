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
 * <p>SYSTEM 메시지는 senderId 와 senderNickname 이 null 이고, 대신
 * {@code systemType} · {@code deepLink} · {@code caseNo} 를 갖는다. TEXT 메시지는 그 반대다.
 * 세 값은 나중에 추가돼서 <b>이전에 저장된 메시지에는 없다</b> — 읽을 때 null 로 복원되므로
 * 화면은 값이 없을 때도 그려질 수 있어야 한다.
 */
public class ChatMessage {

    private final long messageId;
    private final ChatMessageType type;
    private final Long senderId;
    private final String senderNickname;
    private final String content;
    private final LocalDateTime sentAt;
    private final ChatSystemType systemType;
    private final String deepLink;
    private final String caseNo;

    @JsonCreator
    public ChatMessage(@JsonProperty("messageId") long messageId,
                       @JsonProperty("type") ChatMessageType type,
                       @JsonProperty("senderId") Long senderId,
                       @JsonProperty("senderNickname") String senderNickname,
                       @JsonProperty("content") String content,
                       @JsonProperty("sentAt") LocalDateTime sentAt,
                       @JsonProperty("systemType") ChatSystemType systemType,
                       @JsonProperty("deepLink") String deepLink,
                       @JsonProperty("caseNo") String caseNo) {
        this.messageId = messageId;
        this.type = type;
        this.senderId = senderId;
        this.senderNickname = senderNickname;
        this.content = content;
        this.sentAt = sentAt;
        this.systemType = systemType;
        this.deepLink = deepLink;
        this.caseNo = caseNo;
    }

    /** 참여자 메시지(TEXT) — 시스템 필드가 없다 */
    public static ChatMessage of(long messageId, ChatMessageType type, Long senderId,
                                  String senderNickname, String content, LocalDateTime sentAt) {
        return new ChatMessage(messageId, type, senderId, senderNickname, content, sentAt,
                null, null, null);
    }

    public long getMessageId() { return messageId; }
    public ChatMessageType getType() { return type; }
    public Long getSenderId() { return senderId; }
    public String getSenderNickname() { return senderNickname; }
    public String getContent() { return content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public ChatSystemType getSystemType() { return systemType; }
    public String getDeepLink() { return deepLink; }
    public String getCaseNo() { return caseNo; }
}
