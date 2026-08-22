<!--
  용도: 구글 로그인 후 백엔드가 되돌려보내는 착지 지점.
  언제 쓰는지: 라우트 /auth/callback. 사용자가 직접 열 일은 없다.
  쓰면 안 되는 경우: 여기서 refresh 를 다시 호출하는 것.

  refresh 를 부르지 않는 이유:
  OAuth 리다이렉트는 전체 페이지 이동이라 앱이 새로 부팅되고,
  main.js 부팅 시퀀스가 이미 refresh 를 1회 수행한다.
  여기서 또 부르면 리프레시 토큰이 회전 방식이라 두 번째 호출이
  "폐기된 토큰 재사용" 으로 감지돼 전체 토큰이 폐기된다.
-->
<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore, POST_LOGIN_REDIRECT_KEY, isSafeRedirectPath } from '@/stores/auth';
import StateLoading from '@/components/common/StateLoading.vue';

const router = useRouter();
const auth = useAuthStore();

onMounted(() => {
    // 한 번만 쓰고 버린다. 남겨두면 다음 로그인 때 엉뚱한 곳으로 간다.
    const redirect = sessionStorage.getItem(POST_LOGIN_REDIRECT_KEY);
    sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);

    if (!auth.isLoggedIn) {
        router.replace({ name: 'login', query: { error: 'failed' } });
        return;
    }

    // 동의를 마치지 않았으면 원래 가려던 곳보다 동의가 우선이다.
    // (동의 완료 후에는 홈으로 간다 — redirect 는 여기서 버려진다)
    if (auth.needsConsent) {
        router.replace({ name: 'consent' });
        return;
    }

    // 저장 시점에도 검사하지만 여기서 다시 검사한다.
    // sessionStorage 는 사용자가 DevTools 로 직접 고칠 수 있어 저장 시점 검사만으로는 부족하다.
    router.replace(isSafeRedirectPath(redirect) ? redirect : { name: 'home' });
});
</script>

<template>
    <div class="auth-callback">
        <StateLoading message="로그인 중" />
    </div>
</template>

<style scoped>
.auth-callback {
    display: flex;
    align-items: center;
    justify-content: center;
    min-height: 100vh;
    background: var(--tt-bg-subtle);
}
</style>
