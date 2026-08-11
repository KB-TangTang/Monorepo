import http from './http';

/**
 * 구글 로그인 시작 주소.
 * SPA 라우팅이 아니라 window.location.href 로 전체 이동해야 한다 —
 * 백엔드가 302 로 구글에 넘기는 구조이기 때문이다.
 * http 인스턴스의 baseURL('/api')을 타지 않으므로 전체 경로를 적는다.
 */
export const GOOGLE_LOGIN_URL = '/api/auth/google';

/**
 * 리프레시 쿠키로 액세스 토큰을 재발급한다.
 * `{ accessToken, user, needsConsent, needsFinancialConsent, needsAccountLink }`
 * 뒤의 세 플래그가 온보딩 게이트다 (stores/auth.js · utils/user.js 참고).
 */
export function refreshSession() {
    return http.post('/auth/refresh');
}

export function logout() {
    return http.post('/auth/logout');
}

/** 로그인한 본인 정보. { id, nickname, email } */
export function fetchMe() {
    return http.get('/users/me');
}
