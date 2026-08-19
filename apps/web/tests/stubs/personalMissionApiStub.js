/*
 * `@/api/personalMission` 대역(stub).
 *
 * store 의 맞춤 미션 개시 안내 전이만 이 모듈 경계에서 끊어 확인한다.
 * 실제 axios/http.js 까지는 내려가지 않는다.
 */
export const syncCalls = [];
export const acknowledgeCalls = [];

let syncResponse = { status: 'UNTRACKED', showUnlock: false };
let acknowledgeResponse = { status: 'SEEN', showUnlock: false };

export function reset() {
    syncCalls.length = 0;
    acknowledgeCalls.length = 0;
    syncResponse = { status: 'UNTRACKED', showUnlock: false };
    acknowledgeResponse = { status: 'SEEN', showUnlock: false };
}

export function setSyncResponse(response) {
    syncResponse = response;
}

export function syncPersonalMissionUnlock(...args) {
    syncCalls.push(args);
    return Promise.resolve(syncResponse);
}

export function acknowledgePersonalMissionUnlock(...args) {
    acknowledgeCalls.push(args);
    return Promise.resolve(acknowledgeResponse);
}

/* store 가 import 하는 나머지 함수들 - 이 테스트에서는 부르지 않는다. */
const unused = () => Promise.resolve(null);
export const fetchTodayMission = unused;
export const fetchMissionStreak = unused;
export const fetchPendingMissionVerdict = unused;
export const acknowledgeMissionVerdict = unused;
export const fetchMissionMonthlyScore = unused;
export const fetchMissionRankings = unused;
export const fetchMissionCertificate = unused;
export const fetchMissionCertificateTitles = unused;
export const fetchMissionRankingMonths = unused;
export const fetchMissionCategoryAnalysis = unused;
export const reassignTodayMission = unused;
