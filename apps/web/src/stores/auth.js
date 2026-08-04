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
    /**
     * 필수 동의(SIGNUP 그룹)를 아직 마치지 않은 사용자인지.
     * 라우터 가드가 이 값을 보고 동의 화면으로 보낸다.
     * 동의 저장·철회 응답으로도 갱신된다(stores/consent.js).
     */
    const needsConsent = ref(false);

    const isLoggedIn = computed(() => Boolean(accessToken.value));

    function setSession(session) {
        accessToken.value = session.accessToken ?? '';
        user.value = session.user ?? null;
        needsConsent.value = Boolean(session.needsConsent);
    }

    function clear() {
        accessToken.value = '';
        user.value = null;
        needsConsent.value = false;
    }

    return { accessToken, user, needsConsent, isLoggedIn, setSession, clear };
});
