import http from '@/api/http';

/**
 * 동의 API.
 *
 * http 인스턴스가 공통 래퍼({success, data})를 이미 벗기므로 payload 가 바로 온다.
 * 실패는 ApiError(code, message, status) 로 reject 되니 호출부는 err.code 로 분기한다.
 */

/** scope: 'SIGNUP' | 'FINANCIAL' | 'CHALLENGE' */
export function fetchConsentCatalog(scope) {
    return http.get('/consents/catalog', { params: { scope } });
}

/** agreements: [{ type, agreed }] */
export function submitConsents(scope, agreements) {
    return http.post('/consents', { scope, agreements });
}

export function fetchMyConsents() {
    return http.get('/consents/me');
}

export function withdrawConsent(type) {
    return http.post(`/consents/${type}/withdraw`);
}
