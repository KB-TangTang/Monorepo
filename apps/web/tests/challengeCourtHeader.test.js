import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import { THEME_COLOR_COURT_DISTRICT, THEME_COLOR_COURT_SUPREME } from '../src/utils/themeColor.js';

/*
 * 재판탭 법원 헤더 (2026-08-21 새 디자인).
 *
 * 요구사항의 핵심은 「개인챌린지와 그룹챌린지의 헤더 높이가 완전히 같다」는 것이다.
 * 처음엔 이미지 박스의 aspect-ratio 만 맞췄는데, 그 아래 말풍선이 **문구 길이만큼 늘어나서**
 * 두 화면이 어긋났다. 그래서 지금은 높이에 관여하는 값이 전부 고정이고, 아래 테스트가
 * 「본문 길이로 늘어날 수 있는 것」이 다시 들어오지 못하게 막는다.
 * 렌더링 하네스가 없어(node:test + 순수 JS) 소스를 검사한다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

const HEADER_VUE = 'src/components/challenge/ChallengeCourtHeader.vue';
const HEADER_CSS = 'src/components/challenge/ChallengeCourtHeader.css';
const PERSONAL_VIEW = 'src/views/challenge/personal/PersonalMissionHomeView.vue';
const GROUP_VIEW = 'src/views/challenge/group/GroupChallengeHomeView.vue';

/** CSS 에서 셀렉터 하나의 선언 블록만 잘라낸다. */
function rule(css, selector) {
    const at = css.indexOf(selector);
    assert.notEqual(at, -1, `${selector} 규칙이 있어야 한다`);
    return css.slice(at, css.indexOf('}', at));
}

test('두 챌린지 홈이 같은 헤더 컴포넌트를 쓴다', () => {
    for (const [path, variant, image, role] of [
        [PERSONAL_VIEW, 'supreme', 'building_supreme_v2.png', '검사'],
        [GROUP_VIEW, 'district', 'building_district_v2.png', '판사'],
    ]) {
        const src = source(path);
        assert.match(src, /<ChallengeCourtHeader\b/, `${path} 가 공용 헤더를 써야 한다`);
        assert.match(src, new RegExp(`variant="${variant}"`), `${path} 의 variant 가 어긋난다`);
        assert.match(
            src,
            new RegExp(image.replace('.', '\\.')),
            `${path} 의 건물 이미지가 어긋난다`,
        );
        assert.match(src, new RegExp(`tangi-role="${role}"`), `${path} 의 명패 직함이 어긋난다`);
    }
});

/*
 * 높이가 갈라지는 경로는 셋뿐이다 — 이미지 박스 · 말풍선 · 탕이.
 * 세 규칙 모두 「내용과 무관한 고정값」이어야 한다.
 */
test('헤더 높이를 정하는 값은 전부 본문 길이와 무관한 고정값이다', () => {
    const css = source(HEADER_CSS);

    assert.match(
        rule(css, '.court-header__hero {'),
        /aspect-ratio:\s*327\s*\/\s*268;/,
        '이미지 박스 높이는 aspect-ratio 하나로만 정해져야 한다',
    );
    assert.match(
        rule(css, '.court-header__speech {'),
        /aspect-ratio:\s*1600\s*\/\s*680;/,
        '말풍선 높이를 텍스트에 맡기면 문구가 긴 화면만 헤더가 길어진다',
    );
    assert.match(
        rule(css, '.court-header__tangi {'),
        /height:\s*84px;/,
        '탕이는 고정 height 여야 한다. 확대는 transform 으로만 한다',
    );
    assert.match(
        rule(css, '.court-header__speech-text {'),
        /overflow:\s*hidden;/,
        '문구가 넘치면 늘어나는 게 아니라 잘려야 한다',
    );
    assert.match(
        rule(css, '.court-header__skirt {'),
        /height:\s*var\(--tt-radius-2xl\);/,
        '하단 곡면도 고정 높이여야 한다',
    );
});

/*
 * 화자 이름(「검사 냉정한 탕이」)은 말풍선 머리의 엠블럼 오른쪽 빈칸에 앉는다.
 * 자리를 px 로 잡으면 폭이 달라졌을 때 엠블럼이나 구분선을 침범한다 — 반드시 % 여야 한다.
 * 실측값: 엠블럼 오른쪽 끝 18.3% · 구분선 35.9% · 안쪽 금테 위 11.3% / 오른쪽 96.5%.
 */
test('화자 이름은 말풍선 기준 % 로만 자리를 잡는다', () => {
    const css = source(HEADER_CSS);
    const name = rule(css, '.court-header__speech-name {');

    assert.match(name, /position:\s*absolute;/, '말풍선(relative) 안에 얹혀야 한다');

    const inset = name.match(/inset:\s*([^;]+);/);
    assert.ok(inset, '.court-header__speech-name 에 inset 이 있어야 한다');

    const sides = inset[1].trim().split(/\s+/).map(Number.parseFloat);
    assert.equal(sides.length, 4, 'inset 은 상 우 하 좌 네 값이어야 한다');
    assert.match(inset[1], /^(\s*[\d.]+%){4}\s*$/, 'px 를 쓰면 폭이 바뀔 때 깨진다');

    const [top, right, bottom, left] = sides;
    assert.ok(left >= 18.3, `왼쪽 ${left}% 는 판사봉 엠블럼(~18.3%)을 침범한다`);
    assert.ok(100 - bottom <= 35.9, `아래 끝 ${100 - bottom}% 가 구분선(35.9%)을 넘는다`);
    assert.ok(top >= 11.3, `위 ${top}% 는 안쪽 금테(11.3%)를 침범한다`);
    assert.ok(100 - right <= 96.5, `오른쪽 끝 ${100 - right}% 가 안쪽 금테(96.5%)를 넘는다`);

    assert.match(name, /white-space:\s*nowrap;/, '이름이 줄바꿈되면 구분선 아래로 흘러내린다');
    assert.match(name, /overflow:\s*hidden;/);

    assert.match(
        source(HEADER_VUE),
        /class="court-header__speech"[\s\S]*?court-header__speech-name/,
        '이름은 말풍선 안에 있어야 한다. 밖에 두면 % 기준이 말풍선이 아니게 된다',
    );
});

/*
 * variant 클래스가 커스텀 프로퍼티 말고 다른 걸 건드리는 순간 두 화면이 갈라진다.
 * 특히 탕이 확대를 width/height 로 하면 행 높이가 달라진다 (실제로 그렇게 될 뻔했다).
 */
test('variant 클래스는 커스텀 프로퍼티만 바꾼다', () => {
    const css = source(HEADER_CSS);

    for (const variant of ['supreme', 'district']) {
        const body = rule(css, `.court-header--${variant} {`);
        const declarations = body.match(/^\s*[a-z-]+\s*:/gm) ?? [];
        for (const declaration of declarations) {
            assert.match(
                declaration.trim(),
                /^--/,
                `.court-header--${variant} 이 ${declaration.trim()} 를 건드린다. 색·배치 변수만 허용한다`,
            );
        }
    }
});

test('건물 이미지는 항상 폭을 넘겨 아래에 붙인다', () => {
    const css = source(HEADER_CSS);
    const img = rule(css, '.court-header__hero-img {');

    assert.match(img, /bottom:\s*0;/, '하단을 붙여야 아래 그라데이션 패널과 이어져 보인다');
    assert.match(img, /width:\s*var\(--court-img-w\);/);

    /*
     * 이미지가 박스 좌우를 다 덮지 못하면 배경색 띠가 드러나 풀블리드 디자인이 깨진다.
     * translateX 의 % 는 **자기 폭 기준**이라 부모 폭 단위로 환산해서 따져야 한다.
     */
    for (const variant of ['supreme', 'district']) {
        const body = rule(css, `.court-header--${variant} {`);
        const width = Number(body.match(/--court-img-w:\s*([\d.]+)%/)[1]);
        const shift = (Number(body.match(/--court-img-x:\s*(-?[\d.]+)%/)[1]) * width) / 100;

        assert.ok(
            50 - width / 2 + shift <= 0,
            `${variant} 의 이미지 왼쪽에 배경이 드러난다 (폭 ${width}% · 이동 ${shift}%)`,
        );
        assert.ok(
            50 + width / 2 + shift >= 100,
            `${variant} 의 이미지 오른쪽에 배경이 드러난다 (폭 ${width}% · 이동 ${shift}%)`,
        );
    }
});

/*
 * 라운드는 **맨 아래 곡면 패널에만** 있어야 한다.
 * .court-header 에 걸면 풀블리드 건물 이미지의 위쪽 모서리까지 깎인다.
 */
test('라운드는 헤더 하단 곡면에만 있다', () => {
    const css = source(HEADER_CSS);

    assert.doesNotMatch(
        rule(css, '.court-header {'),
        /border-radius/,
        '헤더 위쪽은 각진 풀블리드다',
    );

    const skirt = rule(css, '.court-header__skirt {');
    assert.match(
        skirt,
        /border-radius:\s*var\(--tt-radius-2xl\)\s+var\(--tt-radius-2xl\)\s+0\s+0;/,
        '위 두 모서리만 굴려야 본문으로 자연스럽게 이어진다',
    );
    assert.match(
        skirt,
        /background:\s*var\(--tt-bg-subtle\);/,
        '곡면 색이 본문 배경(.personal-home · .gc-page)과 다르면 띠가 생긴다',
    );
});

/* 위 테스트의 --tt-bg-subtle 전제가 화면 쪽에서 조용히 바뀌지 않게 묶어둔다 */
test('두 화면의 본문 배경이 헤더 하단 곡면과 같은 토큰이다', () => {
    for (const [path, selector] of [
        ['src/views/challenge/personal/PersonalMissionHomeView.css', '.personal-home {'],
        [GROUP_VIEW, '.gc-page {'],
    ]) {
        assert.match(
            rule(source(path), selector),
            /background:\s*var\(--tt-bg-subtle\)/,
            `${path} 의 배경이 바뀌면 헤더 곡면 아래에 다른 색 띠가 드러난다`,
        );
    }
});

test('헤더는 색 HEX 를 직접 쓰지 않는다', () => {
    for (const path of [HEADER_VUE, HEADER_CSS]) {
        const offenders = source(path).match(/#[0-9a-fA-F]{3,8}\b/g) ?? [];
        assert.deepEqual(offenders, [], `${path} 는 tokens.css 의 토큰만 참조한다`);
    }
});

/*
 * 설치형 상태바 색은 이미지 상단 색과 같아야 한다. 값이 tokens.css 와 themeColor.js
 * 두 곳에 있어(CSS 변수를 JS 에서 읽을 방법이 없다) 한쪽만 고치면 조용히 어긋난다.
 */
test('tokens.css 의 법원 헤더 상단 색과 상태바 색 상수가 같다', () => {
    const tokens = source('src/assets/tokens.css');

    for (const [name, expected] of [
        ['--tt-court-top-supreme', THEME_COLOR_COURT_SUPREME],
        ['--tt-court-top-district', THEME_COLOR_COURT_DISTRICT],
    ]) {
        const match = tokens.match(new RegExp(`${name}:\\s*(#[0-9a-fA-F]{6});`));
        assert.ok(match, `${name} 토큰이 tokens.css 에 있어야 한다`);
        assert.equal(
            match[1].toLowerCase(),
            expected,
            `${name} 과 themeColor.js 의 상수가 어긋나면 상태바만 다른 색 띠로 뜬다`,
        );
    }
});
