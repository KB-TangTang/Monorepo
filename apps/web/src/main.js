import './assets/tokens.css';
import './assets/tokens-compat.css';
import './assets/main.css';

import { createApp } from 'vue';
import { createPinia } from 'pinia';

import App from './App.vue';
import router from './router';
import { refreshSession } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);

/*
 * 세션 복원을 라우터 설치보다 먼저 끝낸다.
 *
 * app.use(router) 는 그 자리에서 초기 내비게이션을 시작한다(vue-router install 내부에서
 * push(routerHistory.location) 를 호출). 복원보다 먼저 설치하면 가드가 "비로그인" 상태를
 * 보고 /login 으로 보내버려, 로그인한 사용자가 새로고침할 때마다 튕긴다.
 *
 * 액세스 토큰은 메모리에만 있어 새로고침하면 사라진다. httpOnly 리프레시 쿠키로
 * 한 번 재발급받아 로그인 상태를 되살린다.
 * 실패는 정상 흐름(비로그인)이므로 조용히 넘어간다.
 */
try {
    const session = await refreshSession();
    useAuthStore(pinia).setSession(session);
} catch (error) {
    // 비로그인 사용자는 여기서 실패하는 게 정상이므로 조용히 넘어간다.
    // 단 재사용 감지로 폐기된 경우(탈취 의심)는 이유를 알려야 하므로 로그인 화면으로 보낸다.
    // refreshSession() 은 http 인스턴스를 타므로 여기서 잡히는 건 ApiError 다(http.js 의
    // 인터셉터가 이미 가공했다) — refreshError.response.data.code 가 아니라 error.code 로 접근한다.
    if (error?.code === 'REFRESH_TOKEN_REUSED') {
        window.location.replace('/login?error=security');
    }
}

app.use(router);
// 초기 내비게이션이 끝난 뒤 마운트해 첫 화면 깜빡임을 막는다
await router.isReady();

app.mount('#app');
