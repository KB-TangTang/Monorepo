import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('월간 리포트 고정지출 카드는 관리 화면으로 이동하고 완료 티켓을 사용하지 않는다', () => {
    const src = source('src/views/report/MonthlyConsumptionReportView.vue');

    assert.ok(src.includes('v-if="shouldShowFixedExpenseCard"'));
    assert.ok(src.includes('<h2>고정 지출</h2>'));
    assert.ok(src.includes("router.push({ name: 'fixedExpenseManagement' });"));
    assert.ok(!src.includes('MonthlySavingsCompleteTicket'));
    assert.ok(!src.includes('fixedExpenseSavings'));
    assert.ok(src.includes('MonthlySavingsAnalogyCard'));
});

test('절약 감정서 화면 소스는 보존하되 폐기 라우트는 등록하지 않는다', () => {
    const routerSource = source('src/router/index.js');
    const savingsView = new URL(
        '../src/views/fixed-expense/FixedExpenseSavingsView.vue',
        import.meta.url,
    );

    assert.ok(existsSync(savingsView));
    assert.ok(!routerSource.includes("path: '/asset/fixed-expenses/savings'"));
    assert.ok(!routerSource.includes("name: 'fixedExpenseSavings'"));
});
