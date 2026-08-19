import http from './http';

/** 금융 자산 수동 동기화. */
export function syncFinancialAssets() {
    return http.post('/financial-syncs', null, { timeout: 60000 });
}
