import test from 'node:test';
import assert from 'node:assert/strict';
import { FIXED_EXPENSE_FIXTURE } from '../src/fixtures/fixedExpense.js';
import {
    applyCandidateDecision,
    calculateDday,
    formatBillingCycle,
    formatDday,
    formatPaymentDate,
    formatSavingsWon,
    formatWon,
    resolveFixedExpenseState,
} from '../src/utils/fixedExpense.js';

test('고정지출 표시 값을 포맷한다', () => {
    assert.equal(formatWon(29800), '29,800원');
    assert.equal(formatSavingsWon(14900), '-14,900원');
    assert.equal(formatPaymentDate('2026-07-17'), '2026.07.17');
    assert.equal(formatBillingCycle({ type: 'monthly', day: 17 }), '매월 17일');
    assert.equal(calculateDday('2026-08-17', new Date(2026, 7, 3)), 14);
    assert.equal(formatDday('2026-08-17', new Date(2026, 7, 3)), 'D-14');
});

test('조회 상태의 우선순위를 판정한다', () => {
    assert.equal(resolveFixedExpenseState({ loading: true, error: '오류', data: {} }), 'loading');
    assert.equal(resolveFixedExpenseState({ error: '오류', data: {} }), 'error');
    assert.equal(resolveFixedExpenseState({ data: null }), 'empty');
    assert.equal(resolveFixedExpenseState({ data: {} }), 'ready');
});

test('목록과 상세가 같은 식별자로 연결된다', () => {
    const listItem = FIXED_EXPENSE_FIXTURE.confirmed[0];
    const expense = FIXED_EXPENSE_FIXTURE.confirmed.find((item) => item.id === listItem.id);
    assert.equal(expense.name, listItem.name);
    assert.equal(expense.status, 'ACTIVE');
    assert.notEqual(expense.confirmedAt, null);
    assert.equal(FIXED_EXPENSE_FIXTURE.candidates[0].confirmedAt, null);
});

test('탐지 후보를 고정지출로 지정한다', () => {
    const { state } = applyCandidateDecision(FIXED_EXPENSE_FIXTURE, 'youtube-premium', 'confirm');
    assert.equal(state.candidates.length, 0);
    assert.equal(
        state.confirmed.some((item) => item.id === 'youtube-premium'),
        true,
    );
    assert.equal(state.overview.confirmedCount, 5);
    assert.equal(state.overview.candidateCount, 1);
    assert.equal(state.confirmed.at(-1).status, 'ACTIVE');
    assert.notEqual(state.confirmed.at(-1).confirmedAt, null);
});

test('고정지출이 아닌 후보를 제외한다', () => {
    const { state, result } = applyCandidateDecision(
        FIXED_EXPENSE_FIXTURE,
        'youtube-premium',
        'dismiss',
    );
    assert.equal(state.candidates.length, 0);
    assert.equal(state.overview.confirmedCount, 4);
    assert.equal(state.overview.candidateCount, 1);
    assert.deepEqual(result, { id: 'youtube-premium', status: 'dismissed' });
});

test('존재하지 않는 항목은 명시적인 오류를 반환한다', () => {
    assert.throws(() => applyCandidateDecision(FIXED_EXPENSE_FIXTURE, 'missing', 'confirm'), {
        code: 'NOT_FOUND',
    });
});
