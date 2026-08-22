import { computed, ref } from 'vue';
import { defineStore } from 'pinia';

/**
 * 인증 상태는 여기 한 곳에서만 관리한다. (apps/web/AGENTS.md 규칙)
 *
 * accessToken 은 메모리에만 둔다. localStorage 에 넣지 않는다 —
 * XSS 한 번에 토큰이 통째로 털리기 때문이다.
 * 새로고침하면 사라지지만, main.js 부팅 시퀀스가 httpOnly 쿠키로
 * 재발급받아 복원한다.
 */
/**
 * 로그인 후 돌아갈 경로를 잠시 맡겨두는 sessionStorage 키.
 *
 * 구글 로그인은 SPA 라우팅이 아니라 전체 페이지 이동이라 라우터 상태(?redirect=...)가
 * 통째로 날아간다. 그래서 이동 직전에 여기 저장하고, 콜백 착지 화면이 꺼내 쓴다.
 */
export const POST_LOGIN_REDIRECT_KEY = 'tt.postLoginRedirect';

/**
 * 로그인 후 돌아갈 경로로 안전한 값인지 판정한다.
 *
 * '/' 로 시작하는 것만으로는 부족하다 — '//evil.com' 은 '/' 로 시작하지만
 * 브라우저가 프로토콜 상대 URL 로 보고 외부 도메인으로 해석한다.
 * '/\evil.com' 도 일부 브라우저가 '//' 로 정규화하므로 함께 막는다.
 */
export function isSafeRedirectPath(value) {
    return (
        typeof value === 'string' &&
        value.startsWith('/') &&
        !value.startsWith('//') &&
        !value.startsWith('/\\')
    );
}

export const useAuthStore = defineStore('auth', () => {
    const accessToken = ref('');
    const user = ref(null);
    /*
     * ── 온보딩 게이트 3단 ──────────────────────────────────────────────
     * 셋 다 **완전히 같은 취급**이다. 서버가 `POST /auth/refresh` · `POST /consents` 응답의
     * 같은 자리에 실어 보내고, 라우터 가드가 순서대로 읽어 다음 화면을 정한다.
     * 순서는 `needsConsent → needsFinancialConsent → needsAccountLink → 닉네임` 이고,
     * 그 순서와 면제 규칙은 utils/user.js 의 resolveOnboardingRedirect() 한 곳에만 있다.
     * (닉네임은 플래그가 아니라 user.nickname 이 비었는지로 판별한다 — DECISIONS.md 2026-08-11)
     */

    /** 필수 동의(SIGNUP 그룹)를 아직 마치지 않은 사용자인지. 저장·철회 응답으로도 갱신된다(stores/consent.js). */
    const needsConsent = ref(false);
    /** 제3자 제공 동의(FINANCIAL 그룹의 THIRD_PARTY)를 아직 마치지 않았는지. 계좌 연동의 법적 전제다. */
    const needsFinancialConsent = ref(false);
    /** 활성 연결 계좌가 0개인지. 온보딩에서 계좌 연동을 강제한다(DECISIONS.md 2026-08-11 (7)). */
    const needsAccountLink = ref(false);

    const isLoggedIn = computed(() => Boolean(accessToken.value));

    function setSession(session) {
        accessToken.value = session.accessToken ?? '';
        user.value = session.user ?? null;
        needsConsent.value = Boolean(session.needsConsent);
        needsFinancialConsent.value = Boolean(session.needsFinancialConsent);
        needsAccountLink.value = Boolean(session.needsAccountLink);
    }

    /**
     * 온보딩 게이트 플래그만 부분 갱신한다.
     *
     * 동의 저장(POST /consents)·계좌 연결 저장처럼 **세션 전체가 아니라 일부만** 돌아오는 응답용이다.
     * ⚠ 응답에 없는 플래그는 건드리지 않는다 — `undefined` 를 `false` 로 읽으면 아직 끝나지 않은
     * 단계를 끝난 것으로 착각해 게이트가 통째로 뚫린다(철회 응답은 needsConsent 만 싣는다).
     */
    function applyOnboardingFlags(flags) {
        if (flags?.needsConsent !== undefined) {
            needsConsent.value = Boolean(flags.needsConsent);
        }
        if (flags?.needsFinancialConsent !== undefined) {
            needsFinancialConsent.value = Boolean(flags.needsFinancialConsent);
        }
        if (flags?.needsAccountLink !== undefined) {
            needsAccountLink.value = Boolean(flags.needsAccountLink);
        }
    }

    /**
     * 사용자 정보 부분 갱신.
     *
     * 이름 저장(PATCH /users/me/name) 같은 개별 API 응답을 세션 전체를 다시 받지 않고 반영한다.
     * 이걸 안 하면 저장은 됐는데 화면·스토어는 옛 값을 계속 들고 있게 된다.
     * 넘기지 않은 필드는 그대로 둔다. (DECISIONS.md 2026-08-11)
     */
    function mergeUser(patch) {
        user.value = { ...user.value, ...patch };
    }

    function clear() {
        accessToken.value = '';
        user.value = null;
        needsConsent.value = false;
        needsFinancialConsent.value = false;
        needsAccountLink.value = false;
    }

    return {
        accessToken,
        user,
        needsConsent,
        needsFinancialConsent,
        needsAccountLink,
        isLoggedIn,
        setSession,
        applyOnboardingFlags,
        mergeUser,
        clear,
    };
});
