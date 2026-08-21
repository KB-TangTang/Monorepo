import test from 'node:test';
import assert from 'node:assert/strict';
import { filterTransactionsByTab, resolveAnchorDate } from '../src/utils/ledger.js';

const SAMPLE = [
    { id: 't1', date: '2026-07-29', amount: -48900, classification: 'CONSUMPTION', isRefund: false },
    { id: 't2', date: '2026-07-25', amount: 3420000, classification: 'INCOME', isRefund: false },
    { id: 't3', date: '2026-07-20', amount: -50000, classification: 'TRANSFER', isRefund: false },
    // 적금 납입처럼 계좌로 들어오는 TRANSFER — #389: "입금" 은 여기도 포함해야 한다.
    { id: 't4', date: '2026-07-15', amount: 300000, classification: 'TRANSFER', isRefund: false },
    // 환불 — 금액은 양수지만 소비의 정정일 뿐 유입이 아니라 "입금" 에서 빠져야 한다.
    { id: 't5', date: '2026-07-10', amount: 48900, classification: 'CONSUMPTION', isRefund: true },
];

test('filterTransactionsByTab: ALL 은 전부 반환한다', () => {
    assert.equal(filterTransactionsByTab(SAMPLE, 'ALL').length, 5);
});

test('filterTransactionsByTab: CONSUMPTION 은 지출 거래만 반환한다 (환불 포함)', () => {
    const result = filterTransactionsByTab(SAMPLE, 'CONSUMPTION');
    assert.deepEqual(
        result.map((tx) => tx.id),
        ['t1', 't5'],
    );
});

test('filterTransactionsByTab: INCOME 은 classification 과 무관하게 계좌로 들어오는 돈(환불 제외)을 반환한다', () => {
    const result = filterTransactionsByTab(SAMPLE, 'INCOME');
    assert.deepEqual(
        result.map((tx) => tx.id),
        ['t2', 't4'],
    );
});

test('filterTransactionsByTab: 금액이 음수인 TRANSFER 는 지출·입금 어느 쪽에도 안 잡힌다', () => {
    const consumption = filterTransactionsByTab(SAMPLE, 'CONSUMPTION');
    const income = filterTransactionsByTab(SAMPLE, 'INCOME');
    assert.equal(
        consumption.some((tx) => tx.id === 't3') || income.some((tx) => tx.id === 't3'),
        false,
    );
});

test('filterTransactionsByTab: 환불은 금액이 양수여도 입금에 포함되지 않는다', () => {
    const income = filterTransactionsByTab(SAMPLE, 'INCOME');
    assert.equal(income.some((tx) => tx.id === 't5'), false);
});

test('resolveAnchorDate: 그룹에 존재하는 날짜면 그대로 반환한다', () => {
    const groups = [{ date: '2026-07-29' }, { date: '2026-07-25' }];
    assert.equal(resolveAnchorDate(groups, '2026-07-25'), '2026-07-25');
});

test('resolveAnchorDate: 그룹에 없는 날짜면 null 을 반환한다', () => {
    const groups = [{ date: '2026-07-29' }];
    assert.equal(resolveAnchorDate(groups, '2026-06-01'), null);
});

test('resolveAnchorDate: 요청 날짜가 없으면 null 을 반환한다', () => {
    const groups = [{ date: '2026-07-29' }];
    assert.equal(resolveAnchorDate(groups, null), null);
    assert.equal(resolveAnchorDate(groups, ''), null);
});

