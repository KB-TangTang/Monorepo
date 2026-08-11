import http from '@/api/http';

/**
 * 사용자 본인 정보 API.
 *
 * http 인스턴스가 공통 래퍼({success, data})를 이미 벗기므로 payload 가 바로 온다.
 * 실패는 ApiError(code, message, status) 로 reject 되니 호출부는 err.code 로 분기한다.
 */

/**
 * 실명 저장. 간편인증 화면이 인증 요청 직전에 호출한다.
 *
 * 구글 계정 이름(nickname)이 실명과 다른 사용자가 적지 않다 — 그대로 인증에 쓰면 실패한다.
 * 그래서 간편인증 화면의 「이름」을 편집 가능하게 두고, 사용자가 고친 값을 여기서
 * `tbl_user.name` 에 저장한다. 생년월일·통신사·휴대폰과 달리 **이름만 저장 대상**이다.
 * (DECISIONS.md 2026-08-11)
 *
 * @param {string} name 실명
 * @returns {Promise<{id, nickname, name, email, socialProvider}>} 갱신된 사용자 정보
 */
export function updateMyName(name) {
    return http.patch('/users/me/name', { name });
}
