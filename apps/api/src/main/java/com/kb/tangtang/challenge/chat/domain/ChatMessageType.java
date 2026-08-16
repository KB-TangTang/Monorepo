package com.kb.tangtang.challenge.chat.domain;

/** 채팅 메시지 종류. 저장 위치는 같고 이 값으로만 구분한다(이슈 #174 결정). */
public enum ChatMessageType {
    /** 참여자가 보낸 텍스트 */
    TEXT,
    /** 재판 진행 상황을 알리는 봇 메시지 */
    SYSTEM
}
