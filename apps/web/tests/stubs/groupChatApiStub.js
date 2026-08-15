/*
 * `@/api/groupChat` 대역(stub).
 *
 * stores/groupChat.js 의 loadOlderMessages 가 fetchChatMessages 를 커서(before)로
 * 부르는지만 확인하면 되므로, 실제 axios/http.js 까지 내려가지 않고 이 모듈 경계에서 끊는다.
 */
export const messagesCalls = [];

let messagesResponse = { messages: [], hasMore: false };

export function reset() {
    messagesCalls.length = 0;
    messagesResponse = { messages: [], hasMore: false };
}

export function setMessagesResponse(response) {
    messagesResponse = response;
}

export async function fetchChatRoomInfo() {
    return { groupId: 0, name: '', status: 'ACTIVE', memberCount: 0, unreadCount: 0 };
}

export async function fetchChatMessages(groupId, cursor) {
    messagesCalls.push([groupId, cursor]);
    return messagesResponse;
}

export async function resetUnreadCount() {
    return undefined;
}
