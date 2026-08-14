import test from 'node:test';
import assert from 'node:assert/strict';
import { registerHooks } from 'node:module';

const STUB_URL = new URL('./stubs/fixedExpenseApiStub.js', import.meta.url).href;

registerHooks({
    resolve(specifier, context, nextResolve) {
        if (specifier === '@/api/http') {
            return { url: STUB_URL, shortCircuit: true };
        }
        return nextResolve(specifier, context);
    },
});

const stub = await import(STUB_URL);
const {
    fetchFixedExpenseCandidate,
    fetchFixedExpenseDetail,
    fetchFixedExpenseOverview,
    fetchFixedExpenseSavings,
    confirmFixedExpenseCandidate,
    dismissFixedExpenseCandidate,
    runFixedExpensePaymentReminderBatch,
    resetFixedExpensePaymentReminders,
} = await import('../src/api/fixedExpense.js');

test('고정지출 관리 목록은 API 모드에서 기간·카테고리 필터를 전달한다', async () => {
    stub.reset();
    stub.setGetResponses([{ summary: {}, confirmed: [], candidates: [] }]);

    await fetchFixedExpenseOverview({ yearMonth: '2026-08', categoryId: 3 });

    assert.deepEqual(stub.calls, [
        {
            method: 'get',
            args: ['/fixedExpenses/candidates', { params: { yearMonth: '2026-08', categoryId: 3 } }],
        },
    ]);
});

test('후보와 확정 항목 상세는 같은 endpoint 응답을 화면 모델로 변환한다', async () => {
    stub.reset();
    stub.setGetResponses([
        {
            item: { id: 101, isConfirmed: true, name: '넷플릭스' },
            paymentHistory: [{ id: 1, date: '2026-08-10', amount: 17000 }],
            evidenceMonths: [{ month: 8, detected: true }],
            sixMonthTotal: 81500,
            changeNotice: { month: 8, difference: 3500 },
        },
        {
            item: { id: 201, isConfirmed: false, name: '유튜브 프리미엄' },
            paymentHistory: [],
            evidenceMonths: [],
            sixMonthTotal: 0,
            changeNotice: null,
        },
    ]);

    const confirmed = await fetchFixedExpenseDetail(101);
    const candidate = await fetchFixedExpenseCandidate(201);

    assert.equal(confirmed.name, '넷플릭스');
    assert.equal(confirmed.paymentHistory.length, 1);
    assert.equal(confirmed.sixMonthTotal, 81500);
    assert.equal(candidate.isConfirmed, false);
    assert.deepEqual(stub.calls.map((call) => call.args[0]), [
        '/fixedExpenses/candidates/101',
        '/fixedExpenses/candidates/201',
    ]);
});

test('절약 감정서는 yearMonth를 전달하고 ApiResponse에서 풀린 payload를 그대로 쓴다', async () => {
    stub.reset();
    stub.setGetResponses([{ monthlySavings: 72000, yearlySavings: 864000, items: [] }]);

    const report = await fetchFixedExpenseSavings('2026-08');

    assert.equal(report.yearlySavings, 864000);
    assert.deepEqual(stub.calls[0], {
        method: 'get',
        args: ['/fixedExpenses/savingReport', { params: { yearMonth: '2026-08' } }],
    });
});

test('후보 확정과 제외는 DB 반영 API에 각각 CONFIRM·EXCLUDE action을 보낸다', async () => {
    stub.reset();
    stub.setPatchResponses([
        { candidateId: 101, isConfirmed: true, isExcluded: false },
        { candidateId: 201, isConfirmed: false, isExcluded: true },
    ]);

    await confirmFixedExpenseCandidate(101);
    await dismissFixedExpenseCandidate(201);

    assert.deepEqual(stub.calls, [
        {
            method: 'patch',
            args: ['/fixedExpenses/candidates/101', { action: 'CONFIRM' }],
        },
        {
            method: 'patch',
            args: ['/fixedExpenses/candidates/201', { action: 'EXCLUDE' }],
        },
    ]);
});

test('개발용 결제 예정 알림은 수동 실행하고 현재 사용자의 테스트 이력만 초기화한다', async () => {
    stub.reset();
    stub.setPostResponses([{ batch: 'fixed-expense-payment-reminders', affected: 1 }]);
    stub.setDeleteResponses([{ deletedNotifications: 1, deletedReminderHistory: 1 }]);

    await runFixedExpensePaymentReminderBatch();
    await resetFixedExpensePaymentReminders();

    assert.deepEqual(stub.calls, [
        { method: 'post', args: ['/dev/batches/fixed-expense-payment-reminders'] },
        { method: 'delete', args: ['/dev/batches/fixed-expense-payment-reminders'] },
    ]);
});
