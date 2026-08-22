/**
 * 그룹 채팅 API (이슈 #174).
 *
 * 메시지 원본은 Redis List 에만 있다. 챌린지가 CLOSED 가 되면 즉시 삭제되므로
 * 종료된 챌린지의 대화는 조회할 수 없다(서버가 CHAT_ROOM_CLOSED 로 막는다).
 *
 * 실시간 송수신은 api/chatSocket.js 가 STOMP 로 맡는다. 이 파일은 조회 전용이다.
 */
import http from '@/api/http';

const DEFAULT_LIMIT = 50;
const MIN_LIMIT = 1;
const MAX_LIMIT = 100;

/**
 * 서버가 limit 1~100 밖의 값을 INVALID_REQUEST(400) 로 거절한다.
 * 화면 쪽 실수(0, 음수, 100 초과)가 그대로 요청으로 나가지 않도록 여기서 보정한다.
 */
function normalizeLimit(limit) {
    if (!Number.isFinite(limit)) return DEFAULT_LIMIT;
    return Math.min(MAX_LIMIT, Math.max(MIN_LIMIT, Math.trunc(limit)));
}

/** 채팅방 메타 정보 (이름 · 상태 · 인원 · 내 안 읽은 수) */
export async function fetchChatRoomInfo(groupId) {
    return http.get(`/groups/${groupId}/chat/room`);
}

/**
 * 메시지 목록.
 *
 * before 는 위로 스크롤할 때, after 는 재연결 후 놓친 구간을 메울 때 쓴다.
 * 둘을 함께 주면 서버가 INVALID_REQUEST 로 막으므로 요청을 보내기 전에 막는다.
 */
export async function fetchChatMessages(groupId, { before, after, limit } = {}) {
    const hasBefore = before !== undefined && before !== null;
    const hasAfter = after !== undefined && after !== null;
    if (hasBefore && hasAfter) {
        throw new Error('before 와 after 는 함께 지정할 수 없습니다.');
    }

    const params = { limit: normalizeLimit(limit ?? DEFAULT_LIMIT) };
    if (hasBefore) params.before = before;
    if (hasAfter) params.after = after;

    return http.get(`/groups/${groupId}/chat/messages`, { params });
}

/** 안 읽은 수 초기화. 입장·복귀 시 부른다 */
export async function resetUnreadCount(groupId) {
    return http.post(`/groups/${groupId}/chat/read`);
}
