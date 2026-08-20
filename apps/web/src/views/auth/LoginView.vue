<!--
  용도: 구글 로그인 진입 화면. **화면 한 장이 곧 출석요구서**다 (이슈 #385).
  언제 쓰는지: 라우트 /login. 미로그인 사용자가 보호된 화면에 접근하면 가드가 여기로 보낸다.
  쓰면 안 되는 경우: 로그인 후 화면 — 탭바가 있는 레이아웃을 쓴다.

  ※ 서비스에는 이미 법정 문서 디자인 시스템이 있다(common/cards 7종 + --tt-font-serif).
    이 화면만 범용 카드 스택이라 언어가 갈렸다. 그 구멍을 메우는 것이 이 화면의 목적이다.
    문법은 SummonsCard 를 따른다 — 크래프트 종이 · 모노 키커 · 펀치홀 절취선 · 스텁.
-->
<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { GOOGLE_LOGIN_URL } from '@/api/auth';
import { POST_LOGIN_REDIRECT_KEY, isSafeRedirectPath } from '@/stores/auth';
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
 * 사건번호와 출석 기일은 **오늘 날짜에서 만든다.**
 * 예전에는 「오늘의 사건 No. 001」이 하드코딩이라 모든 사용자가 영원히 같은 번호를 봤다.
 * 사건번호는 이 서비스의 핵심 장치인데 데이터인 척하는 장식이면 값이 없다.
 * 서버 값이 아니라 기기 시각이지만, 로그인 전에는 사용자 데이터가 없으므로 이것이 가질 수 있는
 * 유일한 진짜 값이다. 날짜가 바뀌면 번호도 바뀐다.
 */
const today = new Date();
const pad = (n) => String(n).padStart(2, '0');

const caseNumber = computed(
    () => `제${today.getFullYear()}-${pad(today.getMonth() + 1)}${pad(today.getDate())}호`,
);
const hearingDate = computed(
    () => `${today.getFullYear()}. ${pad(today.getMonth() + 1)}. ${pad(today.getDate())}`,
);

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
        <!-- 법정. 서류의 발신처다 -->
        <header class="login__court">탕탕지방법원</header>

        <div class="login__sheet">
            <p class="login__kicker">SUMMONS · 출석요구서</p>
            <p class="login__no">사건 {{ caseNumber }} · 소비습관</p>

            <h1 class="login__title">오늘의 소비,<br />법정에 세웁니다</h1>

            <div class="login__rule" aria-hidden="true"></div>

            <p class="login__lead">
                새는 돈은 <em>증거</em>로 남기고,<br />소비 습관은 <em>판결</em>로 바꿉니다.
            </p>

            <div class="login__figure">
                <img class="login__mascot" src="@/assets/images/tangtang-cutout.png" alt="" />
                <!--
                  관인. StampSeal 을 쓰지 않는 이유는 그것이 유죄·무죄·확정 세 판정 전용이기 때문이다.
                  관인은 판정이 아니라 발신처 표시라 종류를 늘리면 그 컴포넌트의 뜻이 흐려진다.
                  기울기는 디자인시스템 기준(-8 ~ -12도)을 그대로 따른다.
                -->
                <span class="login__seal" aria-hidden="true">탕탕<br />지방법원</span>
            </div>

            <p v-if="errorMessage" class="login__error" role="alert">{{ errorMessage }}</p>
            <p v-if="noticeMessage" class="login__notice">{{ noticeMessage }}</p>

            <div class="login__tear" aria-hidden="true"></div>

            <div class="login__stub">
                <span class="login__stub-label">출석 기일</span>
                <span class="login__stub-value">{{ hearingDate }}</span>
            </div>

            <GoogleSignInButton @click-login="startGoogleLogin" />
            <p class="login__terms">이용약관 · 개인정보처리방침</p>
        </div>
    </div>
</template>

<style scoped>
/*
 * 이 화면은 스크롤 없이 한 화면에 들어가야 한다(안드·iOS 공통 요청, 2026-08-14).
 *  1) 높이 기준은 100dvh — 100vh 는 주소창이 보이는 브라우저에서 실제 화면보다 커서
 *     내용이 아무리 짧아도 주소창 높이만큼 스크롤이 생긴다.
 *  2) 마스코트 높이를 vh 에 묶어 작은 기기(아이폰 SE 등)에서 함께 줄어들게 한다.
 * 여백을 키울 때는 가장 작은 기기에서 스크롤이 생기지 않는지 반드시 확인할 것.
 */
.login {
    /* 서류 좌우 여백. 절취선 반원도 이 값에 맞물려 있어 한 곳에서만 바꾼다 */
    --login-gutter: 28px;

    display: flex;
    flex-direction: column;
    min-height: 100vh;
    min-height: 100dvh;
    /*
     * ⚠ height 가 아니라 min-height 다. height + overflow:hidden 으로 잡으면 내용이 예상보다
     *   커졌을 때(긴 오류 문구 · 사용자 글꼴 확대) 로그인 버튼이 잘려 **아예 누를 수 없게** 된다.
     *   min-height 면 그런 경우에만 스크롤이 생겨 버튼에 닿을 수 있다. 평상시에는 스크롤이 없다.
     *
     * overflow-x 만 막는 것은 절취선 반원 때문이다 — 화면 밖으로 11px 씩 나가 있어
     * 그대로 두면 가로 스크롤이 생긴다. 세로는 위 이유로 막지 않는다.
     */
    overflow-x: hidden;
    background: var(--tt-kraft);
}

/* 상단 잉크 띠. 종이는 그 아래로 화면 끝까지 간다 */
.login__court {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-3);
    height: 56px;
    padding-top: env(safe-area-inset-top);
    box-sizing: content-box;
    background: linear-gradient(180deg, var(--tt-ink-glow), var(--tt-ink-deep));
    color: var(--tt-gold-bright);
    font-family: var(--tt-font-serif);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.2em;
}

/* 좌우 짧은 괘선. 「— 탕탕지방법원 —」 형태로 발신처를 감싼다 */
.login__court::before,
.login__court::after {
    width: 18px;
    height: 1px;
    background: rgba(247, 201, 72, 0.5);
    content: '';
}

.login__sheet {
    position: relative;
    display: flex;
    flex: 1;
    flex-direction: column;
    min-height: 0;
    padding: var(--tt-space-6) var(--login-gutter)
        calc(var(--tt-space-6) + env(safe-area-inset-bottom));
}

/* 종이결. 평면 색이 아니라 종이로 보이게 하는 아주 옅은 가로 섬유 */
.login__sheet::before {
    position: absolute;
    inset: 0;
    background: repeating-linear-gradient(
        0deg,
        rgba(156, 123, 84, 0.05) 0 1px,
        transparent 1px 4px
    );
    content: '';
    opacity: 0.5;
    pointer-events: none;
}

.login__kicker {
    color: var(--tt-wood-deep);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.2em;
}

.login__no {
    margin-top: var(--tt-space-2);
    color: var(--tt-wood-deep);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-overline);
    letter-spacing: 0.06em;
}

.login__title {
    margin-top: var(--tt-space-5);
    color: var(--tt-text);
    font-family: var(--tt-font-serif);
    font-size: 29px;
    font-weight: var(--tt-fw-black);
    line-height: 1.4;
}

.login__rule {
    margin: var(--tt-space-5) 0 var(--tt-space-4);
    height: 1px;
    background: repeating-linear-gradient(to right, var(--tt-wood) 0 4px, transparent 4px 9px);
    opacity: 0.45;
}

.login__lead {
    color: var(--tt-wood-deep);
    font-family: var(--tt-font-serif);
    font-size: var(--tt-fs-label);
    line-height: 1.95;
}

.login__lead em {
    color: var(--tt-text);
    font-style: normal;
    font-weight: var(--tt-fw-bold);
}

/*
 * 남는 세로 공간을 마스코트가 가져간다. 예전 화면은 이 자리가 통째로 비어 있었다.
 * min-height:0 이 없으면 flex 아이템이 내용 크기 밑으로 줄지 않아 아래 요소를 밀어낸다.
 */
.login__figure {
    position: relative;
    display: flex;
    flex: 1;
    min-height: 0;
    align-items: center;
    justify-content: center;
}

/*
 * 높이를 vh 에 묶는다 — 작은 기기에서 함께 줄어야 스크롤이 안 생긴다.
 * drop-shadow 는 종이 색 계열이다. 마스코트가 붙여넣은 것이 아니라 종이 위에 놓인 것처럼 보이게 한다.
 */
.login__mascot {
    height: min(200px, 26vh);
    width: auto;
    transform: translateX(-9%);
    filter: drop-shadow(0 6px 10px rgba(93, 76, 57, 0.18));
}

.login__seal {
    position: absolute;
    top: 18%;
    right: 2px;
    display: grid;
    place-items: center;
    width: 66px;
    height: 66px;
    border: 2px solid var(--tt-danger-deep);
    border-radius: var(--tt-radius-full);
    color: var(--tt-danger-deep);
    font-family: var(--tt-font-serif);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    line-height: 1.2;
    letter-spacing: 0.03em;
    text-align: center;
    transform: rotate(-9deg);
    opacity: 0.72;
}

/* 인장 안쪽 테두리 — 관인은 이중 원이다 */
.login__seal::before {
    position: absolute;
    inset: 4px;
    border: 1px solid var(--tt-danger-deep);
    border-radius: var(--tt-radius-full);
    content: '';
}

/*
 * 절취선을 화면 양끝까지 뺀다(음수 margin). 반원이 가장자리를 파고들어야
 * 「카드 한 장」이 아니라 「화면 자체가 한 장의 서류」로 읽힌다.
 * 반원 색은 SummonsCard 와 같은 방식 — 종이 뒤 배경색을 칠한 원이다.
 */
.login__tear {
    position: relative;
    flex-shrink: 0;
    margin: 0 calc(var(--login-gutter) * -1);
    height: 1px;
    background: repeating-linear-gradient(to right, var(--tt-wood) 0 6px, transparent 6px 12px);
    opacity: 0.55;
}

.login__tear::before,
.login__tear::after {
    position: absolute;
    top: -11px;
    width: 22px;
    height: 22px;
    background: var(--tt-notch-bg);
    border-radius: var(--tt-radius-full);
    content: '';
}

.login__tear::before {
    left: -11px;
}

.login__tear::after {
    right: -11px;
}

/* 뜯어 보관하는 부본. 예전의 「판결 준비 완료」 카드 자리를 대신한다 */
.login__stub {
    display: flex;
    flex-shrink: 0;
    align-items: baseline;
    justify-content: space-between;
    padding: var(--tt-space-5) 0 var(--tt-space-6);
}

.login__stub-label {
    color: var(--tt-wood-deep);
    font-family: var(--tt-font-serif);
    font-size: var(--tt-fs-body);
    letter-spacing: 0.02em;
}

.login__stub-value {
    color: var(--tt-text);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.05em;
}

.login__error {
    flex-shrink: 0;
    margin-bottom: var(--tt-space-4);
    padding: var(--tt-space-3) var(--tt-space-4);
    border-radius: var(--tt-radius-sm);
    background: var(--tt-danger-subtle);
    color: var(--tt-danger);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    text-align: center;
}

/* 오류가 아니다 — danger 색을 쓰면 탈퇴가 실패한 것처럼 읽힌다 */
.login__notice {
    flex-shrink: 0;
    margin-bottom: var(--tt-space-4);
    padding: var(--tt-space-3) var(--tt-space-4);
    border: 1px dashed rgba(156, 123, 84, 0.5);
    border-radius: var(--tt-radius-sm);
    color: var(--tt-wood);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    text-align: center;
}

.login__terms {
    flex-shrink: 0;
    margin-top: var(--tt-space-3);
    color: var(--tt-wood-deep);
    font-size: var(--tt-fs-caption);
    text-align: center;
}

/*
 * 낮은 화면 대응. **실측으로 잡은 값이다.**
 *
 * 마스코트를 뺀 고정 높이 합계가 506px 이다(상단 띠 56 · 시트 패딩 48 · 키커 18 · 사건번호 26 ·
 * 제목 101 · 괘선 37 · 본문 60 · 절취선 1 · 스텁 68 · 버튼 60 · 약관 31).
 * 여기에 마스코트(26vh)를 더하면 iPhone SE(667)에서 12px, 작은 안드로이드(640)에서 32px 넘쳤다.
 *
 * 아래 블록이 약 79px 을 줄여 640 에서도 스크롤이 생기지 않게 한다.
 * ⚠ 값을 키울 일이 생기면 이 예산부터 다시 재고 고칠 것. 이 화면은 스크롤이 생기면 안 된다.
 */
/*
 * 좁은 화면 대응. 320px 폭(구형 SE 등)에서 로그인 버튼 문구가 두 줄로 깨졌다.
 * 좌우 여백을 줄여 버튼에 폭을 돌려준다.
 * ⚠ 여백을 --login-gutter 변수로 둔 이유: 아래 max-height 블록이 padding 단축 속성으로
 *   상하 여백을 다시 잡는다. 여기서 padding-left/right 만 덮으면 뒤에 오는 단축 속성이
 *   좌우까지 28px 로 되돌려 이 대응이 통째로 무효가 된다(실제로 그렇게 한 번 깨졌다).
 */
@media (max-width: 360px) {
    .login {
        --login-gutter: var(--tt-space-5);
    }
}

@media (max-height: 720px) {
    .login__court {
        height: 46px;
    }

    .login__sheet {
        padding: var(--tt-space-5) var(--login-gutter)
            calc(var(--tt-space-5) + env(safe-area-inset-bottom));
    }

    .login__title {
        margin-top: var(--tt-space-4);
        font-size: var(--tt-fs-title);
    }

    .login__rule {
        margin: var(--tt-space-4) 0 var(--tt-space-3);
    }

    .login__lead {
        font-size: var(--tt-fs-body);
        line-height: 1.75;
    }

    .login__mascot {
        height: min(200px, 22vh);
    }

    .login__stub {
        padding: var(--tt-space-4) 0 var(--tt-space-5);
    }

    .login__terms {
        margin-top: var(--tt-space-2);
    }
}
</style>
