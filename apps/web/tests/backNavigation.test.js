import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 뒤로가기 회귀 방지 (이슈 #77).
 *
 * 이 프로젝트에는 컴포넌트 렌더링 테스트 하네스가 없다(node:test + 순수 JS 단위 테스트뿐).
 * 그래서 소스에 공용 뒤로가기 컴포넌트가 실제로 들어 있는지를 검사한다.
 * 렌더 결과까지 보증하지는 못하지만, **헤더에서 뒤로가기가 통째로 빠지는** 이번 같은 회귀는 잡는다.
 *
 * 화면을 새로 만들 때 뒤로가기가 필요하면 아래 목록에 한 줄 추가할 것.
 */

/** 진입 경로가 있어 반드시 빠져나갈 수단이 필요한 화면 */
const NEEDS_BACK = [
    ['알림 목록', 'src/views/NotificationListView.vue'],
    ['연결 계좌 관리', 'src/views/account/ConnectedAccountView.vue'],
    ['동의 관리', 'src/views/my/ConsentManageView.vue'],
];

/**
 * 뒤로가기가 **있으면 안 되는** 화면.
 * 게이트(로그인·동의)와 되돌아가면 안 되는 플로우 화면이다.
 */
const MUST_NOT_HAVE_BACK = [
    ['로그인', 'src/views/auth/LoginView.vue'],
    ['서비스 동의', 'src/views/consent/ServiceConsentView.vue'],
    ['금융정보 동의', 'src/views/consent/FinancialConsentView.vue'],
    ['계좌 조회 진행', 'src/views/account/LinkProgressView.vue'],
    ['계좌 연결 완료', 'src/views/account/LinkDoneView.vue'],
];

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

for (const [label, path] of NEEDS_BACK) {
    test(`${label} 화면에 공용 뒤로가기가 있다`, () => {
        const src = source(path);
        assert.ok(
            src.includes('BaseBackButton'),
            `${path} 에 BaseBackButton 이 없다 — 사용자가 이 화면에서 나갈 수 없다`,
        );
    });
}

for (const [label, path] of MUST_NOT_HAVE_BACK) {
    test(`${label} 화면에는 뒤로가기를 두지 않는다`, () => {
        const src = source(path);
        assert.ok(
            !src.includes('BaseBackButton'),
            `${path} 에 뒤로가기가 생겼다 — 게이트·플로우 화면은 되돌아가면 안 된다`,
        );
    });
}

test('뒤로가기가 필요한 화면은 목적지를 고정하지 않는다 — 진입 경로가 둘 이상이다', () => {
    /*
     * 연결 계좌 관리는 자산 홈과 마이페이지 양쪽에서 들어온다.
     * to 를 주면 어디서 들어왔든 한 곳으로만 나가 사용자가 원래 자리로 돌아가지 못한다.
     */
    for (const [label, path] of NEEDS_BACK) {
        const src = source(path);
        const tag = src.match(/<BaseBackButton[^>]*>/s);
        assert.ok(tag, `${label}: BaseBackButton 태그를 찾지 못했다`);
        assert.ok(
            !/\bto=/.test(tag[0]),
            `${label}: to 로 목적지를 고정했다 — router.back() 이어야 한다 (${tag[0]})`,
        );
    }
});

test('탐지 후보 상세는 방문 이력을 한 단계씩 되돌린다', () => {
    const src = source('src/views/fixed-expense/FixedExpenseCandidateView.vue');

    assert.ok(
        src.includes('back-label="이전 화면으로 이동"'),
        '탐지 후보 상세에 FixedExpensePageHeader 뒤로가기 버튼이 없다',
    );
    assert.ok(src.includes('@back="goBack"'), '탐지 후보 상세의 헤더 뒤로가기 연결이 없다');
    assert.match(
        src,
        /function goBack\(\) \{\s*router\.back\(\);\s*\}/,
        '탐지 후보 상세은 router.back()으로 이전 화면을 되돌려야 한다',
    );
});

test('월간 소비 리포트와 챌린지 리포트에는 좌상단 뒤로가기를 표시하지 않는다', () => {
    const monthlyReport = source('src/views/report/MonthlyConsumptionReportView.vue');
    const challengeReport = source('src/views/challenge/report/ChallengeReportView.vue');
    const challengeHeader = source('src/components/challenge/ChallengePageHeader.vue');

    assert.ok(!monthlyReport.includes('monthly-report__back'));
    assert.ok(!monthlyReport.includes('@click="router.back()"'));
    assert.ok(challengeReport.includes(':show-back="false"'));
    assert.ok(challengeHeader.includes('v-if="showBack"'));
});
