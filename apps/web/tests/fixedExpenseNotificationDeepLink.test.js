import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

test('고정지출 상세은 항상 서버 데이터를 조회한다', () => {
    const source = readFileSync(
        new URL('../src/views/fixed-expense/FixedExpenseDetailView.vue', import.meta.url),
        'utf8',
    );

    assert.ok(source.includes('store.loadExpense(route.params.expenseId)'));
    assert.doesNotMatch(source, /shouldUseFixedExpenseApiSource|store\.setSource|mock/);
});
