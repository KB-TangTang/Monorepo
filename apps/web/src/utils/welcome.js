/*
 * 서비스 소개 온보딩 (이슈 #459)
 *
 * 앱을 처음 여는 사용자에게 로그인보다 **먼저** 뜨는 5면짜리 소개 화면이다.
 * 한 번 보면 다시 뜨지 않는다.
 *
 * 「봤다」를 localStorage 에 두는 이유 — 이 화면은 로그인 **전**이라 기록을 붙일 사용자가
 * 아직 없다. 개인·그룹 튜토리얼처럼 `tutorial_seen_at` 서버 컬럼에 남기는 방식을 쓸 수 없다.
 * 기기를 바꾸거나 캐시를 지우면 다시 뜨는데, 소개 화면이라 그래도 손해가 없다.
 *
 * 이미지를 여기서 import 하지 않는다 — 이 파일은 node:test 가 그대로 불러 읽는다.
 * 테스트 로더(loader.mjs)는 `@/` 별칭만 풀고 png 는 다루지 못해서, 에셋을 넣는 순간
 * 이 파일에 딸린 테스트가 전부 죽는다. 레이어는 **이름만** 적고 실제 png 매핑은
 * ServiceIntroSlide.vue 가 한다.
 */

/** localStorage 키. 값은 '1' 하나만 쓴다 */
export const WELCOME_SEEN_KEY = 'tt.welcomeSeen';

/*
 * localStorage 에 손대는 걸 전부 try/catch 로 감싼다.
 * Safari 프라이빗 모드·쿠키 차단 설정에서는 `localStorage` 를 **읽는 것만으로도** 던진다
 * (속성 접근 자체가 SecurityError). 막히면 「아직 안 봤다」로 떨어뜨린다 —
 * 온보딩이 한 번 더 뜨는 건 참을 수 있지만 첫 화면이 죽는 건 안 된다.
 */
function defaultStorage() {
    try {
        return globalThis.localStorage ?? null;
    } catch {
        return null;
    }
}

/**
 * 소개 온보딩을 이미 봤는지 확인한다.
 * @param {Storage|null} [storage] 테스트에서 저장소를 갈아 끼우기 위한 인자
 * @returns {boolean} 저장소를 못 쓰면 false (= 안 봄)
 */
export function hasSeenWelcome(storage = defaultStorage()) {
    try {
        return storage?.getItem(WELCOME_SEEN_KEY) === '1';
    } catch {
        return false;
    }
}

/**
 * 소개 온보딩을 봤다고 기록한다.
 * @param {Storage|null} [storage] 테스트에서 저장소를 갈아 끼우기 위한 인자
 * @returns {boolean} 기록에 성공했으면 true
 */
export function markWelcomeSeen(storage = defaultStorage()) {
    try {
        storage?.setItem(WELCOME_SEEN_KEY, '1');
        return true;
    } catch {
        return false;
    }
}

/*
 * 5면의 내용.
 *
 * title 은 「줄 → 조각」 2차원 배열이다. 한 줄 안에서 일부 단어만 파란색이라
 * (`오늘의 소비를 / **챌린지**로 바꿔요`) 문자열 하나로는 표현이 안 된다.
 * 줄바꿈 위치는 디자인이 정한 것이라 화면 폭에 맡기지 않고 그대로 고정한다.
 *
 * layers 의 name 은 CSS 클래스 접미사(`--intro-tangi`)와 png 파일명을 겸한다.
 * 위치·크기는 전부 ServiceIntroView.css 에 있다 — 좌표를 데이터에 넣으면
 * 토큰을 못 쓰고 반응형 계산도 인라인 스타일로 새어 나간다.
 *
 * cta 는 **마지막 면에만** 있다. 그 면만 하단이 꽉 찬 Ink 버튼이고 나머지는
 * `이전 · 인디케이터 · 다음` 이다. 1면에도 「시작하기」를 뒀었는데 「다음」과 하는 일이
 * 같아 같은 화면에 같은 동작이 둘이 됐다. 전환 버튼은 마지막에만 나와야 눈에 띈다.
 */
export const SERVICE_INTRO_SLIDES = Object.freeze([
    {
        key: 'intro',
        title: [[{ text: '소비 습관을 심판하다' }], [{ text: '탕탕!', accent: true, hero: true }]],
        lead: ['현명한 소비 습관을 만들고', '나의 재판 결과를 확인해 보세요.'],
        layers: [
            { name: 'court', alt: '' },
            { name: 'tangi', alt: '판사봉을 든 판사 탕이' },
        ],
        sparkles: 2,
        cta: '',
    },
    {
        key: 'challenge',
        title: [
            [{ text: '오늘의 소비를' }],
            [{ text: '챌린지', accent: true }, { text: '로 바꿔요' }],
        ],
        lead: ['매일 새로운 챌린지에 참여하고', '판결을 받아 보세요.'],
        pill: '배달비 줄이기 · 15,000원 목표',
        layers: [
            { name: 'card', alt: '오늘의 챌린지 카드' },
            { name: 'books', alt: '' },
            { name: 'tangi', alt: '챌린지를 안내하는 탕이' },
        ],
        sparkles: 0,
        cta: '',
    },
    {
        key: 'group',
        title: [
            [{ text: '함께 ' }, { text: '심판', accent: true }, { text: '하고' }],
            [{ text: '절약해요' }],
        ],
        lead: ['그룹 챌린지에서 다른 사람들과', '의견을 나누고 보상을 얻어요.'],
        layers: [
            { name: 'crowd', alt: '' },
            { name: 'bubble-pink', alt: '' },
            { name: 'bubble-blue', alt: '' },
            { name: 'tangi', alt: '그룹 심판을 안내하는 탕이' },
        ],
        sparkles: 0,
        cta: '',
    },
    {
        key: 'asset',
        title: [
            [{ text: '내 ' }, { text: '자산', accent: true }, { text: '을 한눈에' }],
            [{ text: '관리해요' }],
        ],
        lead: ['계좌, 카드, 소비 내역까지', '쉽고 안전하게 관리해요.'],
        layers: [
            { name: 'summary', alt: '총 자산 12,450,000원' },
            { name: 'card', alt: '' },
            { name: 'bank', alt: '' },
            { name: 'chart', alt: '' },
            { name: 'tangi', alt: '자산을 살펴보는 탕이' },
        ],
        sparkles: 0,
        cta: '',
    },
    {
        key: 'outro',
        title: [
            [{ text: '소비가 바뀌면' }],
            [{ text: '내일', accent: true }, { text: '이 바뀌어요' }],
        ],
        lead: ['탕탕과 함께 더 나은 내일을', '만들어 보세요!'],
        layers: [
            { name: 'chart', alt: '' },
            { name: 'tangi', alt: '엄지를 든 탕이' },
        ],
        sparkles: 1,
        cta: '탕탕 시작하기',
    },
]);

/** 면 개수. 인디케이터·경계 계산이 전부 이 값을 본다 */
export const SERVICE_INTRO_COUNT = SERVICE_INTRO_SLIDES.length;

/**
 * 슬라이드 번호를 유효 범위로 자른다.
 *
 * 스와이프는 끝에서 한 번 더 당겨지고, 애니메이션 도중 버튼이 연타되면 범위를 넘는다.
 * 넘긴 값을 그대로 쓰면 빈 화면이 뜨므로 들어오는 길목마다 이걸 통과시킨다.
 *
 * @param {number} index 자를 값
 * @returns {number} 0 이상 SERVICE_INTRO_COUNT-1 이하의 정수. 숫자가 아니면 0
 */
export function clampSlideIndex(index) {
    if (!Number.isFinite(index)) {
        return 0;
    }
    return Math.min(Math.max(Math.trunc(index), 0), SERVICE_INTRO_COUNT - 1);
}

/**
 * 마지막 면인지 판정한다. 이 면의 버튼만 로그인으로 나간다.
 * @param {number} index 슬라이드 번호
 * @returns {boolean}
 */
export function isLastSlide(index) {
    return clampSlideIndex(index) === SERVICE_INTRO_COUNT - 1;
}
