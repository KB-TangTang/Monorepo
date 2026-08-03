import './assets/tokens.css';
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
app.use(router);

/*
 * 부팅 시 세션 복원.
 * 액세스 토큰은 메모리에만 있어 새로고침하면 사라진다. httpOnly 리프레시 쿠키로
 * 한 번 재발급받아 로그인 상태를 되살린다. 구글 콜백에서 돌아온 직후에도 이 경로를 탄다.
 *
 * mount 전에 끝내야 라우터 가드가 올바른 로그인 상태를 보고 판단한다.
 * 실패는 정상 흐름(비로그인)이므로 조용히 넘어간다.
 */
try {
    const session = await refreshSession();
    useAuthStore(pinia).setSession(session);
} catch {
    // 비로그인 상태로 진행한다
}

app.mount('#app');
