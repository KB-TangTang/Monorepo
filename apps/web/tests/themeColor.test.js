import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import {
    INK_TOP_ROUTES,
    THEME_COLOR_COURT_DISTRICT,
    THEME_COLOR_COURT_SUPREME,
    THEME_COLOR_INK,
    THEME_COLOR_PAPER,
    applyThemeColor,
    resolveThemeColor,
} from '../src/utils/themeColor.js';

/*
 * 설치형 상태바 색 (2026-08-21).
 *
 * 상태바는 시스템 영역이라 화면이 위에 못 그린다. <meta name="theme-color"> 만이
 * 그 띠의 색을 정하므로, 화면 맨 위 색과 어긋나면 상태바만 다른 색 띠로 뜬다.
 *
 * 이 프로젝트에는 컴포넌트 렌더링 하네스가 없어(node:test + 순수 JS) DOM 이 필요한
 * 부분은 최소 스텁으로 검사하고, 나머지는 소스를 검사한다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

test('어두운 헤더 화면은 각자의 상단 색, 나머지는 종이색을 쓴다', () => {
    assert.equal(resolveThemeColor('login'), THEME_COLOR_INK);
    assert.equal(resolveThemeColor('personalMissionChallenge'), THEME_COLOR_COURT_SUPREME);
    assert.equal(resolveThemeColor('groupChallenge'), THEME_COLOR_COURT_DISTRICT);

    for (const name of ['home', 'asset', 'ledger', 'myPage', 'assetChecking']) {
        assert.equal(resolveThemeColor(name), THEME_COLOR_PAPER, `${name} 은 종이색이어야 한다`);
    }
});

test('INK_TOP_ROUTES 는 어두운 상단 라우트를 빠짐없이 담는다', () => {
    assert.deepEqual(INK_TOP_ROUTES, ['login', 'personalMissionChallenge', 'groupChallenge']);
    for (const name of INK_TOP_ROUTES) {
        assert.notEqual(
            resolveThemeColor(name),
            THEME_COLOR_PAPER,
            `${name} 이 종이색이면 목록에 있을 이유가 없다`,
        );
    }
});

test('이름 없는 라우트도 종이색으로 떨어진다 (undefined 로 크래시하지 않는다)', () => {
    assert.equal(resolveThemeColor(undefined), THEME_COLOR_PAPER);
    assert.equal(resolveThemeColor(null), THEME_COLOR_PAPER);
});

/*
 * 객체 리터럴로 대응표를 만들면 'constructor' · '__proto__' 같은 이름이
 * Object.prototype 의 값을 맞아 색이 아닌 것을 돌려준다. Map 이라 그 일이 없다.
 */
test('Object.prototype 의 키 이름을 라우트로 넣어도 종이색이다', () => {
    for (const name of ['constructor', 'toString', '__proto__', 'hasOwnProperty']) {
        assert.equal(resolveThemeColor(name), THEME_COLOR_PAPER, `${name} 은 종이색이어야 한다`);
    }
});

/** meta 태그 하나만 흉내 내는 최소 문서 스텁. */
function stubDocument({ withMeta }) {
    const created = [];
    const meta = { name: 'theme-color', content: '#ffffff' };
    const wrap = (o) => ({
        setAttribute: (k, v) => {
            o[k] = v;
        },
        getAttribute: (k) => o[k],
    });
    return {
        appended: created,
        querySelector: (sel) =>
            withMeta && sel === 'meta[name="theme-color"]' ? wrap(meta) : null,
        createElement: () => {
            const o = {};
            created.push(o);
            return wrap(o);
        },
        head: {
            appendChild: () => {},
        },
        _meta: meta,
    };
}

test('기존 meta 태그가 있으면 content 만 갈아 끼운다', () => {
    const doc = stubDocument({ withMeta: true });
    applyThemeColor(THEME_COLOR_INK, doc);
    assert.equal(doc._meta.content, THEME_COLOR_INK);
    assert.equal(doc.appended.length, 0, '있는 태그를 두고 새로 만들면 안 된다');
});

test('meta 태그가 없으면 만들어 붙인다', () => {
    const doc = stubDocument({ withMeta: false });
    applyThemeColor(THEME_COLOR_PAPER, doc);
    assert.equal(doc.appended.length, 1);
    assert.equal(doc.appended[0].name, 'theme-color');
    assert.equal(doc.appended[0].content, THEME_COLOR_PAPER);
});

test('문서가 없는 환경(SSR·테스트)에서는 조용히 넘어간다', () => {
    assert.equal(applyThemeColor(THEME_COLOR_INK, null), null);
});

test('라우터가 afterEach 에서 상태바 색을 적용한다', () => {
    const src = source('src/router/index.js');
    assert.match(src, /router\.afterEach/, 'afterEach 훅이 있어야 한다');
    assert.match(src, /applyThemeColor\(resolveThemeColor\(to\.name\)\)/);
    /*
     * beforeEach 에서 바꾸면 가드가 리다이렉트한 경우 실제로 뜨지 않는 화면의 색이 남는다.
     * 훅을 옮길 일이 생기면 이 이유를 먼저 확인할 것.
     * (import 줄은 당연히 걸리므로 beforeEach 본문만 잘라서 본다)
     */
    const guardBody = src.slice(src.indexOf('router.beforeEach'), src.indexOf('router.afterEach'));
    assert.ok(guardBody, 'beforeEach 와 afterEach 가 이 순서로 있어야 한다');
    assert.doesNotMatch(guardBody, /applyThemeColor\(/, 'beforeEach 안에서 적용하면 안 된다');
});

test('index.html 기본 theme-color 가 종이색과 같다', () => {
    const html = source('index.html');
    const match = html.match(/<meta name="theme-color" content="([^"]+)"/);
    assert.ok(match, 'index.html 에 theme-color 메타가 있어야 한다');
    assert.equal(match[1].toLowerCase(), THEME_COLOR_PAPER);
});

test('INK_TOP_ROUTES 에 적힌 라우트가 실제로 존재한다', () => {
    const routerSrc =
        source('src/router/index.js') + source('src/router/personalMissionChallengeRoutes.js');
    for (const name of INK_TOP_ROUTES) {
        assert.match(
            routerSrc,
            new RegExp(`name:\\s*'${name}'`),
            `${name} 라우트가 라우터에 없다. 이름이 바뀌었다면 themeColor.js 도 같이 고쳐라`,
        );
    }
});
