import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

import {
    SERVICE_INTRO_COUNT,
    SERVICE_INTRO_SLIDES,
    WELCOME_SEEN_KEY,
    clampSlideIndex,
    hasSeenWelcome,
    isLastSlide,
    markWelcomeSeen,
} from '../src/utils/welcome.js';

/*
 * 서비스 소개 온보딩 (이슈 #459).
 *
 * 이 프로젝트에는 컴포넌트 렌더링 하네스가 없다(node:test + 순수 JS). 그래서
 * 판정 로직은 직접 부르고, DOM·에셋이 얽힌 부분은 소스를 읽어 검사한다
 * (themeColor.test.js 가 쓰는 것과 같은 방식).
 */

function source(path) {
    return readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
}

/** localStorage 대역. 실제 브라우저 저장소 없이 읽고 쓴 걸 확인한다 */
function fakeStorage(initial = {}) {
    const map = new Map(Object.entries(initial));
    return {
        getItem: (key) => (map.has(key) ? map.get(key) : null),
        setItem: (key, value) => map.set(key, String(value)),
    };
}

/** 접근하는 것만으로 던지는 저장소 (Safari 프라이빗 모드 / 쿠키 차단) */
function throwingStorage() {
    return {
        getItem() {
            throw new Error('SecurityError');
        },
        setItem() {
            throw new Error('SecurityError');
        },
    };
}

/* ── 열람 플래그 ─────────────────────────────────── */

test('기록이 없으면 아직 안 본 것으로 본다', () => {
    assert.equal(hasSeenWelcome(fakeStorage()), false);
});

test('봤다고 기록하면 그 뒤로는 봤다고 답한다', () => {
    const storage = fakeStorage();

    assert.equal(markWelcomeSeen(storage), true);
    assert.equal(storage.getItem(WELCOME_SEEN_KEY), '1');
    assert.equal(hasSeenWelcome(storage), true);
});

/*
 * 값이 '1' 일 때만 「봤다」로 친다. 다른 코드가 같은 키를 다른 값으로 쓰거나
 * 사용자가 콘솔에서 손대도 조용히 통과하지 않게 한다.
 */
test('저장된 값이 1 이 아니면 안 본 것으로 친다', () => {
    for (const value of ['0', 'true', '', 'null']) {
        const storage = fakeStorage({ [WELCOME_SEEN_KEY]: value });
        assert.equal(hasSeenWelcome(storage), false, `${value} 는 봤다로 치면 안 된다`);
    }
});

/*
 * 프라이빗 모드에서 저장소가 던져도 화면이 죽으면 안 된다.
 * 읽기는 「안 봄」으로 떨어져 온보딩이 한 번 더 뜨고, 쓰기는 실패를 알리되 예외는 안 낸다.
 */
test('저장소가 막혀 있어도 예외를 밖으로 내보내지 않는다', () => {
    const storage = throwingStorage();

    assert.equal(hasSeenWelcome(storage), false);
    assert.equal(markWelcomeSeen(storage), false);
});

test('저장소가 아예 없어도(null) 크래시하지 않는다', () => {
    assert.equal(hasSeenWelcome(null), false);
    assert.equal(markWelcomeSeen(null), true);
});

/* ── 슬라이드 번호 ───────────────────────────────── */

test('슬라이드 번호는 유효 범위 밖으로 나가지 않는다', () => {
    assert.equal(clampSlideIndex(-1), 0);
    assert.equal(clampSlideIndex(0), 0);
    assert.equal(clampSlideIndex(SERVICE_INTRO_COUNT - 1), SERVICE_INTRO_COUNT - 1);
    assert.equal(clampSlideIndex(SERVICE_INTRO_COUNT), SERVICE_INTRO_COUNT - 1);
    assert.equal(clampSlideIndex(999), SERVICE_INTRO_COUNT - 1);
});

test('숫자가 아닌 값은 첫 면으로 떨어진다', () => {
    for (const value of [NaN, Infinity, -Infinity, undefined, null, 'x']) {
        assert.equal(clampSlideIndex(value), 0, `${String(value)} 는 0 이어야 한다`);
    }
});

test('마지막 면 판정은 범위를 벗어난 값도 잘라서 본다', () => {
    assert.equal(isLastSlide(0), false);
    assert.equal(isLastSlide(SERVICE_INTRO_COUNT - 1), true);
    assert.equal(isLastSlide(999), true);
});

/* ── 슬라이드 정의 ───────────────────────────────── */

test('소개는 5면이고 면마다 키가 다르다', () => {
    assert.equal(SERVICE_INTRO_COUNT, 5);

    const keys = SERVICE_INTRO_SLIDES.map((slide) => slide.key);
    assert.deepEqual(keys, ['intro', 'challenge', 'group', 'asset', 'outro']);
    assert.equal(new Set(keys).size, keys.length, '키가 겹치면 CSS 좌표가 서로 덮인다');
});

/*
 * 하단 모양이 cta 유무로 갈린다(있으면 꽉 찬 Ink 버튼, 없으면 이전·다음).
 * 밖으로 나가는 버튼은 마지막 면에만 둔다 — 1면에도 뒀더니 「다음」과 하는 일이 같아
 * 한 화면에 같은 동작이 둘이 됐다.
 */
test('cta 가 있는 면은 마지막 면뿐이다', () => {
    const withCta = SERVICE_INTRO_SLIDES.map((slide) => Boolean(slide.cta));
    assert.deepEqual(withCta, [false, false, false, false, true]);
});

test('모든 면이 제목·설명·레이어를 갖는다', () => {
    for (const slide of SERVICE_INTRO_SLIDES) {
        assert.ok(slide.title.length > 0, `${slide.key} 에 제목이 없다`);
        assert.ok(slide.lead.length > 0, `${slide.key} 에 설명이 없다`);
        assert.ok(slide.layers.length > 0, `${slide.key} 에 일러스트가 없다`);

        for (const line of slide.title) {
            for (const part of line) {
                assert.equal(
                    typeof part.text,
                    'string',
                    `${slide.key} 제목 조각이 문자열이 아니다`,
                );
            }
        }
    }
});

/*
 * 장식용 이미지는 alt 를 빈 문자열로 둬야 스크린리더가 건너뛴다.
 * 빠뜨리면 파일명이 그대로 읽힌다. 면마다 앞에 세운 탕이 하나는 설명을 갖는다.
 */
test('면마다 탕이 레이어에만 대체 텍스트가 있다', () => {
    for (const slide of SERVICE_INTRO_SLIDES) {
        const described = slide.layers.filter((layer) => layer.alt !== '');
        assert.ok(described.length >= 1, `${slide.key} 에 설명 있는 이미지가 없다`);

        for (const layer of slide.layers) {
            assert.equal(typeof layer.alt, 'string', `${slide.key}/${layer.name} 에 alt 가 없다`);
        }
    }
});

/*
 * 레이어 이름은 세 곳에 나뉘어 있다 — 여기 데이터, ServiceIntroSlide.vue 의 png 대응표,
 * 그리고 같은 파일의 CSS 좌표. 하나만 고치면 이미지가 조용히 안 뜨거나 좌표 없이
 * 왼쪽 위에 쌓인다. 셋이 맞는지 소스로 확인한다.
 */
test('모든 레이어가 png 대응표와 CSS 좌표를 갖는다', () => {
    const slideComponent = source('src/components/onboarding/ServiceIntroSlide.vue');

    for (const slide of SERVICE_INTRO_SLIDES) {
        for (const layer of slide.layers) {
            const key = `${slide.key}-${layer.name}`;
            assert.ok(
                slideComponent.includes(`'${key}':`),
                `INTRO_IMAGES 에 '${key}' 가 없다 — 이미지가 안 뜬다`,
            );
            assert.ok(
                slideComponent.includes(`.intro-slide__layer--${key}`),
                `CSS 에 --${key} 좌표가 없다 — 왼쪽 위에 쌓인다`,
            );
        }

        for (let n = 1; n <= slide.sparkles; n += 1) {
            assert.ok(
                slideComponent.includes(`.intro-slide__sparkle--${slide.key}-${n}`),
                `CSS 에 반짝임 ${slide.key}-${n} 좌표가 없다`,
            );
        }
    }
});

/* ── 라우터 가드 ─────────────────────────────────── */

/*
 * 가드가 meta.public 검사보다 **위**에 있어야 한다. 막으려는 대상이 /login 인데
 * 그게 public 이라, 순서가 뒤집히면 온보딩이 영영 안 뜨고 아무도 눈치채지 못한다.
 */
test('온보딩 게이트가 public 검사보다 먼저 온다', () => {
    const router = source('src/router/index.js');

    const gate = router.indexOf('WELCOME_EXEMPT_ROUTES.has(to.name)');
    const publicCheck = router.indexOf('if (to.meta.public)');

    assert.ok(gate > 0, '온보딩 게이트가 가드에 없다');
    assert.ok(publicCheck > 0, 'public 검사가 가드에 없다');
    assert.ok(gate < publicCheck, '게이트가 public 검사보다 뒤에 있으면 /login 을 못 막는다');
});

/*
 * authCallback 을 빼먹으면 구글에서 돌아오는 길에 토큰이 아직 없어 「로그인 전」으로
 * 보이고, 그대로 /welcome 으로 튕겨 로그인이 통째로 깨진다. 재현이 어려운 사고라
 * 목록 자체를 고정해 둔다.
 */
test('온보딩 예외 목록에 welcome 과 authCallback 이 들어 있다', () => {
    const router = source('src/router/index.js');
    const match = router.match(/WELCOME_EXEMPT_ROUTES = new Set\(\[([^\]]*)\]\)/);

    assert.ok(match, '예외 목록을 찾지 못했다');
    assert.ok(match[1].includes("'welcome'"), 'welcome 이 빠지면 무한 리다이렉트가 돈다');
    assert.ok(match[1].includes("'authCallback'"), 'authCallback 이 빠지면 로그인이 깨진다');
});

test('/welcome 라우트는 로그인 없이 들어갈 수 있고 탭바를 숨긴다', () => {
    const router = source('src/router/index.js');
    const route = router.slice(router.indexOf("path: '/welcome'"));
    const meta = route.slice(route.indexOf('meta:'), route.indexOf('},', route.indexOf('meta:')));

    assert.ok(meta.includes('public: true'), '로그인 앞단 화면이라 public 이어야 한다');
    assert.ok(meta.includes('hideTabBar: true'), '로그인 전이라 하단 탭바가 뜨면 안 된다');
});

/*
 * utils/welcome.js 는 이 테스트가 그대로 import 한다. 테스트 로더(loader.mjs)는
 * `@/` 별칭만 풀고 png 는 다루지 못해서, 누가 여기에 이미지 import 를 넣는 순간
 * 위 테스트가 전부 죽는다. 죽고 나서 원인을 찾느니 이유와 함께 먼저 잡는다.
 */
test('utils/welcome.js 는 이미지를 import 하지 않는다', () => {
    const util = source('src/utils/welcome.js');
    assert.ok(
        !/^import .*\.(png|jpg|svg)/m.test(util),
        '에셋을 import 하면 node:test 로더가 못 읽어 이 파일의 테스트가 전부 깨진다',
    );
});
