/*
 * `@/api/groupChallenge` 대역(stub).
 *
 * groupChat 스토어가 채팅방 진입 때 fetchGroupDetail 로 멤버 프로필 이미지를 받아 온다.
 * 실 모듈은 `@/api/http`(axios + import.meta.env)까지 내려가 Vite 밖에서는 터지므로
 * 이 경계에서 끊는다.
 *
 * 여기서 확인하려는 것은 둘이다.
 *   - 어떤 groupId 로 몇 번 부르는가
 *   - 이 호출이 실패해도 채팅 자체는 살아남는가 (이미지는 없으면 이니셜로 떨어지는 장식이다)
 */
export const groupDetailCalls = [];

let groupDetailResponse = { members: [] };
let groupDetailError = null;

export function reset() {
    groupDetailCalls.length = 0;
    groupDetailResponse = { members: [] };
    groupDetailError = null;
}

export function setGroupDetailResponse(response) {
    groupDetailResponse = response;
}

export function setGroupDetailError(error) {
    groupDetailError = error;
}

export async function fetchGroupDetail(groupId) {
    groupDetailCalls.push(groupId);
    if (groupDetailError) throw groupDetailError;
    return groupDetailResponse;
}
