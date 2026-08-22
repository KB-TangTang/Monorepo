import './assets/tokens.css';
import './assets/tokens-compat.css';
import './assets/main.css';

import { createApp, h } from 'vue';
import { createPinia } from 'pinia';

import App from './App.vue';
import router from './router';
import StateError from '@/components/common/StateError.vue';
import { refreshSession } from '@/api/auth';
import { useAuthStore } from '@/stores/auth';
import { isBackendUnreachable } from '@/utils/auth';
import { installStaleChunkRecovery } from '@/utils/staleChunk';

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
let backendUnreachable = false;

try {
    const session = await refreshSession();
    useAuthStore(pinia).setSession(session);
} catch (error) {
    // 비로그인 사용자는 여기서 실패하는 게 정상이므로 조용히 넘어간다.
    // 재사용 감지(REFRESH_TOKEN_REUSED)는 백엔드가 Set-Cookie: Max-Age=0 으로
    // stale 쿠키를 지워주므로, 다음 리로드에서는 단순 인증 실패로 끝난다.
    if (error?.code === 'REFRESH_TOKEN_REUSED') {
        window.location.replace('/login?error=security');
    } else if (isBackendUnreachable(error)) {
        /*
         * 백엔드에 닿지 못한 것을 "비로그인"으로 처리하면 안 된다.
         * 라우터 가드가 로그인 화면으로 보내는데, 사용자에겐 멀쩡하던 세션이
         * 풀린 것으로 보이고 구글 로그인을 다시 누른다 — 그마저도 502 로 떨어진다.
         * 세션은 살아 있으니(리프레시 쿠키·DB 토큰 모두 그대로) 서버가 돌아온 뒤
         * 새로고침 한 번이면 복원된다. 그 사실을 화면으로 알려준다.
         */
        backendUnreachable = true;
    }
}

if (backendUnreachable) {
    createApp({
        render: () =>
            h(StateError, {
                title: '서버에 연결할 수 없습니다',
                message:
                    '배포 중이거나 서버가 일시적으로 내려간 상태입니다. 로그아웃된 것이 아니니 잠시 후 다시 시도해 주세요.',
                onRetry: () => window.location.reload(),
            }),
    }).mount('#app');
} else {
    /*
     * 재배포로 사라진 청크에서 복구한다(이슈 #320).
     * app.use(router) 가 그 자리에서 초기 내비게이션을 시작하므로 그 전에 건다.
     */
    installStaleChunkRecovery(router);

    app.use(router);
    // 초기 내비게이션이 끝난 뒤 마운트해 첫 화면 깜빡임을 막는다
    await router.isReady();

    app.mount('#app');
}
