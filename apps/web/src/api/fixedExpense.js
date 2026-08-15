import http from '@/api/http';

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

export async function confirmFixedExpenseCandidate(candidateId) {
    return http.patch(`/fixedExpenses/candidates/${candidateId}`, { action: 'CONFIRM' });
}

export async function dismissFixedExpenseCandidate(candidateId) {
    return http.patch(`/fixedExpenses/candidates/${candidateId}`, { action: 'EXCLUDE' });
}

/** 로컬에서만 열리는 DEV 배치다. Mock/API 출처와 무관하게 실제 로컬 서버에서 실행한다. */
export async function runFixedExpensePaymentReminderBatch() {
    return http.post('/dev/batches/fixed-expense-payment-reminders');
}

/** 현재 사용자의 PAYMENT_DUE 알림과 발송 이력만 비워 같은 결제 주기를 다시 확인한다. */
export async function resetFixedExpensePaymentReminders() {
    return http.delete('/dev/batches/fixed-expense-payment-reminders');
}
