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

/*
 * 그룹챌린지 판결 플로우 회귀 방지 (이슈 #172).
 *
 * 홈 →(push) 상세 →(push) 재판현황 →(push) 최종판결 →(push) 판결상세 순으로 쌓인 뒤
 * 판결 화면의 「그룹 화면으로 돌아가기」가 `push` 면 상세가 스택 맨 위에 다시 얹힌다.
 * 그 상태에서 상세의 뒤로가기가 `router.back()` 이면 방금 본 판결문으로 되돌아간다.
 */
const RETURNS_TO_DETAIL = [
    ['재판 진행 현황', 'src/views/challenge/group/judgment/TrialProgressView.vue'],
    ['최종 판결', 'src/views/challenge/group/judgment/VerdictResultView.vue'],
    ['판결 상세', 'src/views/challenge/group/judgment/VerdictDetailView.vue'],
    ['탕이 판결 결과', 'src/views/challenge/group/judgment/GroupAiVerdictResultView.vue'],
];

for (const [label, path] of RETURNS_TO_DETAIL) {
    test(`${label} 화면은 그룹 상세로 replace 로 돌아간다`, () => {
        const src = source(path);
        assert.ok(
            !/router\.push\(\s*\{[^}]*groupChallengeDetail/s.test(src),
            `${path}: 상세로 push 하고 있다 — 판결 플로우 위에 상세가 다시 쌓인다`,
        );
        assert.ok(
            /router\.replace\(\s*\{[^}]*groupChallengeDetail/s.test(src),
            `${path}: 상세로 돌아가는 replace 가 없다`,
        );
    });
}

test('공용 뒤로가기 헤더는 @back 을 건 화면에 목적지를 넘긴다', () => {
    /*
     * `defineEmits` 가 없으면 `@back` 은 선언되지 않은 리스너로 루트 엘리먼트에 흘러가
     * **버튼을 눌러도 호출되지 않는다.** 위 RETURNS_TO_DETAIL 검사는 소스만 보기 때문에
     * 이 회귀를 잡지 못한다 — 헤더가 emit 을 잃으면 replace 는 죽은 코드가 된다.
     */
    const src = source('src/components/common/BaseBackHeader.vue');

    assert.match(src, /defineEmits\(\['back'\]\)/, 'BaseBackHeader 가 back 을 emit 하지 않는다');
    assert.ok(
        /onBack/.test(src),
        'BaseBackHeader 가 부모 리스너 유무를 보지 않는다 — router.back() 과 부모 이동이 겹친다',
    );
});

test('재판 종착 화면의 좌상단은 뒤로가기가 아니라 닫기다', () => {
    /*
     * 탕이 판결 결과 뒤에는 판사봉 애니메이션(`groupAiVerdict`)밖에 없다.
     * 화살표를 두면 판결을 확인한 사용자가 방금 본 연출을 다시 본다.
     */
    const src = source('src/views/challenge/group/judgment/GroupAiVerdictResultView.vue');

    assert.ok(src.includes('nav-action="close"'), '탕이 판결 결과 헤더가 아직 뒤로가기 화살표다');
    assert.ok(src.includes('@close="goGroupHome"'), '닫기가 그룹 상세로 연결되지 않았다');

    /* 닫기는 히스토리를 밟지 않는다 — 헤더가 router.back() 을 부르면 X 를 눌러도 애니메이션이 돈다. */
    const header = source('src/components/challenge/group/DefenseCourtHeader.vue');
    assert.match(
        header,
        /navAction === 'close'\)\s*\{\s*emit\('close'\);\s*return;/,
        'DefenseCourtHeader 의 닫기가 router.back() 을 함께 부른다',
    );
});

/*
 * 2026-08-19(이슈 #303)에 「무조건 홈으로 간다」에서 「진입 경로를 보고 정한다」로 바뀌었다.
 * 홈 하드코딩이 목록에서 들어온 사용자까지 홈으로 튕겼기 때문이다.
 *
 * **여기서 지키는 것은 여전히 이슈 #172 다** — 조건 없는 `router.back()` 이면 판결 플로우가
 * `replace` 로 돌아온 뒤 방금 본 재판 화면으로 되돌아간다. 어떤 경우에 back 을 쓰는지는
 * groupChallengeBackNavigation.test.js 가 규칙 단위로 검증한다.
 */
test('그룹챌린지 상세의 뒤로가기는 조건 없이 히스토리를 밟지 않는다', () => {
    const src = source('src/views/challenge/group/GroupChallengeDetailView.vue');

    assert.ok(
        !/function goBack\(\) \{\s*router\.back\(\);\s*\}/.test(src),
        '상세가 조건 없는 router.back() 이면 재판·판결 플로우로 되돌아간다',
    );
    assert.match(
        src,
        /resolveBack\(window\.history\.state/,
        '진입 경로 표시를 보고 정해야 한다 (utils/groupChallengeNavigation)',
    );
});

test('월간 소비 리포트와 챌린지 리포트에는 좌상단 뒤로가기를 표시하지 않는다', () => {
    const monthlyReport = source('src/views/report/MonthlyConsumptionReportView.vue');
    const challengeReport = source('src/views/challenge/report/ChallengeReportView.vue');

    assert.ok(!monthlyReport.includes('monthly-report__back'));
    assert.ok(!monthlyReport.includes('@click="router.back()"'));
    assert.ok(challengeReport.includes('<h1>재판 보고서</h1>'));
    assert.ok(!challengeReport.includes('@click="router.back()"'));
});
