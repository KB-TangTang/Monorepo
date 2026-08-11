<!--
  용도: 계좌 연결이 끝났음을 알리고 무엇이 연결됐는지 보여준다 (AC_01_04). 연결 플로우 5단계(마지막).
  언제 쓰는지: 계좌 선택에서 연결을 저장한 직후.
  쓰면 안 되는 경우: 연결 계좌를 관리하는 화면(ConnectedAccountView).

  레이아웃은 `개발참고화면/레퍼런스/계좌연동성공시화면.png` 기준이다 —
  체크 애니메이션 히어로 · 축하 문구 · "연결된 계좌" 카드(상품명 + 마스킹 계좌번호).
  기관 로고는 바로 앞 화면(기관 선택 그리드)과 같은 것을 쓴다. 방금 고른 그것들이라는 게
  얼굴로 이어져야 완료 화면이 결과처럼 읽힌다.
-->
<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import BaseButton from '@/components/common/BaseButton.vue';
import InstitutionLogo from '@/components/account/InstitutionLogo.vue';
import { useAccountStore } from '@/stores/account';
import { useAuthStore } from '@/stores/auth';
import { PROGRESS_STATUS } from '@/utils/account';
import { NICKNAME_SETUP_ROUTE } from '@/utils/user';
import successAnimation from '@/assets/images/link-success.svg';

const router = useRouter();
const store = useAccountStore();
const auth = useAuthStore();
const { linkedCount, linkableGroups, selectedAccountIds, progressInstitutions } =
    storeToRefs(store);

/**
 * 애니메이션을 재생할지.
 *
 * SMIL 애니메이션은 `<img>` 안에서 도는 것이라 CSS 로 멈출 수 없다.
 * 그래서 재생 여부 자체를 여기서 정하고, 끄는 경우에는 정적인 체크를 그린다.
 */
const playsAnimation = !window.matchMedia('(prefers-reduced-motion: reduce)').matches;

/**
 * 방금 연결한 계좌.
 * 기관별로 묶여 있는 조회 결과에서 **사용자가 고른 것만** 펼친다.
 * 기관의 배지 라벨을 함께 실어 화면이 다시 조회하지 않게 한다.
 */
const selectedAccounts = computed(() =>
    linkableGroups.value.flatMap((group) =>
        group.accounts
            .filter((account) => selectedAccountIds.value.includes(account.accountId))
            .map((account) => ({
                ...account,
                shortLabel: group.shortLabel,
                bankCode: group.bankCode,
            })),
    ),
);

/**
 * 목록으로 그릴 계좌.
 *
 * ⚠ 개수의 출처가 둘이다 — 헤더의 `linkedCount` 는 **서버가 실제로 저장한 건수**이고,
 * 목록은 화면이 고른 것이다. 서버는 이미 연결된 계좌를 조용히 건너뛰므로(다른 탭에서 먼저
 * 연결한 경우 등) 두 수가 어긋날 수 있다. 그때 "2개" 아래 3행을 그리면 화면이 스스로 모순된다.
 * 어긋나면 목록을 접고 저장된 건수만 말한다 — 틀린 목록보다 없는 목록이 낫다.
 * 제대로 고치려면 서버가 저장한 계좌 식별자를 응답에 실어야 한다(이슈 #12 후속).
 */
const linkedAccounts = computed(() =>
    selectedAccounts.value.length === linkedCount.value ? selectedAccounts.value : [],
);

const failedCount = computed(
    () =>
        progressInstitutions.value.filter((item) => item.status === PROGRESS_STATUS.FAILED).length,
);

/* 첫 수집은 자동 동기화 배치(매일 18시)를 탄다. */
const firstCollectLabel = computed(() => {
    const now = new Date();
    return now.getHours() < 18 ? '오늘 18:00' : '내일 18:00';
});

/*
 * ⚠ 플로우 상태는 **여기서만** 비운다. 예전에는 onBeforeUnmount 에서 비웠는데,
 *   라우터는 가드를 통과시킨 뒤 이전 화면을 언마운트하므로 뒤로가기로 계좌 선택에 도착한
 *   직후 connectionId 가 지워졌다 — 그 화면이 `/connections//accounts` 를 불러 항상 실패했다.
 *   완료 화면에서 뒤로가면 계좌 선택이 그대로 살아 있는 편이 자연스럽다.
 */
function leaveFlow(name) {
    store.resetFlow();
    router.replace({ name });
}

/*
 * 온보딩으로 들어온 사용자는 여기서 끝이 아니다 — 닉네임 설정이 남았다
 * (로그인 → 동의 → 계좌연동 → **닉네임 설정** → 홈. DECISIONS.md 2026-08-11).
 *
 * 라우터 가드가 안전망으로 잡아주긴 하지만, 그것만 믿고 '연결 계좌 확인' 을 눌렀다가
 * 엉뚱한 화면으로 튕기게 두면 완료 화면이 고장 난 것처럼 보인다. 다음 할 일을 화면이 직접 말한다.
 * 이미 닉네임이 있는 사용자(나중에 기관을 더 붙이는 경우)는 원래 동선 그대로다.
 */
const needsNickname = computed(() => !auth.user?.nickname);
const primaryLabel = computed(() =>
    needsNickname.value ? '닉네임 정하러 가기' : '연결 계좌 확인',
);

function goNext() {
    leaveFlow(needsNickname.value ? NICKNAME_SETUP_ROUTE : 'connectedAccounts');
}

function goHome() {
    leaveFlow('home');
}
</script>

<template>
    <div class="link-done">
        <div class="link-done__body">
            <div class="link-done__hero">
                <img
                    v-if="playsAnimation"
                    class="link-done__animation"
                    :src="successAnimation"
                    alt=""
                    aria-hidden="true"
                />
                <span v-else class="link-done__check" aria-hidden="true">
                    <svg viewBox="0 0 24 24" width="40" height="40">
                        <path
                            d="M4.5 12.5 10 18 19.5 6.5"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="3"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        />
                    </svg>
                </span>
            </div>

            <h1 class="link-done__title">연결이 완료되었어요!</h1>
            <p class="link-done__description">이제 거래내역을 모아<br />새는 돈을 찾아드릴게요.</p>

            <section v-if="linkedCount" class="link-done__card">
                <header class="link-done__card-head">
                    <h2 class="link-done__card-title">연결된 계좌</h2>
                    <span class="link-done__card-count">{{ linkedCount }}개</span>
                </header>
                <ul v-if="linkedAccounts.length" class="link-done__list">
                    <li
                        v-for="account in linkedAccounts"
                        :key="account.accountId"
                        class="link-done__row"
                    >
                        <InstitutionLogo
                            :code="account.bankCode"
                            :short-label="account.shortLabel"
                            size="sm"
                        />
                        <span class="link-done__account">{{ account.accountName }}</span>
                        <span class="link-done__number">{{ account.accountNoMasked }}</span>
                    </li>
                </ul>
            </section>

            <div class="link-done__next">
                <p class="link-done__next-label">첫 거래 수집</p>
                <p class="link-done__next-value">{{ firstCollectLabel }}</p>
            </div>

            <p v-if="failedCount" class="link-done__failure">
                {{ failedCount }}곳은 연결하지 못했어요. 연결 계좌 관리에서 다시 시도할 수 있어요.
            </p>
        </div>

        <div class="link-done__cta">
            <BaseButton variant="accent" block size="lg" @click="goNext">
                {{ primaryLabel }}
            </BaseButton>
            <!-- 닉네임을 정하기 전에는 홈으로 갈 수 없다. 눌러도 가드가 되돌리므로 아예 감춘다. -->
            <button v-if="!needsNickname" class="link-done__link" type="button" @click="goHome">
                홈으로
            </button>
        </div>
    </div>
</template>

<style scoped>
.link-done {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    padding: var(--tt-space-8) var(--tt-space-5) var(--tt-space-8);
    background: var(--tt-bg-subtle);
}

.link-done__body {
    display: flex;
    flex-direction: column;
    align-items: center;
    flex: 1;
    text-align: center;
}

.link-done__hero {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 160px;
    height: 160px;
}

.link-done__animation {
    width: 100%;
    height: 100%;
}

/* 애니메이션을 끈 경우의 대체 표시. 같은 자리에 같은 크기로 앉는다. */
.link-done__check {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 96px;
    height: 96px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary);
    color: var(--tt-bg);
}

.link-done__title {
    margin: var(--tt-space-4) 0 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.link-done__description {
    margin: var(--tt-space-3) 0 0;
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.link-done__card {
    width: 100%;
    margin-top: var(--tt-space-8);
    padding: var(--tt-space-5);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
    text-align: left;
}

.link-done__card-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-3);
}

.link-done__card-title {
    margin: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.link-done__card-count {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.link-done__list {
    margin: var(--tt-space-4) 0 0;
    padding: 0;
    list-style: none;
}

.link-done__row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
}

.link-done__row + .link-done__row {
    margin-top: var(--tt-space-4);
}

.link-done__account {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 계좌번호는 자릿수를 눈으로 세게 되므로 고정폭이다. */
.link-done__number {
    flex-shrink: 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.link-done__next {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    width: 100%;
    margin-top: var(--tt-space-3);
    padding: var(--tt-space-4) var(--tt-space-5);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
    text-align: left;
}

.link-done__next-label {
    margin: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.link-done__next-value {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.link-done__failure {
    margin: var(--tt-space-4) 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.link-done__cta {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-8);
}

/*
 * 체크가 도는 동안 아래 내용이 차례로 올라온다.
 * 연출은 이 한 번뿐이다 — 완료 화면은 오래 머무는 화면이 아니다.
 */
.link-done__title,
.link-done__description,
.link-done__card,
.link-done__next {
    animation: link-done-rise 420ms cubic-bezier(0.16, 1, 0.3, 1) both;
}

.link-done__title {
    animation-delay: 120ms;
}

.link-done__description {
    animation-delay: 200ms;
}

.link-done__card {
    animation-delay: 300ms;
}

.link-done__next {
    animation-delay: 380ms;
}

@keyframes link-done-rise {
    from {
        opacity: 0;
        transform: translateY(12px);
    }
    to {
        opacity: 1;
        transform: none;
    }
}

@media (prefers-reduced-motion: reduce) {
    .link-done__title,
    .link-done__description,
    .link-done__card,
    .link-done__next {
        animation: none;
    }
}

.link-done__link {
    padding: var(--tt-space-2);
    border: 0;
    background: none;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    cursor: pointer;
}
</style>
