<script setup>
/*
 * 구글 로그인 진입 화면. 디자인소스.png의 화면 구성과 로그인 전용 에셋을 사용한다.
 * 로그인 후 화면에는 탭바 레이아웃을 사용한다.
 */
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { GOOGLE_LOGIN_URL } from '@/api/auth';
import { POST_LOGIN_REDIRECT_KEY, isSafeRedirectPath } from '@/stores/auth';
import GoogleSignInButton from '@/components/auth/GoogleSignInButton.vue';
import heroEmblemImage from '@/assets/images/auth/login/login-hero-emblem.png';
import columnImage from '@/assets/images/auth/login/login-column-single.png';
import ribbonImage from '@/assets/images/auth/login/login-ribbon.png';

const route = useRoute();

const ERROR_MESSAGES = {
    invalid: '로그인 요청이 올바르지 않습니다. 다시 시도해 주세요.',
    failed: '구글 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.',
    // 탈퇴 사용자가 아니라 차단된 계정에만 이 안내를 노출한다.
    withdrawn: '정지된 계정입니다. 고객센터에 문의해 주세요.',
    security: '보안을 위해 로그아웃되었습니다. 다시 로그인해 주세요.',
    expired: '로그인이 만료되었습니다. 다시 로그인해 주세요.',
    cancelled: '',
};

const NOTICE_MESSAGES = {
    // error=withdrawn(차단)과 의미가 반대인 탈퇴 완료 안내다.
    goodbye:
        '탈퇴가 완료되었습니다. 구글 계정 > 보안 > ‘타사 앱 연결’ 에서 탕탕 연결을 직접 해제하실 수 있습니다.',
};

const errorMessage = computed(() => ERROR_MESSAGES[route.query.error] ?? '');
const noticeMessage = computed(() => NOTICE_MESSAGES[route.query.notice] ?? '');
function startGoogleLogin() {
    const redirect = route.query.redirect;

    // 내부 경로만 보존해 외부 URL로의 리다이렉트를 막는다.
    if (isSafeRedirectPath(redirect)) {
        sessionStorage.setItem(POST_LOGIN_REDIRECT_KEY, redirect);
    } else {
        sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
    }

    // 백엔드가 state 쿠키를 설정한 후 구글로 이동하므로 전체 페이지 이동이 필요하다.
    window.location.href = GOOGLE_LOGIN_URL;
}
</script>

<template>
    <div class="login">
        <section class="login__hero">
            <div class="login__columns" aria-hidden="true">
                <img class="login__column login__column--left" :src="columnImage" alt="" />
                <img class="login__column login__column--right" :src="columnImage" alt="" />
            </div>

            <div class="login__hero-content">
                <img class="login__emblem" :src="heroEmblemImage" alt="" aria-hidden="true" />
                <h1 class="login__title">오늘의 소비,<br /><em>판결</em>을 시작합니다</h1>
                <p class="login__lead">
                    소비 기록을 증거로 확인하고<br />더 나은 금융 습관을 만들어 보세요.
                </p>
            </div>
        </section>

        <div class="login__body">
            <div class="login__portrait">
                <span class="login__portrait-surface" aria-hidden="true"></span>
                <img class="login__mascot" src="@/assets/images/tangtang-cutout.png" alt="" />
            </div>

            <div class="login__ribbon" aria-label="오늘의 사건 번호">
                <img class="login__ribbon-image" :src="ribbonImage" alt="" aria-hidden="true" />
                <span class="login__ribbon-label">오늘의 사건 No.001</span>
            </div>

            <dl class="login__case">
                <div class="login__case-cell">
                    <span class="login__case-icon" aria-hidden="true">
                        <svg
                            viewBox="0 0 20 20"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="1.6"
                        >
                            <circle cx="10" cy="7" r="3.1" />
                            <path d="M4.2 16.4a5.8 5.8 0 0 1 11.6 0" stroke-linecap="round" />
                        </svg>
                    </span>
                    <div>
                        <dt class="login__case-key">원고</dt>
                        <dd class="login__case-value">미래의 나</dd>
                    </div>
                </div>

                <span class="login__case-divider" aria-hidden="true"></span>

                <div class="login__case-cell">
                    <span class="login__case-icon" aria-hidden="true">
                        <svg
                            viewBox="0 0 20 20"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="1.6"
                        >
                            <path
                                d="M3 6.4c0-1 .8-1.8 1.8-1.8h9.4c1 0 1.8.8 1.8 1.8v8.2c0 1-.8 1.8-1.8 1.8H4.8c-1 0-1.8-.8-1.8-1.8z"
                                stroke-linejoin="round"
                            />
                            <path d="M3 8.6h5.2a1.9 1.9 0 0 1 0 3.8H3" stroke-linejoin="round" />
                        </svg>
                    </span>
                    <div>
                        <dt class="login__case-key">피고인</dt>
                        <dd class="login__case-value">당신의 지갑</dd>
                    </div>
                </div>
            </dl>

            <p v-if="errorMessage" class="login__error" role="alert">{{ errorMessage }}</p>
            <p v-if="noticeMessage" class="login__notice">{{ noticeMessage }}</p>

            <div class="login__action">
                <GoogleSignInButton @click-login="startGoogleLogin" />
                <p class="login__terms">
                    이용약관 <span aria-hidden="true">|</span> 개인정보처리방침
                </p>
            </div>
        </div>
    </div>
</template>

<style scoped>
.login {
    --login-portrait: min(38.5vw, 186px);
    --login-hero-height: 93.023vw;

    position: relative;
    display: flex;
    min-height: 100vh;
    min-height: 100dvh;
    flex-direction: column;
    overflow-x: clip;
    background: var(--tt-bg);
}

.login__hero {
    position: relative;
    display: flex;
    height: var(--login-hero-height);
    flex: 0 0 var(--login-hero-height);
    justify-content: center;
    overflow: hidden;
    border-radius: 0 0 50% 50% / 0 0 7% 7%;
    background: linear-gradient(180deg, var(--tt-ink-glow), var(--tt-ink-deep));
    isolation: isolate;
}

.login__columns {
    position: absolute;
    top: 30%;
    right: 0;
    left: 0;
    z-index: -1;
    width: 100%;
    height: 100%;
    clip-path: inset(0 0 30% 0);
    mask-image: linear-gradient(to bottom, black 30%, transparent 70%);
    opacity: 0.28;
    pointer-events: none;
}

.login__column {
    position: absolute;
    top: 0;
    width: min(24vw, 104px);
    height: 117%;
    object-fit: fill;
}

.login__column--left {
    left: 5%;
    transform: scaleX(1.8);
    transform-origin: right center;
}

.login__column--right {
    right: 1%;
    transform: scaleX(1.8);
    transform-origin: left center;
}

.login__hero-content {
    position: absolute;
    top: 0;
    left: 50%;
    display: flex;
    width: 100%;
    align-self: center;
    box-sizing: border-box;
    flex-direction: column;
    align-items: center;
    padding: min(6vw, 29px) var(--tt-space-6) calc(var(--login-portrait) * 0.3 + var(--tt-space-2));
    text-align: center;
    transform: translate(-50%, 6px);
}

.login__emblem {
    width: min(40vw, 192px);
    height: auto;
    margin-inline: auto;
}

.login__title {
    margin: calc(min(3.7vw, 16px) + 5px) auto 0;
    color: var(--tt-text-inverse);
    font-size: min(7vw, 34px);
    font-weight: var(--tt-fw-black);
    line-height: 1.4;
    letter-spacing: -0.03em;
}

.login__title em {
    color: var(--tt-accent);
    font-style: normal;
}

.login__lead {
    margin: var(--tt-space-5) auto 0;
    color: color-mix(in srgb, var(--tt-text-inverse) 78%, transparent);
    font-size: var(--tt-fs-body);
    line-height: 1.8;
}

.login__body {
    display: flex;
    width: 100%;
    flex: 1;
    flex-shrink: 0;
    flex-direction: column;
    align-items: center;
    padding: 0 var(--tt-space-6) calc(var(--tt-space-5) + 8px + env(safe-area-inset-bottom));
}

.login__portrait {
    position: relative;
    display: flex;
    width: var(--login-portrait);
    height: var(--login-portrait);
    align-self: center;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    margin-top: calc(var(--login-portrait) * -0.48);
    padding: 12px;
    border: 2px solid var(--tt-bg);
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
    box-shadow:
        0 0 0 1px var(--tt-accent),
        var(--tt-elevation-3);
}

.login__portrait::before {
    position: absolute;
    z-index: 1;
    inset: 2px;
    border: 2px dashed var(--tt-accent);
    border-radius: inherit;
    content: '';
    pointer-events: none;
}

.login__portrait-surface {
    position: absolute;
    inset: 10px;
    z-index: 0;
    border-radius: inherit;
    background: var(--tt-bg-fill);
}

.login__mascot {
    position: relative;
    z-index: 2;
    width: 90%;
    height: 90%;
    object-fit: contain;
}

.login__ribbon {
    position: relative;
    display: flex;
    z-index: 3;
    width: min(58vw, 280px);
    height: min(13vw, 62px);
    align-self: center;
    align-items: center;
    justify-content: center;
    margin-top: calc(var(--login-portrait) * -0.1);
}

.login__ribbon-image {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 100%;
    max-width: none;
    height: auto;
    transform: translate(-50%, -50%) scaleY(1.45);
}

.login__ribbon-label {
    position: relative;
    top: -5px;
    color: var(--tt-accent-bright);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.04em;
}

.login__case {
    display: flex;
    width: 100%;
    align-items: center;
    margin-top: auto;
    padding: var(--tt-space-4) 0;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-2);
}

.login__case-cell {
    display: flex;
    width: 50%;
    min-width: 0;
    align-items: center;
    gap: var(--tt-space-3);
    padding: 0 var(--tt-space-4);
}

.login__case-icon {
    display: inline-flex;
    width: 34px;
    height: 34px;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg-fill);
    color: var(--tt-primary);
}

.login__case-icon svg {
    width: 19px;
    height: 19px;
}

.login__case-key {
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    line-height: 1.35;
}

.login__case-value {
    margin-top: 1px;
    overflow: hidden;
    color: var(--tt-text);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-bold);
    line-height: 1.3;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.login__case-divider {
    width: 1px;
    height: 32px;
    flex-shrink: 0;
    background: var(--tt-border);
}

.login__error,
.login__notice {
    width: 100%;
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-3) var(--tt-space-4);
    border-radius: var(--tt-radius-sm);
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    text-align: center;
}

.login__notice {
    border: 1px solid var(--tt-border);
    background: var(--tt-bg);
    color: var(--tt-text-muted);
}

.login__action {
    width: 100%;
    margin-top: var(--tt-space-10);
}

.login__terms {
    margin-top: var(--tt-space-4);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    text-align: center;
}

.login__terms span {
    margin: 0 var(--tt-space-3);
}

@media (max-height: 700px) {
    .login {
        --login-portrait: min(33vw, 158px);
        --login-hero-height: min(68vw, 320px);
    }

    .login__hero-content {
        padding-top: min(5vw, 24px);
        transform: translate(-50%, 0);
    }

    .login__title {
        margin-top: calc(min(1.7vw, 8px) + 5px);
        font-size: min(6.3vw, 30px);
    }

    .login__lead {
        margin-top: var(--tt-space-2);
        line-height: 1.6;
    }

    .login__case {
        padding: var(--tt-space-3) 0;
    }

    .login__action {
        margin-top: var(--tt-space-6);
    }
}
</style>
