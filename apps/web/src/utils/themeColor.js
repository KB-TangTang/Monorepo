/*
 * 설치형(PWA) 상태바 색.
 *
 * 안드로이드 standalone 에서 상태바(시계·배터리 줄)는 시스템 영역이라 웹 화면이
 * 그 위에 직접 못 그린다. 그 띠의 배경색을 정하는 건 <meta name="theme-color"> 하나뿐이다.
 * 그래서 화면 맨 위 색과 이 값을 맞춰야 띠가 화면에 이어져 보인다.
 *
 * 값을 하나로 고정하지 않는 이유 — 화면 63개 중 57개가 밝은 배경(--tt-neutral-paper)인데
 * 로그인·재판·그룹챌린지 세 화면만 상단이 잉크 패널이다. 하나로 박으면 어느 쪽이든
 * 나머지에서 상태바만 다른 색 띠로 떠 보인다.
 */

/** --tt-neutral-paper. 대부분의 화면이 쓰는 배경 */
export const THEME_COLOR_PAPER = '#f7f8fa';

/** --tt-ink-deep. 잉크 패널 그라데이션의 시작색(제일 위) */
export const THEME_COLOR_INK = '#1e2338';

/*
 * 상단에 잉크 패널이 깔리는 라우트.
 *
 * 판단 기준은 「화면 맨 위 픽셀이 어두운가」다. 헤더가 어디 있느냐가 아니다.
 *   login                     LoginView 의 .login__hero (ink-glow → ink-deep)
 *   personalMissionChallenge  PersonalCourtHeader.css 의 잉크 그라데이션
 *   groupChallenge            GroupChallengeHomeView 의 잉크 그라데이션
 *
 * 잉크 헤더를 쓰는 화면을 새로 만들면 여기에 라우트 이름을 추가한다.
 * 빠뜨리면 그 화면에서만 상태바가 흰 띠로 뜬다.
 */
export const INK_TOP_ROUTES = Object.freeze([
    'login',
    'personalMissionChallenge',
    'groupChallenge',
]);

/**
 * 라우트 이름으로 상태바 색을 고른다.
 * @param {string|symbol|null|undefined} routeName 라우트의 name
 * @returns {string} HEX 색상
 */
export function resolveThemeColor(routeName) {
    return INK_TOP_ROUTES.includes(routeName) ? THEME_COLOR_INK : THEME_COLOR_PAPER;
}

/**
 * <meta name="theme-color"> 의 값을 바꾼다. 태그가 없으면 만들어 붙인다.
 * @param {string} color HEX 색상
 * @param {Document} [doc] 테스트에서 문서를 갈아 끼우기 위한 인자
 * @returns {HTMLMetaElement|null} 갱신된 meta 태그. 문서가 없으면 null
 */
export function applyThemeColor(color, doc = typeof document === 'undefined' ? null : document) {
    if (!doc) return null;
    let meta = doc.querySelector('meta[name="theme-color"]');
    if (!meta) {
        meta = doc.createElement('meta');
        meta.setAttribute('name', 'theme-color');
        doc.head.appendChild(meta);
    }
    meta.setAttribute('content', color);
    return meta;
}
