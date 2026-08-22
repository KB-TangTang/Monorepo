/**
 * 사용자 표시명·닉네임 판정과 **온보딩 게이트 판정** (DECISIONS.md 2026-08-11).
 *
 * 표시명 규칙과 닉네임 검증을 화면마다 따로 쓰면 프로필 카드·설정 시트·온보딩이
 * 각자 다른 이름과 다른 오류 문구를 보여준다. 판정은 전부 여기 한 곳에 둔다.
 * 온보딩 단계 판정도 같은 이유로 여기 모은다 — 라우터 가드는 결과만 받아 이동한다.
 *
 * ⚠ **이 파일에는 `@/` 별칭 import 를 쓰지 않는다.** 별칭은 Vite 가 푸는 것이라
 * `node --test` 는 해석하지 못해 테스트 파일이 통째로 죽는다. (utils/account.js 와 같은 이유)
 */
import { LINK_STEP_ROUTES } from './account.js';

/**
 * 닉네임 길이 한계.
 * 상한 50 은 저장 대상 컬럼이 `tbl_user.nickname VARCHAR(50)` 이기 때문이다 —
 * 서버 검증(1~50자)과 같은 값이라야 화면은 통과했는데 서버가 INVALID_REQUEST 를
 * 돌려주는 어긋남이 안 생긴다.
 */
export const NICKNAME_MIN_LENGTH = 1;
export const NICKNAME_MAX_LENGTH = 50;

/** 닉네임 설정 화면의 라우트 이름. 가드·완료 화면·이 파일이 같은 값을 봐야 한다. */
export const NICKNAME_SETUP_ROUTE = 'nicknameSetup';

/**
 * 화면에 보여줄 이름. 규칙은 `nickname ?? socialName` 이다.
 *
 * 서버가 이미 `displayName` 으로 계산해 내려주므로 그 값을 가장 먼저 쓴다.
 * 다만 그 필드가 없는 응답(닉네임만 부분 갱신한 직후 등)도 있어 같은 규칙을 여기서 한 번 더 편다.
 * 셋 다 없으면 **빈 문자열**이다 — 대체 문구('이름 없음')는 화면이 정한다.
 * 여기서 대체 문구를 돌려주면 아바타 이니셜이 '이' 가 돼버린다.
 */
export function resolveDisplayName(user) {
    const found = [user?.displayName, user?.nickname, user?.socialName].find(
        (value) => typeof value === 'string' && value.trim() !== '',
    );
    return found ? found.trim() : '';
}

/** 아이디 앞에 남길 글자 수. 어느 계정인지는 알아보되 전체가 드러나지 않을 만큼만 남긴다. */
const EMAIL_VISIBLE_LENGTH = 3;

/**
 * 화면에 보여줄 이메일. 아이디 앞 3자만 남기고 가린다. 도메인은 그대로 둔다(이슈 #328).
 *
 * 마이페이지는 본인만 보는 화면이지만 시연영상·발표자료·스크린샷에 계속 등장하는 자리다.
 * 제출물에 개인정보를 담지 않기 위해 화면 단계에서 가린다.
 *
 * 도메인을 남기는 이유는 **소셜 계정을 여러 개 쓸 때 어느 계정인지 구분**해야 해서다.
 * 소셜 로그인이라 이메일이 사실상 유일한 식별 정보고, 전부 가리면 그 수단이 사라진다.
 *
 * 짧은 아이디도 안전해야 한다 - 남기는 글자 수가 아이디 길이를 넘지 않게 최소 1자는 가린다.
 *   twinsjh01@gmail.com → twi******@gmail.com
 *   ab@naver.com        → a*@naver.com
 *
 * @returns 가린 이메일. 값이 없거나 `@` 가 없으면 빈 문자열이다 - 이메일이 아닌 값을
 *          그대로 화면에 흘리지 않는다.
 */
export function maskEmail(email) {
    const value = String(email ?? '').trim();
    const at = value.lastIndexOf('@');
    if (at <= 0 || at === value.length - 1) {
        return '';
    }

    const local = value.slice(0, at);
    const domain = value.slice(at);
    const visible = Math.min(EMAIL_VISIBLE_LENGTH, Math.max(1, local.length - 1));

    return `${local.slice(0, visible)}${'*'.repeat(local.length - visible)}${domain}`;
}

/**
 * 닉네임 입력 검증.
 *
 * **중복은 보지 않는다.** 중복을 허용하기로 했고 서버도 검사하지 않는다
 * (DECISIONS.md 2026-08-11). 여기서 볼 수 있는 것은 길이뿐이다.
 *
 * @returns {{valid: boolean, value: string, error: string}} value 는 서버로 보낼 trim 된 값
 */
export function validateNickname(value) {
    const trimmed = String(value ?? '').trim();
    if (trimmed.length < NICKNAME_MIN_LENGTH) {
        return { valid: false, value: trimmed, error: '닉네임을 입력해주세요' };
    }
    if (trimmed.length > NICKNAME_MAX_LENGTH) {
        return {
            valid: false,
            value: trimmed,
            error: `닉네임을 ${NICKNAME_MIN_LENGTH}~${NICKNAME_MAX_LENGTH}자로 입력해주세요`,
        };
    }
    return { valid: true, value: trimmed, error: '' };
}

/**
 * 온보딩 단계 정의 (DECISIONS.md 2026-08-11 (7)).
 *
 * 동선은 `로그인 → 서비스동의 → 금융동의 → 계좌연동 → 닉네임 설정 → 홈` 이고 **계좌 연동까지 강제**한다.
 * 예전에는 서비스 동의 뒤 바로 홈으로 보내 금융동의(THIRD_PARTY, 제3자 제공) 화면에 도달할 길이
 * 아예 없었다 — 아무도 동의하지 않은 채 계좌가 연동됐다. 그래서 순서를 여기 한 줄로 못박는다.
 *
 * **배열 순서가 곧 판정 순서다.** 앞 단계가 남아 있으면 뒤 단계는 보지 않는다.
 *
 * - `pending` — 그 단계가 아직 안 끝났는지. 앞의 셋은 서버가 내려주는 플래그
 *   (`POST /auth/refresh` · `POST /consents` 응답)이고, 마지막은 `nickname` 이 비었는지로만 본다.
 * - `route`   — 그 단계로 보낼 라우트 이름.
 * - `matches` — 라우트 이름 외에 "그 단계의 화면"으로 쳐야 하는 라우트 판별(면제 계산에만 쓴다).
 */
const ONBOARDING_STEPS = [
    {
        route: 'consent',
        pending: (session) => Boolean(session.needsConsent),
    },
    {
        route: 'financialConsent',
        pending: (session) => Boolean(session.needsFinancialConsent),
    },
    {
        /* 계좌 연동 1단계(기관 선택)만 라우트 이름이 있다. 2~5단계는 meta.linkStep 으로 판별한다. */
        route: LINK_STEP_ROUTES.institutions,
        pending: (session) => Boolean(session.needsAccountLink),
        matches: (to) => Boolean(to?.meta?.linkStep),
    },
    {
        route: NICKNAME_SETUP_ROUTE,
        pending: (session) => !session.user?.nickname,
    },
];

/**
 * `to` 가 `index` 번째 단계 **이하**의 화면인지. 면제 규칙은 이것 하나가 전부다.
 *
 * ⚠ 각 단계는 **자기 화면과 그 앞 단계 화면**에서는 발동하지 않는다.
 *   - 자기 화면을 면제하지 않으면 그 화면에 들어가는 순간 자기 자신으로 다시 보내 무한 리다이렉트가 난다.
 *   - 앞 단계 화면을 면제하지 않으면, 계좌를 연동하던 사용자가 닉네임 화면으로 튕겨나가 연동을 끝내지 못한다.
 *
 * 반대로 **뒤 단계 화면은 면제하지 않는다.** 그래야 앞 단계를 건너뛴 채 뒤 화면에 들어올 수 없다.
 */
function isAtOrBeforeStep(to, index) {
    return ONBOARDING_STEPS.slice(0, index + 1).some(
        (step) => step.route === to?.name || step.matches?.(to) === true,
    );
}

/**
 * 온보딩 게이트. 지금 보내야 할 라우트 이름을 돌려준다. 보낼 곳이 없으면 `null`.
 * 라우터 가드가 이 결과만 보고 이동한다 — 가드 본문에 조건을 다시 쓰지 말 것.
 *
 * ⚠ `session.user` 가 아직 없으면 **전부 통과시킨다.** 부팅 직후·로그인 직후에는 사용자 정보가
 * 잠깐 비는데, 없는 값을 "닉네임 미설정"으로 읽으면 엉뚱한 순간에 온보딩 화면으로 튕긴다.
 * 게이트 플래그 셋도 같은 순간에는 기본값(false)이라 판정할 근거 자체가 없다.
 *
 * 스토어·라우터를 직접 읽지 않고 값만 받는 순수 함수라 마운트 없이 검증할 수 있다.
 *
 * @param {{user?: object|null, needsConsent?: boolean, needsFinancialConsent?: boolean,
 *          needsAccountLink?: boolean}} session 인증 스토어(또는 같은 모양의 객체)
 * @param {{name?: string, meta?: {linkStep?: string}}} to 이동하려는 라우트
 * @returns {string|null} 보낼 라우트 이름 · 통과면 null
 */
export function resolveOnboardingRedirect(session, to) {
    if (!session?.user) {
        return null;
    }
    for (let index = 0; index < ONBOARDING_STEPS.length; index += 1) {
        const step = ONBOARDING_STEPS[index];
        if (step.pending(session) && !isAtOrBeforeStep(to, index)) {
            return step.route;
        }
    }
    return null;
}

/**
 * 닉네임 설정 화면으로 보내야 하는지. **온보딩 4단계 중 닉네임만** 떼어 본다.
 *
 * 판정 자체는 resolveOnboardingRedirect() 로 흡수했다 — 게이트가 넷으로 늘어난 마당에
 * 면제 규칙이 두 벌 존재하면 언젠가 반드시 어긋난다. 앞 단계 플래그를 넘기지 않으므로
 * 여기서는 닉네임 단계만 발동할 수 있다.
 *
 * 온보딩 완료 여부는 **`nickname` 이 비어 있는지로만** 판별한다 — 별도 판별 API 는 두지 않는다
 * (DECISIONS.md 2026-08-11). 빈 문자열·null·undefined 를 모두 미설정으로 본다.
 *
 * @param {object|null} user 인증 스토어의 사용자 정보
 * @param {{name?: string, meta?: {linkStep?: string}}} to 이동하려는 라우트
 */
export function needsNicknameSetup(user, to) {
    return resolveOnboardingRedirect({ user }, to) === NICKNAME_SETUP_ROUTE;
}
