/*
 * 설치형(PWA) 상태바 색.
 *
 * 안드로이드 standalone 에서 상태바(시계·배터리 줄)는 시스템 영역이라 웹 화면이
 * 그 위에 직접 못 그린다. 그 띠의 배경색을 정하는 건 <meta name="theme-color"> 하나뿐이다.
 * 그래서 화면 맨 위 색과 이 값을 맞춰야 띠가 화면에 이어져 보인다.
 *
 * 값을 하나로 고정하지 않는 이유 — 대부분의 화면이 밝은 배경(--tt-neutral-paper)인데
 * 로그인·재판·그룹챌린지 세 화면만 상단이 어두운 패널이다. 게다가 이 세 화면의 상단 색도
 * 서로 다르다. 하나로 박으면 어느 쪽이든 나머지에서 상태바만 다른 색 띠로 떠 보인다.
 */

/** --tt-neutral-paper. 대부분의 화면이 쓰는 배경 */
export const THEME_COLOR_PAPER = '#fafafb';

/** --tt-ink-deep. 잉크 패널 그라데이션의 시작색(제일 위) */
export const THEME_COLOR_INK = '#1e2338';

/** --tt-court-top-supreme. 개인챌린지 헤더의 대법원 건물 이미지 상단 색 */
export const THEME_COLOR_COURT_SUPREME = '#0d1e38';

/** --tt-court-top-district. 그룹챌린지 헤더의 지방법원 건물 이미지 상단 색 */
export const THEME_COLOR_COURT_DISTRICT = '#0c233f';

/*
 * 상단이 어두운 라우트와 그 색.
 *
 * 판단 기준은 「화면 맨 위 픽셀이 어두운가」다. 헤더가 어디 있느냐가 아니다.
 * 색이 라우트마다 다르므로 목록이 아니라 라우트 → 색 대응표다.
 *   login                     LoginView 의 .login__hero (ink-glow → ink-deep)
 *   personalMissionChallenge  ChallengeCourtHeader 의 대법원 건물 이미지 상단
 *   groupChallenge            ChallengeCourtHeader 의 지방법원 건물 이미지 상단
 *
 * 값은 tokens.css 의 --tt-court-top-* 와 같아야 한다(테스트가 잡는다).
 * 어두운 헤더를 쓰는 화면을 새로 만들면 여기에 추가한다.
 * 빠뜨리면 그 화면에서만 상태바가 흰 띠로 뜬다.
 *
 * 객체 리터럴이 아니라 Map 인 이유 — resolveThemeColor('constructor') 같은 이름이
 * Object.prototype 의 키를 맞아 색을 잘못 돌려주는 걸 막는다.
 */
const DARK_TOP_COLORS = new Map([
    ['login', THEME_COLOR_INK],
    ['personalMissionChallenge', THEME_COLOR_COURT_SUPREME],
    ['groupChallenge', THEME_COLOR_COURT_DISTRICT],
]);

/** 상단이 어두운 라우트 이름 목록. */
export const INK_TOP_ROUTES = Object.freeze([...DARK_TOP_COLORS.keys()]);

/**
 * 라우트 이름으로 상태바 색을 고른다.
 * @param {string|symbol|null|undefined} routeName 라우트의 name
 * @returns {string} HEX 색상
 */
export function resolveThemeColor(routeName) {
    return DARK_TOP_COLORS.get(routeName) ?? THEME_COLOR_PAPER;
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
