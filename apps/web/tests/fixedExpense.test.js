import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import {
    calculateDday,
    formatBillingCycle,
    formatDday,
    formatPaymentDate,
    formatSavingsWon,
    formatWon,
    resolveFixedExpenseState,
} from '../src/utils/fixedExpense.js';

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

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

test('고정지출 상세는 진입 경로와 무관하게 서버 데이터를 조회한다', () => {
    const detailView = source('src/views/fixed-expense/FixedExpenseDetailView.vue');

    assert.ok(detailView.includes('store.loadExpense(route.params.expenseId)'));
    assert.doesNotMatch(detailView, /source|mock|TempFixedExpenseSourceToggle/);
});

test('고정지출 스토어는 실 API 호출만 사용한다', () => {
    const store = source('src/stores/fixedExpense.js');

    assert.match(store, /from '@\/api\/fixedExpense';/);
    assert.doesNotMatch(store, /tempFixedExpenseMock|source === 'mock'|setSource/);
});

test('후보 확정은 서버 요청 결과를 다시 목록에 반영한다', () => {
    const store = source('src/stores/fixedExpense.js');

    assert.match(store, /await confirmFixedExpenseCandidate\(candidateId\);/);
    assert.match(store, /await this\.loadOverview\(\);/);
});

test('후보 제외는 서버 요청으로 처리한다', () => {
    const store = source('src/stores/fixedExpense.js');

    assert.match(store, /await dismissFixedExpenseCandidate\(candidateId\);/);
});

test('고정지출 화면은 목업 상태 전이 유틸리티를 포함하지 않는다', () => {
    const utils = source('src/utils/fixedExpense.js');

    assert.doesNotMatch(utils, /applyCandidateDecision|FIXED_EXPENSE_FIXTURE/);
});
