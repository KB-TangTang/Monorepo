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

    /*
     * chat:notify-cd:{groupId}:{userId} 가 있었다. 같은 방·같은 사람에게 30초에 한 번만 알림을
     * 보내던 키다. 이슈 #423 에서 이 SSE 이벤트가 그룹챌린지 홈의 배지·미리보기를 갱신하는
     * 데이터 채널이 되면서 없앴다 — 자세한 이유는 ChatMessageService#pushChatAlert 주석에 있다.
     * 남아 있던 키는 TTL 30초라 알아서 사라진다.
     */
}
