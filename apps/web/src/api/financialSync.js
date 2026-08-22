import http from './http';

/**
 * 금융 자산 수동 동기화.
 *
 * institutionCodes 는 선택 항목이다. 계좌 연동 완료 직후의 최초 동기화(LinkDoneView)에서만 방금
 * 고른 기관 코드를 넘긴다 — 대출·페이머니는 계좌 연동이 다루지 못해 이 최초 동기화 전까지
 * `연결 계좌`에 잡히지 않고, 그래서 동기화 범위에도 못 들어간다(#334). 그 기관 코드를 직접 실어
 * 첫 동기화 범위를 넓혀야 실제로 연결된다. 그 외 호출(예: 향후 다른 화면)은 비워 보낸다.
 */
export function syncFinancialAssets(institutionCodes) {
    const body = institutionCodes?.length ? { institutionCodes } : null;
    return http.post('/financial-syncs', body, { timeout: 60000 });
}
