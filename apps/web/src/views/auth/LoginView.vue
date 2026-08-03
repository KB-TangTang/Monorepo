<!--
  용도: 구글 로그인 진입 화면. figma_tangtang/home/download 8.png 기준.
  언제 쓰는지: 라우트 /login. 미로그인 사용자가 보호된 화면에 접근하면 가드가 여기로 보낸다.
  쓰면 안 되는 경우: 로그인 후 화면 — 탭바가 있는 레이아웃을 쓴다.
-->
<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { GOOGLE_LOGIN_URL } from '@/api/auth';
import { POST_LOGIN_REDIRECT_KEY } from '@/stores/auth';
import GoogleSignInButton from '@/components/auth/GoogleSignInButton.vue';

const route = useRoute();

const ERROR_MESSAGES = {
    invalid: '로그인 요청이 올바르지 않습니다. 다시 시도해 주세요.',
    failed: '구글 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.',
    withdrawn: '이용할 수 없는 계정입니다.',
    security: '보안을 위해 로그아웃되었습니다. 다시 로그인해 주세요.',
    expired: '로그인이 만료되었습니다. 다시 로그인해 주세요.',
    cancelled: '',
};

const errorMessage = computed(() => ERROR_MESSAGES[route.query.error] ?? '');

/*
 * SPA 라우팅이 아니라 전체 이동이다.
 * 백엔드가 state 쿠키를 심고 구글로 302 하는 구조라 router.push 로는 동작하지 않는다.
 *
 * 전체 이동이라 라우터의 ?redirect= 값도 함께 날아간다. 그래서 떠나기 직전에
 * sessionStorage 에 맡겨두고, 돌아온 뒤 AuthCallbackView 가 꺼내 쓴다.
 */
function startGoogleLogin() {
    const redirect = route.query.redirect;
    // '/' 로 시작하는 내부 경로만 허용한다. 외부 URL 을 그대로 받으면 오픈 리다이렉트가 된다.
    if (typeof redirect === 'string' && redirect.startsWith('/')) {
        sessionStorage.setItem(POST_LOGIN_REDIRECT_KEY, redirect);
    } else {
        sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
    }
    window.location.href = GOOGLE_LOGIN_URL;
}
</script>

<template>
    <div class="login">
        <p class="login__brand">탕탕 · 지갑재판소</p>

        <div class="login__hero">
            <img class="login__mascot" src="@/assets/images/tangtang.png" alt="" />
        </div>

        <p class="login__case">오늘의 사건 No. 001</p>

        <h1 class="login__title">오늘의 소비,<br />판결을 시작합니다</h1>

        <p class="login__lead">
            소비 기록을 증거로 확인하고<br />더 나은 금융 습관을 만들어보세요.
        </p>

        <div class="login__status">
            <span class="login__status-label">소비 습관 개선 사건</span>
            <span class="login__status-value">판결 준비 완료</span>
        </div>

        <p v-if="errorMessage" class="login__error" role="alert">{{ errorMessage }}</p>

        <div class="login__action">
            <GoogleSignInButton @click-login="startGoogleLogin" />
            <p class="login__terms">이용약관 · 개인정보처리방침</p>
        </div>
    </div>
</template>

<style scoped>
.login {
    display: flex;
    flex-direction: column;
    align-items: center;
    min-height: 100vh;
    padding: var(--tt-space-8) var(--tt-space-6) var(--tt-space-10);
    background: var(--tt-bg-subtle);
    text-align: center;
}

.login__brand {
    margin-bottom: var(--tt-space-6);
    color: var(--tt-primary);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.08em;
}

.login__hero {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 180px;
    height: 180px;
    margin-bottom: var(--tt-space-5);
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary-subtle);
}

.login__mascot {
    width: 130px;
    height: auto;
}

.login__case {
    margin-bottom: var(--tt-space-5);
    padding: var(--tt-space-2) var(--tt-space-4);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
    color: var(--tt-text);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
}

.login__title {
    margin-bottom: var(--tt-space-4);
    color: var(--tt-text);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
}

.login__lead {
    margin-bottom: var(--tt-space-8);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-body);
    line-height: var(--tt-lh-normal);
}

.login__status {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-4);
    width: 100%;
    padding: var(--tt-space-5);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
}

.login__status-label {
    color: var(--tt-text);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
}

.login__status-value {
    color: var(--tt-primary);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
}

.login__error {
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-3) var(--tt-space-4);
    border-radius: var(--tt-radius-sm);
    background: var(--tt-danger-subtle);
    color: var(--tt-danger);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
}

.login__action {
    width: 100%;
    margin-top: auto;
    padding-top: var(--tt-space-10);
}

.login__terms {
    margin-top: var(--tt-space-4);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
}
</style>
