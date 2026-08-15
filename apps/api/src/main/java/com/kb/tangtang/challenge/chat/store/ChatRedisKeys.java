package com.kb.tangtang.challenge.chat.store;

/**
 * 채팅 Redis 키. 문자열을 여러 곳에 흩뿌리면 오타 하나로 조용히 다른 방을 읽는다.
 *
 * <p>키 설계는 DECISIONS.md 2026-08-15 참고.
 */
public final class ChatRedisKeys {

    private ChatRedisKeys() {
    }

    /** 메시지 원본 (List). 일반·시스템 공용 */
    public static String messages(long groupId) {
        return "chat:messages:" + groupId;
    }

    /** 방별 메시지 일련번호. 1부터 시작하며 List 인덱스 + 1 과 항상 같다 */
    public static String seq(long groupId) {
        return "chat:seq:" + groupId;
    }

    /** 사용자별 안 읽은 수 */
    public static String unread(long groupId, long userId) {
        return "chat:unread:" + groupId + ":" + userId;
    }

    /** 참여자 캐시 (Set). 메시지마다 tbl_group_member 를 조회하지 않기 위한 것 */
    public static String members(long groupId) {
        return "chat:members:" + groupId;
    }

    /** 알림 도배 방지 쿨다운 */
    public static String notifyCooldown(long groupId, long userId) {
        return "chat:notify-cd:" + groupId + ":" + userId;
    }
}
