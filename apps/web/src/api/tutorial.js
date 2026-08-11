import http from '@/api/http';

/**
 * 튜토리얼 완료 플래그 API.
 *
 * 완료 여부는 브라우저가 아니라 서버(`tbl_user.tutorial_seen_at` ·
 * `tbl_user.group_tutorial_seen_at`)에 둔다 — localStorage 에 두면 기기를 바꾸거나
 * 캐시를 지운 사용자에게 튜토리얼이 다시 뜬다. (DECISIONS.md 2026-08-11 RV-108)
 *
 * 네 함수 모두 응답이 **갱신된 사용자 정보**
 * (`{ id, nickname, name, email, socialProvider, tutorialSeenAt, groupTutorialSeenAt }`)다.
 * 호출부는 이 값을 그대로 인증 스토어에 병합(mergeUser)하면 되고,
 * 저장 직후 `/users/me` 를 다시 부를 필요가 없다.
 *
 * http 인스턴스가 공통 래퍼({success, data})를 이미 벗기므로 payload 가 바로 온다.
 * 실패는 ApiError(code, message, status) 로 reject 된다.
 */

/** 개인(대법원, MC_01_05) 튜토리얼을 봤다고 기록한다. @returns {Promise<object>} 갱신된 사용자 정보 */
export function completeMainTutorial() {
    return http.patch('/main-challenge/tutorial/complete');
}

/** 개인 튜토리얼 플래그를 해제한다 — 마이페이지 「튜토리얼 다시 보기」용. @returns {Promise<object>} 갱신된 사용자 정보 */
export function resetMainTutorial() {
    return http.delete('/main-challenge/tutorial/complete');
}

/** 그룹(지방법원, GC_01_01) 튜토리얼을 봤다고 기록한다. @returns {Promise<object>} 갱신된 사용자 정보 */
export function completeGroupTutorial() {
    return http.patch('/group-challenge/tutorial/complete');
}

/** 그룹 튜토리얼 플래그를 해제한다. @returns {Promise<object>} 갱신된 사용자 정보 */
export function resetGroupTutorial() {
    return http.delete('/group-challenge/tutorial/complete');
}
