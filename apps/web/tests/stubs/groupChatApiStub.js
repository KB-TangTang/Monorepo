/*
 * `@/api/groupChat` 대역(stub).
 *
 * store 가 fetchChatRoomInfo · fetchChatMessages · resetUnreadCount 를 어떤 인자로
 * 부르는지, 그리고 fetchChatRoomInfo 가 실패했을 때 store 가 어떻게 반응하는지를
 * 이 모듈 경계에서 끊어 확인한다. 실제 axios/http.js 까지는 내려가지 않는다.
 */
export const messagesCalls = [];
export const roomInfoCalls = [];
export const resetUnreadCalls = [];

let messagesResponse = { messages: [], hasMore: false };
let roomInfoResponse = {
    groupId: 0,
    challengeName: '',
    status: 'ACTIVE',
    memberCount: 0,
    unreadCount: 0,
};
let roomInfoError = null;

export function reset() {
    messagesCalls.length = 0;
    roomInfoCalls.length = 0;
    resetUnreadCalls.length = 0;
    messagesResponse = { messages: [], hasMore: false };
    roomInfoResponse = {
        groupId: 0,
        challengeName: '',
        status: 'ACTIVE',
        memberCount: 0,
        unreadCount: 0,
    };
    roomInfoError = null;
}

export function setMessagesResponse(response) {
    messagesResponse = response;
}

export function setRoomInfoResponse(response) {
    roomInfoResponse = response;
}

/** ApiError 모양(code 를 가진 Error)을 넘긴다. 실 http.js 인터셉터가 던지는 것과 같은 모양이다 */
export function setRoomInfoError(error) {
    roomInfoError = error;
}

export async function fetchChatRoomInfo(groupId) {
    roomInfoCalls.push(groupId);
    if (roomInfoError) throw roomInfoError;
    return roomInfoResponse;
}

export async function fetchChatMessages(groupId, cursor) {
    messagesCalls.push([groupId, cursor]);
    return messagesResponse;
}

export async function resetUnreadCount(groupId) {
    resetUnreadCalls.push(groupId);
}
