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

test('절약 감정서 화면은 소스까지 지워져 어떤 경로로도 살아나지 않는다', () => {
    const routerSource = source('src/router/index.js');
    const savingsView = new URL(
        '../src/views/fixed-expense/FixedExpenseSavingsView.vue',
        import.meta.url,
    );
    const completeTicket = new URL(
        '../src/components/report/monthly-consumption/MonthlySavingsCompleteTicket.vue',
        import.meta.url,
    );

    /* 진단을 고정지출 관리 한 곳에서만 본다 - 2026-08-21. 라우트만 빼고 소스를 남겨 뒀던
       2026-08-14 결정(#228)을 뒤집은 것이라 파일이 없다는 사실 자체를 여기서 못박는다.
       계산 로직과 API 는 재도입에 대비해 그대로 둔다. */
    assert.ok(!existsSync(savingsView));
    assert.ok(!existsSync(completeTicket));
    assert.ok(!routerSource.includes("path: '/asset/fixed-expenses/savings'"));
    assert.ok(!routerSource.includes("name: 'fixedExpenseSavings'"));
});

test('고정지출 관리와 두 상세 화면은 경로가 자산이어도 자료실 탭을 활성화한다', () => {
    const routerSource = source('src/router/index.js');
    const tabBarSource = source('src/components/common/TheTabBar.vue');

    for (const routeName of [
        'fixedExpenseManagement',
        'fixedExpenseCandidate',
        'fixedExpenseDetail',
    ]) {
        assert.match(
            routerSource,
            new RegExp(
                `name: '${routeName}',[\\s\\S]*?meta: \\{ title: '[^']+', tabBar: 'ledger' \\}`,
            ),
            `${routeName} 라우트는 자료실 탭을 지정해야 한다`,
        );
    }
    assert.ok(
        tabBarSource.includes('route.meta.tabBar === tab.name'),
        '탭바가 라우트의 탭 지정값을 URL 규칙보다 먼저 반영해야 한다',
    );
});

test('고정지출 관리 화면은 개발용 알림 실행·초기화 도구를 노출하지 않는다', () => {
    const managementSource = source('src/views/fixed-expense/FixedExpenseManagementView.vue');

    assert.doesNotMatch(
        managementSource,
        /isDev|runFixedExpensePaymentReminderBatch|resetFixedExpensePaymentReminders|알림 실행|알림 초기화/,
    );
});
