import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { keyboardInsetOf } from '../src/components/common/useOverlay.js';

/*
 * PWA(설치형) 아이폰 회귀 방지 — 2026-08-14 팀원 검증에서 나온 3건.
 *
 * 세 증상 모두 **안드로이드에서는 재현되지 않는다.** env(safe-area-inset-bottom) 이 0 이고
 * 키보드가 올라오면 뷰포트가 줄어들기 때문이다. 그래서 개발 중에는 눈에 띄지 않는다 —
 * 아래 검사가 없으면 같은 실수가 다음 화면에서 그대로 반복된다.
 *
 * 이 프로젝트에는 컴포넌트 렌더링 하네스가 없어(node:test + 순수 JS) 소스를 검사한다.
 * 계산이 들어가는 부분(keyboardInsetOf)만 실제 단위 테스트다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/**
 * 하단 탭바 위에 fixed 로 띄우는 요소들.
 * bottom 계산에 env(safe-area-inset-bottom) 이 빠지면 아이폰에서 탭바에 깔린다.
 * (재판탭 대법원/지방법원 토글이 이 누락으로 가려졌다)
 */
const FLOATING_ABOVE_TABBAR = [
    ['재판 대법원/지방법원 토글', 'src/components/challenge/ChallengeModeTabBar.css'],
    ['자산 현황/거래내역 토글', 'src/components/asset/AssetLedgerToggle.vue'],
    ['자료실 보고서 토글', 'src/components/challenge/report/ChallengeReportToggle.vue'],
    ['고정지출 후보 하단 버튼', 'src/views/fixed-expense/FixedExpenseCandidateView.vue'],
    ['절약 시뮬레이션 하단 버튼', 'src/views/fixed-expense/FixedExpenseSavingsView.vue'],
    ['개인 랭킹 내 순위 독', 'src/views/challenge/personal/PersonalRankingView.css'],
];

for (const [label, path] of FLOATING_ABOVE_TABBAR) {
    test(`${label} 의 bottom 계산에 safe-area 가 들어 있다`, () => {
        const src = source(path);
        /*
         * 앞의 (?<!-) 는 padding-bottom 을 걸러낸다. 여기서 보는 것은 요소를 띄우는
         * bottom 오프셋뿐이다 — 콘텐츠 아래 여백은 조금 모자라도 가려지지 않는다.
         */
        const offsets = src.match(/(?<!-)bottom:\s*calc\([^;]*--tt-tabbar-height[^;]*\);/g) ?? [];

        assert.ok(
            offsets.length > 0,
            `${path} 에서 --tt-tabbar-height 기준 bottom 계산을 찾지 못했다 — 목록을 갱신할 것`,
        );
        for (const offset of offsets) {
            assert.ok(
                offset.includes('env(safe-area-inset-bottom)'),
                `${path} 의 "${offset.trim()}" 에 env(safe-area-inset-bottom) 이 없다 — ` +
                    '아이폰 설치형에서 탭바에 가려진다',
            );
        }
    });
}

test('탭바가 홈 인디케이터 자리를 비우고 그만큼 항목 높이를 깎는다', () => {
    const tabbar = source('src/components/common/TheTabBar.vue');
    assert.ok(
        tabbar.includes('padding-bottom: env(safe-area-inset-bottom)'),
        'TheTabBar 에서 홈 인디케이터 예약 여백이 사라졌다',
    );
    assert.ok(
        tabbar.includes('calc(var(--tt-tabbar-height) - var(--tt-tabbar-trim))'),
        '탭 항목 높이가 --tt-tabbar-trim 을 반영하지 않는다 — 아이폰에서 탭바가 98px 로 두꺼워진다',
    );
    assert.ok(
        source('src/assets/tokens.css').includes('--tt-tabbar-trim:'),
        'tokens.css 에 --tt-tabbar-trim 이 없다',
    );
});

test('iOS 에서만 입력창 글자를 16px 로 올린다 (자동 확대 방지)', () => {
    const base = source('src/assets/base.css');
    assert.match(
        base,
        /@supports \(-webkit-touch-callout: none\) \{[\s\S]*?font-size: 16px !important;/,
        'iOS 전용 16px 규칙이 없다 — 닉네임 수정 바텀시트가 포커스 때 확대된다',
    );
});

test('오버레이가 키보드 높이만큼 떠오른다', () => {
    assert.ok(
        source('src/components/common/BaseBottomSheet.vue').includes(
            'padding-bottom: var(--tt-keyboard-inset, 0px)',
        ),
        'BaseBottomSheet 가 --tt-keyboard-inset 을 쓰지 않는다 — 아이폰에서 키보드에 가려진다',
    );
    assert.ok(
        source('src/components/common/BaseModal.vue').includes('var(--tt-keyboard-inset, 0px)'),
        'BaseModal 이 --tt-keyboard-inset 을 쓰지 않는다',
    );
});

/*
 * html 에 scrollbar-gutter: stable 이 걸려 있어 overflow:hidden 을 줘도 스크롤바 자리는
 * 돌아오지 않는다. 잠그기 「전에」 스크롤바 너비를 재서 보정하면 돌려받지도 않은 폭을 깎아,
 * 가운데 정렬된 .tt-app 이 오버레이가 열릴 때마다 좁아진다.
 */
test('스크롤 잠금 보정폭은 잠근 뒤에 잰다 — 오버레이가 열릴 때 화면이 좁아지지 않는다', () => {
    const overlay = source('src/components/common/useOverlay.js');
    assert.ok(
        !/window\.innerWidth - document\.documentElement\.clientWidth/.test(overlay),
        '잠그기 전 스크롤바 너비로 보정하고 있다 — scrollbar-gutter 와 이중 보정된다',
    );
    assert.match(
        overlay,
        /const widthBefore = document\.documentElement\.clientWidth;[\s\S]{0,200}?overflow = 'hidden';[\s\S]{0,200}?document\.documentElement\.clientWidth - widthBefore/,
        '잠금 전후 clientWidth 차이로 보정폭을 재지 않는다',
    );
    assert.ok(
        source('src/assets/base.css').includes('scrollbar-gutter: stable'),
        'base.css 의 scrollbar-gutter 가 사라졌다 — 위 보정 방식의 전제가 바뀌었으니 함께 검토할 것',
    );
});

test('로그인 화면은 dvh 를 써서 주소창 높이만큼 스크롤이 생기지 않는다', () => {
    const login = source('src/views/auth/LoginView.vue');
    assert.ok(login.includes('min-height: 100dvh'), '로그인 화면이 100dvh 를 쓰지 않는다');
    assert.ok(
        login.includes('min-height: 100vh'),
        'dvh 를 모르는 브라우저용 100vh 폴백이 빠졌다 — 순서상 100vh 가 먼저 와야 한다',
    );
    assert.ok(
        source('src/App.vue').includes('min-height: 100dvh'),
        '앱 셸(.tt-app)이 100vh 로 남아 있으면 로그인 화면만 고쳐도 스크롤이 남는다',
    );
});

/* ── 키보드 가림 높이 계산 ─────────────────────────────── */

test('키보드가 없으면 0 이다', () => {
    assert.equal(keyboardInsetOf(844, { height: 844, offsetTop: 0 }), 0);
});

test('키보드가 올라온 만큼 반환한다', () => {
    assert.equal(keyboardInsetOf(844, { height: 508, offsetTop: 0 }), 336);
});

test('iOS 가 화면을 밀어올린 양(offsetTop)도 함께 뺀다', () => {
    /* 보이는 영역이 레이아웃 좌표 100~608 이면 아래로 가려진 높이는 844-508-100 = 236 */
    assert.equal(keyboardInsetOf(844, { height: 508, offsetTop: 100 }), 236);
});

test('소수점은 반올림하고 음수는 0 으로 막는다', () => {
    assert.equal(keyboardInsetOf(844, { height: 843.4, offsetTop: 0 }), 1);
    assert.equal(keyboardInsetOf(844, { height: 900, offsetTop: 0 }), 0);
});

test('visualViewport 가 없는 환경에서는 0 이다', () => {
    assert.equal(keyboardInsetOf(844, undefined), 0);
});
