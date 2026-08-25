import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

/*
 * 홈 히어로 헤더 (2026-08-23 새 디자인).
 *
 * 원본은 docs.local/reference/design/home/메인_홈_애니메이션_ver.html 이고,
 * 좌표·주기를 390x500 기준 px 그대로 옮겼다. 아래 테스트가 지키는 것은 셋이다.
 *   1) 헤더가 「정보판」으로 되돌아가지 않게 — 인사말과 알림 벨 말고는 아무것도 얹지 않는다
 *   2) 헤더 잔디 끝 색과 본문 배경색이 갈라지지 않게 — 갈라지면 이음매에 색 띠가 생긴다
 *   3) 원본의 애니메이션 주기·왕복 폭이 조용히 바뀌지 않게
 * 렌더링 하네스가 없어(node:test + 순수 JS) 소스를 검사한다.
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/**
 * CSS 에서 셀렉터 하나의 선언 블록만 잘라낸다.
 * 주석을 먼저 걷어내는 이유 — 주석에 `img { max-width: 100% }` 처럼 중괄호가 들어가면
 * 그 `}` 를 블록 끝으로 잘못 읽어 뒤 선언이 통째로 안 보인다(실제로 한 번 걸렸다).
 */
function rule(css, selector) {
    const bare = css.replace(/\/\*[\s\S]*?\*\//g, '');
    const at = bare.indexOf(selector);
    assert.notEqual(at, -1, `${selector} 규칙이 있어야 한다`);
    return bare.slice(at, bare.indexOf('}', at));
}

/**
 * 실제로 그려지는 마크업만 남긴다.
 * 주석을 걷어내는 이유 — `rule()` 과 같다. 주석은 렌더되지 않으니 「무엇을 그리는가」를
 * 묻는 검사에 걸려서는 안 된다. 설명에 예시를 적었다고 테스트가 깨지면 주석을 못 단다.
 */
function template(vue) {
    const open = vue.indexOf('<template>');
    assert.notEqual(open, -1, '<template> 블록이 있어야 한다');
    return vue.slice(open, vue.indexOf('</template>')).replace(/<!--[\s\S]*?-->/g, '');
}

const HEADER_VUE = 'src/components/home/HomeHeroHeader.vue';
const HEADER_CSS = 'src/components/home/HomeHeroHeader.css';
const HOME_VUE = 'src/views/HomeView.vue';
const HOME_CSS = 'src/views/HomeView.css';

/*
 * 헤더가 쓰는 풍경 PNG. 파일이 하나라도 빠지면 Vite 가 빌드에서 죽는데,
 * 빌드를 돌리지 않는 리뷰에서는 그게 안 보인다.
 */
test('헤더가 import 하는 이미지가 전부 실제로 있다', () => {
    const paths = [...source(HEADER_VUE).matchAll(/from '@\/assets\/([^']+)'/g)].map((m) => m[1]);

    assert.ok(paths.length >= 22, `풍경 이미지가 ${paths.length}개뿐이다. import 가 빠졌다`);
    for (const path of paths) {
        assert.ok(
            existsSync(new URL(`../src/assets/${path}`, import.meta.url)),
            `src/assets/${path} 가 없다`,
        );
    }
});

/*
 * 이 헤더의 존재 이유가 「스크롤 없이 보이는 첫 화면을 정보판에서 풍경으로 바꾼다」다.
 * 숫자·상태·이동 버튼이 다시 들어오면 그 이유가 사라진다.
 */
test('헤더는 인사말과 알림 벨 말고 아무것도 얹지 않는다', () => {
    const vue = source(HEADER_VUE);

    assert.match(vue, /<TheNotificationBell \/>/, '알림 벨은 헤더 안에 있다');
    assert.doesNotMatch(vue, /@click|@open/, '헤더에는 알림 벨 말고 누를 것이 없다');
    assert.doesNotMatch(vue, /router|useRouter/, '헤더는 화면을 이동시키지 않는다');
    assert.doesNotMatch(vue, /api\//, '헤더는 데이터를 직접 불러오지 않는다');

    /* 받는 값은 인사말에 쓸 이름 하나뿐이다 */
    const props = [...vue.matchAll(/^\s{4}(\w+): \{/gm)].map((m) => m[1]);
    assert.deepEqual(props, ['userName']);
});

/* 벨은 홈 헤더 한 곳에만 있어야 한다. 본문에 또 두면 같은 화면에 종이 두 개 뜬다 */
test('홈 본문에는 알림 벨이 없다', () => {
    assert.doesNotMatch(source(HOME_VUE), /TheNotificationBell/);
});

/*
 * 잔디 맨 아래(--tt-home-hero-fade)와 본문 배경이 같은 값이어야 한다.
 * 어긋나면 헤더가 끝나는 자리에 색 띠가 한 줄 생긴다. 값이 두 파일에 나뉘어 있어
 * 한쪽만 바꾸면 조용히 어긋난다.
 */
test('헤더 잔디 끝 색과 본문 배경이 같은 토큰이다', () => {
    const tokens = source('src/assets/tokens.css');
    assert.match(
        tokens,
        /--tt-home-hero-fade:\s*var\(--tt-bg-subtle\);/,
        '페이드 끝은 본문 배경 토큰을 그대로 가리켜야 한다',
    );
    assert.match(
        rule(source(HOME_CSS), '.home {'),
        /background:\s*var\(--tt-bg-subtle\);/,
        '본문 배경을 바꾸면 헤더 잔디 끝에 다른 색 띠가 드러난다',
    );

    /* 페이드 시작점은 「같은 색의 알파 0」이어야 한다. transparent 면 중간이 회색으로 뜬다 */
    const paper = tokens.match(/--tt-neutral-paper:\s*#([0-9a-f]{6});/)[1];
    const fadeOut = tokens.match(/--tt-scene-fade-out:\s*rgba\((\d+),\s*(\d+),\s*(\d+),\s*0\)/);
    assert.ok(fadeOut, '--tt-scene-fade-out 은 알파 0 인 rgba 여야 한다');
    assert.equal(
        fadeOut
            .slice(1, 4)
            .map((v) => Number(v).toString(16).padStart(2, '0'))
            .join(''),
        paper,
        '--tt-neutral-paper 와 채널 값이 같아야 한다',
    );
});

/*
 * 원본 씬(__stage) 은 500px 그대로 두고, 밖의 __scene 이 위쪽을 잘라낸다.
 * 좌표를 하나씩 빼는 대신 이렇게 한 이유는 하늘 그라데이션 정지점(42%·66%·71%)이
 * 씬 높이 기준이라 높이를 건드리면 지평선이 소품과 어긋나기 때문이다.
 * 누가 __stage 를 지우고 __scene 높이를 직접 줄이면 그 어긋남이 조용히 돌아온다.
 */
test('잘라내는 창과 원본 좌표계가 분리돼 있다', () => {
    const css = source(HEADER_CSS);
    const scene = rule(css, '.home-hero__scene {');
    const stage = rule(css, '.home-hero__stage {');

    assert.match(stage, /height:\s*500px;/, '__stage 는 원본 씬 높이를 그대로 들고 있어야 한다');
    assert.match(stage, /bottom:\s*0;/, '바닥을 맞춰 걸어야 잘리는 쪽이 늘 위가 된다');
    assert.match(stage, /background:\s*linear-gradient\(/, '하늘 그라데이션은 원본 높이 위에 건다');

    assert.match(scene, /overflow:\s*hidden;/);
    assert.doesNotMatch(scene, /background:/, '창에 배경을 또 깔면 그라데이션이 두 번 그려진다');

    const crop = 500 - Number(scene.match(/height:\s*(\d+)px;/)[1]);
    assert.ok(crop > 0 && crop <= 64, `잘라낸 양 ${crop}px 이 이상하다`);

    assert.match(
        template(source(HEADER_VUE)),
        /home-hero__scene"[\s\S]*?home-hero__stage"/,
        '__scene 안에 __stage 가 들어가야 한다',
    );
});

/*
 * 헤더는 화면 폭을 꽉 채우는 풀블리드다. .home 에 좌우 padding 을 주면
 * 하늘 양옆에 배경색 띠가 생긴다 — 실제로 이전 레이아웃이 그랬다.
 */
test('좌우 여백은 본문만 잡는다', () => {
    const css = source(HOME_CSS);
    assert.doesNotMatch(rule(css, '.home {'), /padding/, '헤더가 잘리거나 양옆에 띠가 생긴다');
    assert.match(rule(css, '.home__body {'), /padding:[^;]*var\(--tt-screen-padding\)/);
});

/* 인사말 왼쪽 끝과 아래 카드 왼쪽 끝이 한 줄로 떨어져야 한다 */
test('인사말 좌우 여백이 본문 카드와 같다', () => {
    const greeting = rule(source(HEADER_CSS), '.home-hero__greeting {');
    assert.match(greeting, /right:\s*var\(--tt-screen-padding\);/);
    assert.match(greeting, /left:\s*var\(--tt-screen-padding\);/);
});

/*
 * 실제로 겪은 버그 — 예전에는 넓은 상자 하나에 구름 두 장을 담고 px 로 밀었다.
 * 이동량(400px)과 구름 간격(402px)이 어긋나 루프마다 튀었고, 480px 화면에서는
 * 오른쪽 구름이 화면 밖으로 반쯤 나가 「가장자리에서 뚝 잘린 구름」이 보였다.
 *
 * 지금은 구름 한 장에 트랙 하나이고, 트랙이 씬과 폭이 같다(left·right 0).
 * 그래야 translateX 의 100% 가 곧 화면 한 폭이 되어, 어떤 기기 폭에서도
 * 시작·끝이 완전히 화면 밖이다. 트랙에 폭이 생기는 순간 이 보장이 깨진다.
 */
test('구름 트랙이 씬과 폭이 같아 어떤 폭에서도 잘리지 않는다', () => {
    const css = source(HEADER_CSS);
    const track = rule(css, '.home-hero__cloud-track {');

    assert.match(track, /left:\s*0;/, '트랙 폭이 씬 폭과 같아야 100% = 한 화면이다');
    assert.match(track, /right:\s*0;/, '트랙 폭이 씬 폭과 같아야 100% = 한 화면이다');

    const drift = css.slice(css.indexOf('@keyframes tt-cloud-drift'));
    const stops = [...drift.slice(0, drift.indexOf('\n}')).matchAll(/translateX\((-?\d+)%\)/g)];
    assert.deepEqual(
        stops.map((m) => Number(m[1])).sort((a, b) => a - b),
        [-100, 100],
        'px 로 밀면 화면 폭이 달라질 때 다시 잘린다. ±100% 여야 한다',
    );
});

/*
 * 트랙의 `transform` 은 애니메이션이 꺼졌을 때(prefers-reduced-motion) 의 정지 배치다.
 * 애니메이션이 켜져 있을 때의 t=0 위치와 같아야 「모션 끄면 구름이 한쪽에 뭉치는」 일이 없다.
 * 100% → -100% 이동이라 t=0 위치는 100 - 200 × (지연/주기) 다.
 */
test('구름 트랙의 정지 배치가 애니메이션 시작 위치와 같다', () => {
    const css = source(HEADER_CSS);

    for (const n of [1, 2, 3, 4]) {
        const block = rule(css, `.home-hero__cloud-track--${n} {`);
        const rest = Number(block.match(/transform:\s*translateX\((-?[\d.]+)%\)/)[1]);
        const duration = Number(block.match(/animation-duration:\s*([\d.]+)s/)[1]);
        const delay = Number(block.match(/animation-delay:\s*(-[\d.]+)s/)[1]);

        const atZero = 100 - 200 * (-delay / duration);
        assert.ok(
            Math.abs(atZero - rest) <= 1,
            `트랙 ${n}: 정지 배치 ${rest}% 와 시작 위치 ${atZero.toFixed(1)}% 가 어긋난다`,
        );
    }
});

/*
 * 실제로 겪은 버그 — 낙하 잎의 top 이 134~160px 이었는데, 이건 어느 나무의
 * 잎갈래보다도 50~75px 위다. 그래서 빈 하늘에 잎 조각이 툭 나타났다.
 * 잎은 나무에서 떨어지는 것이지 하늘에서 생겨나는 게 아니다.
 *
 * 지금 스프라이트 기준 잎갈래 위쪽 끝은 tree-lg 가 y 209px 로 가장 높다(stage 500px 기준).
 * 낙하는 -20px 에서 시작하므로 top 이 229px 보다 위면 반드시 하늘에서 시작한다.
 * 소품 폭을 바꾸면 높이도 같이 변하니 그때 이 값을 다시 계산한다.
 */
test('낙하 잎은 하늘이 아니라 잎갈래 안에서 시작한다', () => {
    const css = source(HEADER_CSS);
    const CANOPY_TOP = 229;

    for (const n of [1, 2, 3, 4, 5]) {
        const top = Number(rule(css, `.home-hero__fall--${n} {`).match(/top:\s*(\d+)px;/)[1]);
        assert.ok(
            top >= CANOPY_TOP,
            `잎 ${n} 의 top ${top}px 이 잎갈래(${CANOPY_TOP}px)보다 위다 — 빈 하늘에서 나타난다`,
        );
    }
});

/*
 * 잎은 잔디(stage y 396px)에 닿으며 사라져야 한다. 낙하 거리가 너무 길면
 * 잔디를 지나쳐 본문 경계까지 내려가 「땅에 닿는」 느낌이 사라진다.
 */
test('낙하 잎이 잔디를 크게 지나치지 않는다', () => {
    const css = source(HEADER_CSS);
    const FIELD_TOP = 396;

    for (const [keyframe, name] of [
        ['@keyframes tt-leaf-fall-a', 'a'],
        ['@keyframes tt-leaf-fall-b', 'b'],
    ]) {
        const body = css.slice(css.indexOf(keyframe));
        const drops = [
            ...body.slice(0, body.indexOf('\n}')).matchAll(/translate\([^,]+,\s*(-?\d+)px\)/g),
        ];
        const travel = Math.max(...drops.map((m) => Number(m[1])));

        for (const n of [1, 2, 3, 4, 5]) {
            const fall = rule(css, `.home-hero__fall--${n} {`);
            if (!fall.includes(`tt-leaf-fall-${name}`)) continue;

            const land = Number(fall.match(/top:\s*(\d+)px;/)[1]) + travel;
            assert.ok(
                land >= FIELD_TOP - 24 && land <= FIELD_TOP + 60,
                `잎 ${n} 이 y ${land}px 에서 사라진다 — 잔디(${FIELD_TOP}px) 근처여야 한다`,
            );
        }
    }
});

/*
 * transform 은 요소당 하나뿐이라 왕복(pace)과 걸음(step)을 한 요소에 같이 걸면
 * 뒤엣것이 앞엣것을 덮어쓴다. 겹이 합쳐지지 않았는지 본다.
 */
test('탕이의 왕복과 걸음은 서로 다른 겹에 걸려 있다', () => {
    const css = source(HEADER_CSS);

    assert.match(rule(css, '.home-hero__walk-pace {'), /animation:\s*tt-tangi-pace/);
    assert.match(rule(css, '.home-hero__tangi {'), /animation:\s*tt-tangi-step/);
    assert.doesNotMatch(
        rule(css, '.home-hero__walk-pace {'),
        /tt-tangi-step/,
        '한 요소에 두 애니메이션을 걸면 왕복이 걸음에 먹힌다',
    );

    assert.match(
        source(HEADER_VUE),
        /home-hero__walk"[\s\S]*?home-hero__walk-pace"[\s\S]*?home-hero__tangi"/,
        '__walk → __walk-pace → __tangi 순으로 감싸야 한다',
    );
});

/*
 * 실제로 겪은 버그 — base.css 의 전역 `img { max-width: 100% }` 에서 100% 는
 * 컨테이닝 블록(__walk-pace) 의 폭인데 그게 0 이다. 그대로 두면 max-width:0 이 돼
 * 탕이만 통째로 사라지고 발밑 그림자(span)만 남는다. 눈으로만 잡히는 종류라 여기서 묶는다.
 */
test('탕이는 전역 img 리셋의 max-width 를 풀어 둔다', () => {
    assert.match(
        rule(source(HEADER_CSS), '.home-hero__tangi {'),
        /max-width:\s*none;/,
        '폭 0 인 상자 안의 img 라 max-width:100% 가 걸리면 0px 로 찌부러진다',
    );
});

/* 발이 땅에 닿는 순간이 맞으려면 걸음과 그림자의 주기가 같아야 한다 */
test('탕이 걸음과 발밑 그림자의 주기가 같다', () => {
    const css = source(HEADER_CSS);
    const step = rule(css, '.home-hero__tangi {').match(/animation:[^;]*?([\d.]+)s/)[1];
    const shadow = rule(css, '.home-hero__walk-shadow {').match(/animation:[^;]*?([\d.]+)s/)[1];

    assert.equal(step, shadow, `걸음 ${step}s 와 그림자 ${shadow}s 가 어긋나면 발이 떠 보인다`);
});

/*
 * 잔디 페이드는 소품보다 앞, 탕이보다는 뒤여야 한다.
 * 탕이보다 앞에 오면 발이 지워진다.
 */
test('잔디 페이드는 소품 뒤 · 탕이 앞에 온다', () => {
    const vue = source(HEADER_VUE);
    const at = (mark) => vue.indexOf(mark);

    assert.ok(at('home-hero__leaf--5') < at('home-hero__fade'), '잔디 위 잎이 페이드에 안 덮인다');
    assert.ok(at('home-hero__fade') < at('home-hero__walk'), '페이드가 탕이 발을 지운다');
});

/*
 * 목업의 `9:41` 줄은 가짜 크롬이라 그리지 않는다. 대신 설치형에서 시계가 올라앉는
 * 자리를 하늘 맨 위 색으로 채운다 — 안 채우면 그 띠만 흰색으로 뜬다.
 */
test('설치형 상태바 자리를 하늘색으로 채운다', () => {
    const statusbar = rule(source(HEADER_CSS), '.home-hero__statusbar {');

    assert.match(statusbar, /height:\s*env\(safe-area-inset-top,\s*0px\);/);
    assert.match(statusbar, /background:\s*var\(--tt-home-hero-sky-top\);/);
    assert.doesNotMatch(template(source(HEADER_VUE)), /9:41/, '목업의 가짜 시계는 그리지 않는다');
});

test('헤더는 색 HEX 를 직접 쓰지 않는다', () => {
    for (const path of [HEADER_VUE, HEADER_CSS]) {
        const offenders = source(path).match(/#[0-9a-fA-F]{3,8}\b/g) ?? [];
        assert.deepEqual(offenders, [], `${path} 는 tokens.css 의 토큰만 참조한다`);
    }
});
