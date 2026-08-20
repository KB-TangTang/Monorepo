<!--
  용도: 구글 로그인 진입 화면. 상단 잉크 패널(법정) + 마스코트 원형 + 사건 카드 + 구글 버튼.
  언제 쓰는지: 라우트 /login. 미로그인 사용자가 보호된 화면에 접근하면 가드가 여기로 보낸다.
  쓰면 안 되는 경우: 로그인 후 화면 — 탭바가 있는 레이아웃을 쓴다.

  디자인 근거(2026-08-20, 이슈 #385 — 팀 시안 기준):
   - 배경은 `--tt-bg-subtle`(#F7F8FA) 다. 앱 63개 화면 중 57개가 쓰는 색이라
     로그인 → `/auth/callback` → `/consent` 로 넘어갈 때 색이 튀지 않는다.
     종이색(크래프트·아이보리)으로 만든 앞의 두 안은 「앱 안에서 거의 안 나오는 색」이라 반려됐다.
   - 상단 잉크 패널은 재판 탭 헤더(`PersonalCourtHeader`)·그룹 챌린지 홈과 같은
     `--tt-ink` 그라데이션 + `--tt-radius-2xl` 하단 곡률이다.
   - 로그인 전에는 서버에서 아무 수치도 받을 수 없다(열린 경로는 `/api/health`·`/api/auth/**` 뿐).
     그래서 팀 시안의 통계 카드 자리에는 **서버 없이 참인 값**(원고·피고인)을 넣는다.
-->
<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { GOOGLE_LOGIN_URL } from '@/api/auth';
import { POST_LOGIN_REDIRECT_KEY, isSafeRedirectPath } from '@/stores/auth';
import { toSummonsCaseNumber } from '@/utils/auth';
import GoogleSignInButton from '@/components/auth/GoogleSignInButton.vue';

const route = useRoute();

const ERROR_MESSAGES = {
    invalid: '로그인 요청이 올바르지 않습니다. 다시 시도해 주세요.',
    failed: '구글 인증에 실패했습니다. 잠시 후 다시 시도해 주세요.',
    /*
     * 탈퇴자는 provider_user_id 가 변조돼 재가입되므로 이 경로로 오지 않는다.
     * 여기 걸리는 것은 BLOCKED 뿐이다. (DECISIONS.md 2026-08-13 회원 탈퇴)
     */
    withdrawn: '정지된 계정입니다. 고객센터에 문의해 주세요.',
    security: '보안을 위해 로그아웃되었습니다. 다시 로그인해 주세요.',
    expired: '로그인이 만료되었습니다. 다시 로그인해 주세요.',
    cancelled: '',
};

/*
 * 오류가 아닌 안내. error=withdrawn(차단)과 뜻이 정반대라 키를 반드시 갈라 둔다 —
 * 같은 이름을 쓰면 「탈퇴 완료」와 「이용 불가」가 한 키를 공유하게 된다.
 */
const NOTICE_MESSAGES = {
    goodbye:
        '탈퇴가 완료되었습니다. 구글 계정 > 보안 > ‘타사 앱 연결’ 에서 탕탕 연결을 직접 해제하실 수 있습니다.',
};

const errorMessage = computed(() => ERROR_MESSAGES[route.query.error] ?? '');
const noticeMessage = computed(() => NOTICE_MESSAGES[route.query.notice] ?? '');

/*
 * 사건번호는 오늘 날짜로 찍는다. 매일 새 사건이 열린다는 뜻이고,
 * 어제 날짜가 박힌 사건번호는 그 자체로 거짓말이 된다.
 * 화면이 떠 있는 동안 자정을 넘기는 경우까지는 보지 않는다(로그인 화면 체류시간).
 */
const caseNumber = toSummonsCaseNumber(new Date());

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
    if (isSafeRedirectPath(redirect)) {
        sessionStorage.setItem(POST_LOGIN_REDIRECT_KEY, redirect);
    } else {
        sessionStorage.removeItem(POST_LOGIN_REDIRECT_KEY);
    }
    window.location.href = GOOGLE_LOGIN_URL;
}
</script>

<template>
    <div class="login">
        <section class="login__hero">
            <span class="login__emblem" aria-hidden="true">
                <span class="login__emblem-rule"></span>
                <svg viewBox="0 0 48 48" fill="none" stroke="currentColor" stroke-width="1.7">
                    <path d="M24 12v26M17 38h14M11 17h26" stroke-linecap="round" />
                    <path d="M11 17 5.5 27h11zM37 17l-5.5 10h11z" stroke-linejoin="round" />
                    <circle cx="24" cy="11" r="2.4" fill="currentColor" stroke="none" />
                </svg>
                <span class="login__emblem-rule"></span>
            </span>

            <h1 class="login__title">오늘의 소비,<br /><em>판결</em>을 시작합니다</h1>

            <p class="login__lead">
                소비 기록을 증거로 확인하고<br />더 나은 금융 습관을 만들어 보세요.
            </p>
        </section>

        <div class="login__body">
            <!-- 잉크 패널과 배경 경계에 걸친다. 마스코트가 법정에서 걸어 나오는 자리다 -->
            <div class="login__portrait">
                <img class="login__mascot" src="@/assets/images/tangtang-cutout.png" alt="" />
            </div>

            <p class="login__ribbon">오늘의 사건 {{ caseNumber }}</p>

            <!--
              팀 시안의 통계 카드 자리다. 로그인 전에는 서버에서 수치를 받을 수 없으므로
              「서버 없이도 참인 값」만 둔다. 원고가 미래의 나이고 피고인이 지갑이라는 것이
              이 서비스의 전제라, 첫 화면에서 그것을 말한다.
              사건번호는 바로 위 리본에 있으므로 여기서 되풀이하지 않는다.
            -->
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
                <p class="login__terms">이용약관 · 개인정보처리방침</p>
            </div>
        </div>
    </div>
</template>

<style scoped>
/*
 * 이 화면은 스크롤 없이 한 화면에 들어가야 한다(안드·iOS 공통 요청, 2026-08-14).
 *  1) 높이 기준은 100dvh — 100vh 는 주소창이 보이는 브라우저에서 실제 화면보다 커서
 *     내용이 아무리 짧아도 주소창 높이만큼 스크롤이 생긴다.
 *  2) 아래 블록(`.login__body`)은 고정 약 350px 이고, 남는 세로는 잉크 패널이 흡수한다.
 * ⚠ height + overflow:hidden 으로 잡지 않는다. 긴 오류 문구나 사용자 글꼴 확대에서
 *   로그인 버튼이 잘려 아예 누를 수 없게 된다. min-height 면 그때만 스크롤이 생긴다.
 */
.login {
    /* 마스코트 원의 지름. 잉크 패널의 아래 여백과 원의 음수 margin 이 이 값에 함께 물려 있다 */
    --login-portrait: 146px;

    display: flex;
    flex-direction: column;
    min-height: 100vh;
    min-height: 100dvh;
    background: var(--tt-bg-subtle);
}

/* ── 잉크 패널(법정) ────────────────────────────────
 * 재판 탭 헤더와 같은 그라데이션·곡률이다. 로그인 후 화면과 이어지는 면이라 값을 맞춰 둔다.
 * padding-top 이 아이폰 상태바 자리를 이 패널이 직접 덮는다.
 * ⚠ padding-bottom 은 원이 파고드는 깊이(지름의 절반)와 짝이다. 한쪽만 고치면 원이 경계에서 뜬다. */
.login__hero {
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    padding: calc(var(--tt-space-6) + env(safe-area-inset-top)) var(--tt-space-6)
        calc(var(--login-portrait) / 2 + var(--tt-space-5));
    border-radius: 0 0 var(--tt-radius-2xl) var(--tt-radius-2xl);
    background: linear-gradient(180deg, var(--tt-ink-glow), var(--tt-ink-deep));
    text-align: center;
}

.login__emblem {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-3);
    color: var(--tt-accent);
    animation: login-rise 0.45s ease-out both;
}

.login__emblem svg {
    width: 32px;
    height: 32px;
    flex-shrink: 0;
}

/* 문장 좌우의 짧은 금선. 월계관 대신 이것만 남긴다 */
.login__emblem-rule {
    width: 40px;
    height: 1px;
    background: linear-gradient(90deg, transparent, currentColor);
}

.login__emblem-rule:last-child {
    background: linear-gradient(90deg, currentColor, transparent);
}

.login__title {
    margin-top: var(--tt-space-5);
    color: var(--tt-text-inverse);
    font-size: 27px;
    font-weight: var(--tt-fw-black);
    line-height: 1.4;
    letter-spacing: -0.01em;
    animation: login-rise 0.45s ease-out 0.07s both;
}

/* 금색은 잉크 위에서만 쓴다. 밝은 배경에서는 대비가 무너진다 */
.login__title em {
    color: var(--tt-accent);
    font-style: normal;
}

.login__lead {
    margin-top: var(--tt-space-4);
    color: color-mix(in srgb, var(--tt-text-inverse) 74%, transparent);
    font-size: var(--tt-fs-body);
    line-height: 1.8;
    animation: login-rise 0.45s ease-out 0.14s both;
}

/* ── 잉크 아래 ─────────────────────────────────────── */
.login__body {
    display: flex;
    flex-shrink: 0;
    flex-direction: column;
    align-items: center;
    padding: 0 var(--tt-space-6) calc(var(--tt-space-5) + env(safe-area-inset-bottom));
}

/*
 * 원의 절반이 잉크 패널을 파고든다. 음수 margin 이 패널 padding-bottom 과 짝이라
 * 둘 중 하나만 바꾸면 마스코트가 경계에서 떨어진다 — `--login-portrait` 한 곳만 고칠 것.
 */
.login__portrait {
    display: flex;
    width: var(--login-portrait);
    height: var(--login-portrait);
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    margin-top: calc(var(--login-portrait) / -2);
    padding: 12px;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-3);
    animation: login-pop 0.42s cubic-bezier(0.22, 1.2, 0.4, 1) 0.2s both;
}

/*
 * 이미지 파일 자체가 정사각 캔버스 한가운데에 캐릭터를 놓은 것이라
 * (2026-08-20 에 원본을 잘라 맞췄다) 여백 없이 채우면 원 정중앙에 선다.
 * ⚠ 이미지를 갈아끼울 때 캐릭터가 캔버스 가운데에 있는지 먼저 확인할 것.
 */
.login__mascot {
    width: 100%;
    height: 100%;
    object-fit: contain;
}

/* 사건 리본. 양끝이 V 로 파인 띠 */
.login__ribbon {
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-2) var(--tt-space-6);
    background: var(--tt-ink);
    color: var(--tt-accent-bright);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-medium);
    letter-spacing: 0.04em;
    clip-path: polygon(0 0, 100% 0, calc(100% - 9px) 50%, 100% 100%, 0 100%, 9px 50%);
    animation: login-rise 0.45s ease-out 0.28s both;
}

/* ── 사건 카드 ─────────────────────────────────────
 * 두 칸이 정확히 반씩 나눠 갖고, 값은 아이콘 오른쪽에서 왼쪽 정렬로 시작한다.
 * 가운데 정렬로 두면 값 길이에 따라 아이콘 위치가 칸마다 달라져 구분선이 어긋나 보인다. */
.login__case {
    display: flex;
    width: 100%;
    align-items: center;
    margin-top: var(--tt-space-5);
    padding: var(--tt-space-4) 0;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-2);
    animation: login-rise 0.45s ease-out 0.34s both;
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
    color: var(--tt-text);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-bold);
    line-height: 1.3;
    /* 값이 길어져도 두 줄로 흐르게 두지 않는다 — 칸 높이가 달라지면 구분선이 어긋난다 */
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.login__case-divider {
    width: 1px;
    height: 32px;
    flex-shrink: 0;
    background: var(--tt-border);
}

/* ── 오류 · 안내 ───────────────────────────────────── */
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

/* 오류가 아니다 — danger 색을 쓰면 탈퇴가 실패한 것처럼 읽힌다 */
.login__notice {
    border: 1px solid var(--tt-border);
    background: var(--tt-bg);
    color: var(--tt-text-muted);
}

.login__action {
    width: 100%;
    margin-top: var(--tt-space-5);
    animation: login-rise 0.45s ease-out 0.4s both;
}

.login__terms {
    margin-top: var(--tt-space-3);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    text-align: center;
}

/* ── 움직임 ────────────────────────────────────────
 * 위에서부터 차례로 놓이고(rise), 마스코트만 한 번 튀어나온다(pop). 흩뿌리지 않는다. */
@keyframes login-rise {
    from {
        opacity: 0;
        transform: translateY(10px);
    }
}

@keyframes login-pop {
    from {
        opacity: 0;
        transform: scale(0.72);
    }
}

/*
 * 낮은 화면(작은 안드로이드 640 · 아이폰 SE 667) 대응.
 * 아래 블록이 약 350px 이라 잉크 패널이 그만큼 눌린다. 여백·글자를 함께 줄인다.
 * ⚠ 값을 키울 일이 생기면 이 블록부터 다시 계산할 것. 이 화면은 스크롤이 생기면 안 된다.
 */
@media (max-height: 700px) {
    .login {
        --login-portrait: 118px;
    }

    .login__hero {
        padding-top: calc(var(--tt-space-4) + env(safe-area-inset-top));
    }

    .login__emblem svg {
        width: 26px;
        height: 26px;
    }

    .login__title {
        margin-top: var(--tt-space-3);
        font-size: 23px;
    }

    .login__lead {
        margin-top: var(--tt-space-2);
        line-height: 1.6;
    }

    .login__ribbon {
        margin-top: var(--tt-space-3);
    }

    .login__case {
        margin-top: var(--tt-space-3);
        padding: var(--tt-space-3) 0;
    }

    .login__action {
        margin-top: var(--tt-space-4);
    }
}

/*
 * 움직임을 줄이도록 설정한 기기에서는 연출만 끈다.
 * 각 요소의 기본 상태가 이미 「완료된 모습」이라 animation:none 으로 충분하다.
 */
@media (prefers-reduced-motion: reduce) {
    .login__emblem,
    .login__title,
    .login__lead,
    .login__portrait,
    .login__ribbon,
    .login__case,
    .login__action {
        animation: none;
    }
}
</style>
