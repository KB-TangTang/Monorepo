import test from 'node:test';
import assert from 'node:assert/strict';
import { filterTransactionsByTab, resolveAnchorDate } from '../src/utils/ledger.js';

const SAMPLE = [
    { id: 't1', date: '2026-07-29', amount: -48900, classification: 'CONSUMPTION' },
    { id: 't2', date: '2026-07-25', amount: 3420000, classification: 'INCOME' },
    { id: 't3', date: '2026-07-20', amount: -50000, classification: 'TRANSFER' },
];

test('filterTransactionsByTab: ALL 은 전부 반환한다', () => {
    assert.equal(filterTransactionsByTab(SAMPLE, 'ALL').length, 3);
});

test('filterTransactionsByTab: CONSUMPTION 은 지출 거래만 반환한다', () => {
    const result = filterTransactionsByTab(SAMPLE, 'CONSUMPTION');
    assert.deepEqual(
        result.map((tx) => tx.id),
        ['t1'],
    );
});

test('filterTransactionsByTab: INCOME 은 입금 거래만 반환한다', () => {
    const result = filterTransactionsByTab(SAMPLE, 'INCOME');
    assert.deepEqual(
        result.map((tx) => tx.id),
        ['t2'],
    );
});

test('filterTransactionsByTab: TRANSFER 거래는 CONSUMPTION·INCOME 어느 쪽에도 안 잡힌다', () => {
    const consumption = filterTransactionsByTab(SAMPLE, 'CONSUMPTION');
    const income = filterTransactionsByTab(SAMPLE, 'INCOME');
    assert.equal(
        consumption.some((tx) => tx.id === 't3') || income.some((tx) => tx.id === 't3'),
        false,
    );
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
