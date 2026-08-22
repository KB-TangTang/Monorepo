/*
 * `@/api/groupChallenge` 대역(stub).
 *
 * groupChat 스토어가 이 모듈을 두 군데서 쓴다.
 *   - fetchGroupDetail   — 채팅방 진입 때 멤버 프로필 이미지를 받는다
 *   - fetchMyGroupChallenges — 로그인 직후 안 읽은 채팅 개수의 씨를 뿌린다
 * 실 모듈은 `@/api/http`(axios + import.meta.env)까지 내려가 Vite 밖에서는 터지므로
 * 이 경계에서 끊는다.
 *
 * 여기서 확인하려는 것은 둘이다.
 *   - 어떤 인자로 몇 번 부르는가
 *   - 이 호출이 실패해도 채팅 자체는 살아남는가 (이미지는 없으면 이니셜로 떨어지는 장식이다)
 */
export const groupDetailCalls = [];
/** refreshChatSummary 가 넘긴 상태 배열. 「씨 뿌리기를 실제로 부르는가」를 본다 */
export const myChallengeCalls = [];

let groupDetailResponse = { members: [] };
let groupDetailError = null;
let myChallengesResponse = [];
let myChallengesError = null;

export function reset() {
    groupDetailCalls.length = 0;
    myChallengeCalls.length = 0;
    groupDetailResponse = { members: [] };
    groupDetailError = null;
    myChallengesResponse = [];
    myChallengesError = null;
}

export function setMyChallengesResponse(response) {
    myChallengesResponse = response;
}

export function setMyChallengesError(error) {
    myChallengesError = error;
}

export async function fetchMyGroupChallenges(statuses) {
    myChallengeCalls.push(statuses);
    if (myChallengesError) throw myChallengesError;
    return myChallengesResponse;
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
