import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

/*
 * 앱 셸 아래 여백(--tt-app-bottom-inset)에 관한 불변식.
 *
 * App.vue 의 <main> 은 고정 탭바에 가리지 않도록 아래 여백을 잡는데, 그 자리는 페이지
 * 요소 **밖**이라 페이지 배경이 닿지 않고 앱 셸의 흰 배경이 띠로 드러났다.
 * base.css 의 전역 규칙 하나가 페이지 루트에 「투명 border + 같은 크기 음수 마진」을 걸어
 * 그 자리를 페이지 배경으로 이어 칠한다. 높이는 (h + X) + (−X) = h 라 그대로다.
 *
 * 이 등식이 한쪽만 바뀌면 전 화면 레이아웃이 조용히 밀린다. 그래서 여기서 묶어둔다.
 * 렌더링 하네스가 없어(node:test + 순수 JS) 소스를 검사한다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/** CSS 에서 셀렉터 하나의 선언 블록만 잘라낸다. */
function rule(css, selector) {
    const at = css.indexOf(selector);
    assert.notEqual(at, -1, `${selector} 규칙이 있어야 한다`);
    return css.slice(at, css.indexOf('}', at));
}

const BASE_CSS = source('src/assets/base.css');
const TOKENS = source('src/assets/tokens.css');

test('페이지 루트가 앱 셸 아래 여백을 자기 배경으로 이어 칠한다', () => {
    const decl = rule(BASE_CSS, '.tt-app__content > * {');
    assert.match(
        decl,
        /border-bottom:\s*var\(--tt-app-bottom-inset\)\s+solid\s+transparent;/,
        'padding 이 아니라 투명 border 여야 한다 — 루트에 padding-bottom 을 직접 쓰는 화면 4곳과 부딪힌다',
    );
    assert.match(
        decl,
        /margin-bottom:\s*calc\(var\(--tt-app-bottom-inset\)\s*\*\s*-1\);/,
        '음수 마진이 border 와 같은 크기여야 부모 높이가 그대로다',
    );
});

test('탭바가 없는 화면은 그 보정을 되돌린다', () => {
    const decl = rule(BASE_CSS, '.tt-app__content--bare > * {');
    assert.match(decl, /border-bottom-width:\s*0;/);
    assert.match(decl, /margin-bottom:\s*0;/);
    /* --bare 규칙이 앞에 오면 특이도가 같아 위 규칙에 져버린다 */
    assert.ok(
        BASE_CSS.indexOf('.tt-app__content--bare > * {') >
            BASE_CSS.indexOf('.tt-app__content > * {'),
        '--bare 규칙은 일반 규칙보다 뒤에 있어야 한다',
    );
});

test('App.vue 는 같은 토큰으로 아래 여백을 잡는다', () => {
    assert.match(
        rule(source('src/App.vue'), '.tt-app__content {'),
        /padding-bottom:\s*var\(--tt-app-bottom-inset\);/,
        '셸 여백과 페이지 보정이 같은 토큰이어야 서로 상쇄된다',
    );
});

test('하단에 뜨는 토글을 피하는 여백을 토큰으로 둔다', () => {
    assert.match(TOKENS, /--tt-float-toggle-inset:\s*calc\(/);
    /* 토글은 탭바 위에 뜬다. 탭바 높이를 다시 더하면 이중이 돼 빈 화면이 남는다 */
    assert.doesNotMatch(
        rule(TOKENS, '--tt-float-toggle-inset: calc('),
        /--tt-tabbar-height/,
        '탭바 몫은 --tt-app-bottom-inset 이 이미 맡는다',
    );
});

test('ChallengeModeTabBar 를 띄우는 화면은 그 토큰만큼 본문을 비운다', () => {
    for (const [path, selector] of [
        ['src/views/challenge/personal/PersonalMissionHomeView.css', '.personal-home__content {'],
        ['src/views/challenge/group/GroupChallengeHomeView.vue', '.gc-page {'],
    ]) {
        assert.match(
            rule(source(path), selector),
            /var\(--tt-float-toggle-inset\)/,
            `${path} — 이 여백이 없으면 마지막 줄이 모드 전환 토글에 가린다`,
        );
    }
});
