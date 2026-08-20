import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

test('실제 결제 예정 알림으로 연 상세는 API 출처를 선택한다', () => {
    const source = readFileSync(
        new URL('../src/views/fixed-expense/FixedExpenseDetailView.vue', import.meta.url),
        'utf8',
    );

    assert.ok(source.includes('shouldUseFixedExpenseApiSource(route.query.source)'));
    assert.ok(source.includes("store.setSource('api');"));
});
