import http, { ApiError } from '@/api/http';

function unavailable(message) {
    throw new ApiError('API_NOT_AVAILABLE', message, 0);
}

function toParams({ yearMonth, categoryId } = {}) {
    const params = {};
    if (yearMonth) {
        params.yearMonth = yearMonth;
    }
    if (categoryId) {
        params.categoryId = categoryId;
    }
    return params;
}

function toDetailModel(detail) {
    return {
        ...detail.item,
        paymentHistory: detail.paymentHistory ?? [],
        evidenceMonths: detail.evidenceMonths ?? [],
        sixMonthTotal: detail.sixMonthTotal ?? 0,
        changeNotice: detail.changeNotice ?? null,
    };
}

export async function fetchFixedExpenseSavings(yearMonth) {
    return http.get('/fixedExpenses/savingReport', { params: toParams({ yearMonth }) });
}

export async function fetchFixedExpenseOverview(options) {
    return http.get('/fixedExpenses/candidates', { params: toParams(options) });
}

export async function fetchFixedExpenseDetail(expenseId) {
    const detail = await http.get(`/fixedExpenses/candidates/${expenseId}`);
    return toDetailModel(detail);
}

export async function fetchFixedExpenseCandidate(candidateId) {
    return fetchFixedExpenseDetail(candidateId);
}

export async function confirmFixedExpenseCandidate() {
    return unavailable('고정지출 후보 확정 API는 후속 이슈에서 연동됩니다.');
}

export async function dismissFixedExpenseCandidate() {
    return unavailable('고정지출 후보 제외 API는 후속 이슈에서 연동됩니다.');
}
