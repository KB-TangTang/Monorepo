/**
 * 그룹 채팅 API 모듈
 *
 * 현재는 목업 데이터를 반환한다.
 * 추후 WebSocket(STOMP) + Redis 연동 시 실제 HTTP/WS 호출로 교체.
 *
 * Redis 키 패턴:
 *   chat:messages:{groupId}          → ZSET (score = timestamp)
 *   chat:unread:{groupId}:{userId}   → STRING (count)
 */
import {
    MOCK_CHAT_ROOMS,
    MOCK_CHAT_MESSAGES,
    MOCK_OLDER_MESSAGES,
} from '@/fixtures/groupChat';

const DELAY = 200; // 네트워크 시뮬레이션 (ms)
const PAGE_SIZE = 20;

function wait(ms = DELAY) {
    return new Promise((r) => setTimeout(r, ms));
}

/**
 * 채팅방 메타 정보 조회
 * GET /api/groups/{groupId}/chat/room
 */
export async function fetchChatRoomInfo(groupId) {
    await wait();
    const room = MOCK_CHAT_ROOMS[groupId];
    if (!room) throw new Error(`채팅방 없음: groupId=${groupId}`);
    return { ...room };
}

/**
 * 메시지 목록 조회 (페이징)
 * GET /api/groups/{groupId}/chat/messages?offset=&limit=
 */
export async function fetchChatMessages(groupId, offset = 0, limit = PAGE_SIZE) {
    await wait();
    const all = MOCK_CHAT_MESSAGES[groupId] ?? [];

    if (offset === 0) {
        // 최초 로드: 최신 limit개
        const sliced = all.slice(-limit);
        return { messages: sliced, hasMore: all.length > limit };
    }

    // 스크롤-업: 더 오래된 메시지
    const older = MOCK_OLDER_MESSAGES[groupId] ?? [];
    return { messages: older, hasMore: false };
}

/**
 * 메시지 전송
 * POST /api/groups/{groupId}/chat/messages
 * body: { contentType: 'TEXT'|'STICKER', content: string }
 */
export async function sendChatMessage(groupId, { contentType, content }) {
    await wait(100);
    return {
        messageId: `m-${Date.now()}`,
        createdAt: new Date().toISOString(),
    };
}

/**
 * 안 읽은 메시지 카운트 초기화
 * PUT /api/groups/{groupId}/chat/unread/reset
 */
export async function resetUnreadCount(groupId) {
    await wait(50);
    return { success: true };
}
