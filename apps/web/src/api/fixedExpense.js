import http, { ApiError } from '@/api/http';

function unavailable(message) {
    throw new ApiError('API_NOT_AVAILABLE', message, 0);
}

export async function fetchFixedExpenseSavings() {
    return http.get('/fixedExpenses/savingReport');
}

export async function fetchFixedExpenseOverview() {
    return unavailable('고정지출 관리 조회 API는 후속 이슈에서 연동됩니다.');
}

export async function fetchFixedExpenseDetail() {
    return unavailable('고정지출 상세 조회 API는 후속 이슈에서 연동됩니다.');
}

export async function fetchFixedExpenseCandidate(candidateId) {
    return http.get(`/fixedExpenses/candidates/${candidateId}`);
}

export async function confirmFixedExpenseCandidate() {
    return unavailable('고정지출 후보 확정 API는 후속 이슈에서 연동됩니다.');
}

export async function dismissFixedExpenseCandidate() {
    return unavailable('고정지출 후보 제외 API는 후속 이슈에서 연동됩니다.');
}
