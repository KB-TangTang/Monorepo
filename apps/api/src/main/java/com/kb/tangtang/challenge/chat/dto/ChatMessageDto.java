package com.kb.tangtang.challenge.chat.dto;

import com.kb.tangtang.challenge.chat.domain.ChatMessage;
import com.kb.tangtang.challenge.chat.domain.ChatVerdictInfo;

import java.time.LocalDateTime;

/** 프론트로 나가는 메시지 모양. STOMP 와 REST 가 같은 DTO 를 쓴다 */
public class ChatMessageDto {

    private final long messageId;
    private final String type;
    private final Long senderId;
    private final String senderNickname;
    private final String content;
    private final LocalDateTime sentAt;
    /* 아래 셋은 SYSTEM 메시지에만 있다. TEXT 메시지와 이 필드가 생기기 전에 저장된 메시지는 null 이다 */
    private final String systemType;
    private final String deepLink;
    private final String caseNo;
    /* 판결 확정 메시지에만 있다. 화면이 도장과 「투표 4:2 · 목숨 1 차감」을 그리는 값이다(이슈 #304) */
    private final ChatVerdictInfo verdict;

    private ChatMessageDto(ChatMessage m) {
        this.messageId = m.getMessageId();
        this.type = m.getType().name();
        this.senderId = m.getSenderId();
        this.senderNickname = m.getSenderNickname();
        this.content = m.getContent();
        this.sentAt = m.getSentAt();
        this.systemType = m.getSystemType() == null ? null : m.getSystemType().name();
        this.deepLink = m.getDeepLink();
        this.caseNo = m.getCaseNo();
        this.verdict = m.getVerdict();
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
    public String getSystemType() { return systemType; }
    public String getDeepLink() { return deepLink; }
    public String getCaseNo() { return caseNo; }
    public ChatVerdictInfo getVerdict() { return verdict; }
}
