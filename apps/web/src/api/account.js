import http from './http';

/**
 * 계좌 연동 API (이슈 #12).
 *
 * `http.js` 인터셉터가 공통 래퍼(`{success, data}`)를 이미 벗겨서 반환하므로
 * 호출부는 `data` 안의 실제 payload 를 바로 받는다. `res.data.data` 같은 코드를 쓰지 말 것.
 * 실패는 `ApiError(code, message, status)` 로 정규화돼 reject 된다 — 호출부는 `err.code` 로 분기한다.
 *
 * 엔드포인트는 백엔드 `AccountLinkController` 와 1:1 이다.
 *
 * ⚠ **이 파일 어디에도 mock/codef 분기가 없다.** 어느 금융 데이터 공급자가 붙었는지는
 *   백엔드 `financial.client` 프로퍼티가 정하고, 프론트는 `fetchAuthMethods()` 응답만 보고
 *   인증 화면을 고른다. 여기에 빌드 플래그 분기가 생기면
 *   "설정 교체만으로 실 CODEF 전환" 이라는 전제가 깨진다. (DECISIONS.md 2026-08-05)
 */

/** 연동 지원 금융기관 목록 (AC_01_01). */
export function fetchInstitutions() {
    return http.get('/accounts/institutions');
}

/**
 * 지원 인증 수단.
 * 목 모드는 `SIMPLE_AUTH`, CODEF 모드는 `INSTITUTION_LOGIN` 이 내려온다.
 * @returns {Promise<{methods: Array<{type: string, providers: Array<{code: string, name: string}>}>}>}
 */
export function fetchAuthMethods() {
    return http.get('/accounts/link/auth-methods');
}

/**
 * 간편인증 요청.
 *
 * ⚠ 개인정보(생년월일·통신사·휴대폰)를 보내지 않는다. 화면에서 형식만 검증하고 폐기한다.
 * @param {{provider: string, organizations: string[]}} payload
 * @returns {Promise<{connectionId: string, status: string, expiresInSeconds: number}>}
 */
export function requestSimpleAuth(payload) {
    return http.post('/accounts/connections/simple-auth', payload);
}

/**
 * 기관 로그인 인증 → connectedId 발급 (AC_01_02).
 *
 * ⚠ credentials 는 인자로만 흐르고 어디에도 저장하지 않는다. 스토어·로컬스토리지 금지.
 * @param {Array<{organization: string, loginType: string, id: string, password: string}>} credentials
 */
export function createConnection(credentials) {
    return http.post('/accounts/connections', { credentials });
}

/** 간편인증 승인 여부 폴링. `PENDING | APPROVED | FAILED | EXPIRED` */
export function fetchAuthStatus(connectionId) {
    return http.get(`/accounts/connections/${connectionId}/auth-status`);
}

/** 추가인증(2-way) 제출 (AC_01_02). 실 CODEF 에서 기관에 따라 요구한다. */
export function submitExtraAuth(connectionId, payload) {
    return http.post(`/accounts/connections/${connectionId}/extra-auth`, payload);
}

/**
 * 기관별 조회 진행 상황 폴링.
 * 진행률은 서버가 계산해 내려준다 — 프론트 연출이 아니다.
 */
export function fetchLinkProgress(connectionId) {
    return http.get(`/accounts/connections/${connectionId}/progress`);
}

/**
 * 인증한 기관에서 조회된 계좌 목록 (AC_01_03).
 * 기관별로 묶여서 오고, 계좌번호는 서버가 마스킹한 값이다.
 */
export function fetchLinkableAccounts(connectionId) {
    return http.get(`/accounts/connections/${connectionId}/accounts`);
}

/** 선택한 계좌를 실제로 연결한다 (AC_01_03 → 완료). */
export function linkAccounts(connectionId, accountIds, excludedAccountIds = []) {
    /* excludedAccountIds: 체크를 푼 자동 연동 행(대출·페이머니·카드)의 음수 id (#467). 비면 전부 연동된다. */
    return http.post('/accounts', { connectionId, accountIds, excludedAccountIds });
}

/** 연결 계좌 관리 목록 (AC_01_04). */
export function fetchConnectedAccounts() {
    return http.get('/accounts');
}

/**
 * 계좌 즉시 조회 (AC_02_03).
 * 쿨다운은 서버가 판단해 `cooldownSeconds` 로 내려준다. 프론트가 직접 계산하지 않는다.
 */
export function refreshAccounts() {
    return http.post('/accounts/refresh');
}

/** 계좌 연결 해제 (AC_01_04). */
export function disconnectAccount(accountId) {
    return http.delete(`/accounts/${accountId}`);
}

/** 계좌 재동기화 (AC_02_04 / AC_02_06). */
export function resyncAccount(accountId) {
    return http.post(`/accounts/${accountId}/resync`);
}
