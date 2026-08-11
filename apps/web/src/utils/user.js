/**
 * 사용자 표시명·닉네임 판정 (DECISIONS.md 2026-08-11 「닉네임 온보딩 신설」).
 *
 * 표시명 규칙과 닉네임 검증을 화면마다 따로 쓰면 프로필 카드·설정 시트·온보딩이
 * 각자 다른 이름과 다른 오류 문구를 보여준다. 판정은 전부 여기 한 곳에 둔다.
 *
 * ⚠ **이 파일에는 `@/` 별칭 import 를 쓰지 않는다.** 별칭은 Vite 가 푸는 것이라
 * `node --test` 는 해석하지 못해 테스트 파일이 통째로 죽는다. (utils/account.js 와 같은 이유)
 */

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
 * 닉네임 게이트를 면제할 라우트.
 *
 * 온보딩 동선은 `로그인 → 서비스동의 → 금융동의 → 계좌연동 → 닉네임 설정 → 홈` 이다.
 * 닉네임보다 **앞에 있는 단계**를 면제하지 않으면, 계좌를 연동하던 사용자가 닉네임 화면으로
 * 튕겨나가 연동을 끝내지 못한다. 닉네임 화면 자신을 빼면 무한 리다이렉트가 난다.
 *
 * 계좌 연동 플로우의 2~5단계는 `meta.linkStep` 으로 판별하므로 여기에는 그것이 없는
 * 첫 단계(기관 선택)만 적는다.
 */
const NICKNAME_GATE_EXEMPT_ROUTES = [
    NICKNAME_SETUP_ROUTE,
    'consent',
    'financialConsent',
    'accountLinkInstitutions',
];

/**
 * 닉네임 설정 화면으로 보내야 하는지. 라우터 가드가 쓴다.
 *
 * 온보딩 완료 여부는 **`nickname` 이 비어 있는지로만** 판별한다 — 별도 판별 API 는 두지 않는다
 * (DECISIONS.md 2026-08-11). 빈 문자열·null·undefined 를 모두 미설정으로 본다.
 *
 * ⚠ `user` 가 아직 없으면 **통과시킨다.** 부팅 직후·로그인 직후에는 사용자 정보가 잠깐 비는데,
 * 없는 값을 "닉네임 미설정"으로 읽으면 엉뚱한 순간에 온보딩 화면으로 튕긴다.
 *
 * 스토어·라우터를 직접 읽지 않고 값만 받는 순수 함수라 마운트 없이 검증할 수 있다.
 *
 * @param {object|null} user 인증 스토어의 사용자 정보
 * @param {{name?: string, meta?: {linkStep?: string}}} to 이동하려는 라우트
 */
export function needsNicknameSetup(user, to) {
    if (!user || user.nickname) {
        return false;
    }
    if (to?.meta?.linkStep) {
        return false;
    }
    return !NICKNAME_GATE_EXEMPT_ROUTES.includes(to?.name);
}
