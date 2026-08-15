import http from '@/api/http';

export async function fetchLedgerMonths() {
    const result = await http.get('/transactions/months');
    return result.months;
}

/* LedgerView.vue 전용. summary/transactions 를 한 번에 받아온다(항상 같이 쓰이므로 요청도 하나). */
export async function fetchLedgerPeriod(period) {
    return http.get('/transactions', { params: { yearMonth: period } });
}

/* 검색 화면용. yearMonth 를 생략하면 데이터가 있는 모든 월의 거래를 합쳐서 반환한다. */
export async function fetchLedgerSearchTransactions() {
    const result = await http.get('/transactions');
    return result.transactions;
}

/* 거래 한 건의 카테고리를 사용자 지정으로 바꾼다. applyToMerchant=true면 같은 가맹점명의
 * 이후 거래에도 최우선 적용되지만, 이미 표시된 다른 거래는 소급 반영되지 않는다. */
export async function updateTransactionCategory(transactionId, { categoryId, applyToMerchant }) {
    return http.patch(`/transactions/${transactionId}/category`, { categoryId, applyToMerchant });
}
