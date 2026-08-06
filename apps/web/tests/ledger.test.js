import test from 'node:test';
import assert from 'node:assert/strict';
import { filterTransactionsByTab, resolveAnchorDate } from '../src/utils/ledger.js';
import { getLedgerTransactions } from '../src/fixtures/ledger.js';

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

test('fixture 거래는 모두 유효한 classification 을 갖는다', () => {
    const allTransactions = ['2026-06', '2026-07'].flatMap((period) =>
        getLedgerTransactions(period),
    );
    assert.ok(allTransactions.length > 0);
    for (const tx of allTransactions) {
        assert.ok(
            ['CONSUMPTION', 'TRANSFER', 'INCOME'].includes(tx.classification),
            `${tx.id} has invalid classification: ${tx.classification}`,
        );
    }
});

test('fixture 거래의 classification 은 amount 부호와 모순되지 않는다', () => {
    const allTransactions = ['2026-06', '2026-07'].flatMap((period) =>
        getLedgerTransactions(period),
    );
    for (const tx of allTransactions) {
        if (tx.classification === 'INCOME') {
            assert.ok(tx.amount > 0, `${tx.id} is INCOME but amount <= 0`);
        }
        if (tx.classification === 'CONSUMPTION') {
            assert.ok(tx.amount < 0, `${tx.id} is CONSUMPTION but amount >= 0`);
        }
    }
});

import { sumByDirection } from '../src/api/ledger.js';

test('sumByDirection: CONSUMPTION 만 지출로, INCOME 만 입금으로 합산한다', () => {
    const result = sumByDirection([
        { amount: -10000, classification: 'CONSUMPTION' },
        { amount: -20000, classification: 'CONSUMPTION' },
        { amount: 100000, classification: 'INCOME' },
    ]);
    assert.deepEqual(result, { totalSpent: 30000, totalDeposit: 100000 });
});

test('sumByDirection: TRANSFER 거래는 지출·입금 어느 합계에도 안 들어간다 (카드정산 중복집계 방지)', () => {
    const result = sumByDirection([
        { amount: -10000, classification: 'CONSUMPTION' },
        { amount: -10000, classification: 'TRANSFER' },
        { amount: 100000, classification: 'INCOME' },
    ]);
    assert.deepEqual(result, { totalSpent: 10000, totalDeposit: 100000 });
});
