<!--
  용도: 사용자가 요청한 즉시 거래 조회와 쿨다운 안내 (AC_02_03).
  언제 쓰는지: 연결 계좌 관리 → 즉시 조회.
  쓰면 안 되는 경우: 자동 동기화 상태 확인. 그건 연결 계좌 관리 목록의 배지가 보여준다.

  레이아웃은 개발참고화면 `0-4[계좌관리]조회/동기화.png` 기준이다 —
  진행 스피너 · 다크 요약 카드 · 기관별 신규 거래 건수 · 쿨다운 안내 + 비활성 CTA.

  쿨다운 값(cooldownSeconds)은 서버가 내려준다. 프론트는 카운트다운만 그린다 —
  남은 시간을 프론트가 계산하면 새로고침으로 우회된다.
-->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import BaseButton from '@/components/common/BaseButton.vue';
import StateError from '@/components/common/StateError.vue';
import LinkStepHeader from '@/components/account/LinkStepHeader.vue';
import InstitutionLogo from '@/components/account/InstitutionLogo.vue';
import { useAccountStore } from '@/stores/account';
import { formatAmount, formatCooldown, formatSyncTime } from '@/utils/account';

const router = useRouter();
const store = useAccountStore();
const { refreshResult, loading, error } = storeToRefs(store);

const remaining = ref(0);
let timer = null;

const syncedLabel = computed(() => formatSyncTime(refreshResult.value?.lastSyncAt));

/** 기관 한 줄의 보조 문구. 조회에 실패했으면 건수 대신 이유를 말한다. */
function syncLabel(syncStatus, newTransactionCount) {
    if (syncStatus === 'NEED_RECONNECT') {
        return '연결이 만료돼 다시 연동해야 해요';
    }
    if (syncStatus && syncStatus !== 'NORMAL') {
        return '조회하지 못했어요';
    }
    return `신규 거래 ${newTransactionCount}건`;
}
const canRefresh = computed(() => remaining.value === 0 && !loading.value);

function stopTimer() {
    if (timer) {
        clearInterval(timer);
        timer = null;
    }
}

function startCooldown(seconds) {
    stopTimer();
    remaining.value = seconds ?? 0;
    if (remaining.value <= 0) {
        return;
    }
    timer = setInterval(() => {
        remaining.value -= 1;
        if (remaining.value <= 0) {
            stopTimer();
        }
    }, 1000);
}

async function runRefresh() {
    try {
        const result = await store.refresh();
        startCooldown(result.cooldownSeconds);
    } catch {
        /* 실패 메시지는 store.error 로 노출된다. 쿨다운은 걸지 않는다. */
    }
}

onMounted(() => {
    /* 화면을 다시 열면 직전 방문 결과가 먼저 그려진다. 새로 조회할 것이므로 비우고 시작한다. */
    store.clearRefreshResult();
    runRefresh();
});
onBeforeUnmount(stopTimer);
</script>

<template>
    <div class="account-refresh">
        <LinkStepHeader title="계좌 즉시 조회" @back="router.back()" />

        <div v-if="loading" class="account-refresh__progress">
            <span class="account-refresh__spinner" aria-hidden="true"></span>
            <p class="account-refresh__progress-title">최신 거래 불러오는 중...</p>
            <p class="account-refresh__progress-sub">오늘 거래를 조회하고 있어요</p>
        </div>

        <StateError v-else-if="error && !refreshResult" :message="error" @retry="runRefresh" />

        <!-- v-else-if 여야 한다. v-if 로 두면 조회 중에 스피너와 **직전 결과**가 함께 보인다. -->
        <template v-else-if="refreshResult">
            <section class="account-refresh__summary">
                <div>
                    <p class="account-refresh__summary-label">마지막 동기화 기준</p>
                    <p class="account-refresh__summary-time">{{ syncedLabel }}</p>
                </div>
                <span class="account-refresh__chip">
                    {{ refreshResult.institutionCount }}개 기관
                </span>
            </section>

            <div class="account-refresh__list">
                <div
                    v-for="item in refreshResult.institutions"
                    :key="item.bankCode"
                    class="account-refresh__item"
                >
                    <InstitutionLogo
                        :code="item.bankCode"
                        :short-label="item.shortLabel"
                        size="sm"
                    />
                    <div class="account-refresh__item-body">
                        <p class="account-refresh__item-name">{{ item.bankName }}</p>
                        <!--
                          조회에 실패한 기관은 그 사실을 말한다.
                          예전에는 서버가 공급자를 부르지도 않고 전부 성공으로 썼다.
                        -->
                        <p
                            class="account-refresh__item-sub"
                            :class="{
                                'account-refresh__item-sub--fail':
                                    item.syncStatus && item.syncStatus !== 'NORMAL',
                            }"
                        >
                            {{ syncLabel(item.syncStatus, item.newTransactionCount) }}
                        </p>
                    </div>
                    <p class="account-refresh__item-amount">{{ formatAmount(item.balance) }}</p>
                </div>
            </div>

            <p class="account-refresh__notice">
                <strong>과도한 반복 조회를 막기 위해</strong> 방금 갱신 후에는 잠시 뒤 다시 조회할
                수 있어요.
            </p>

            <p v-if="error" class="account-refresh__error">{{ error }}</p>

            <div class="account-refresh__cta">
                <BaseButton
                    :variant="canRefresh ? 'dark' : 'ghost'"
                    block
                    size="lg"
                    :disabled="!canRefresh"
                    @click="runRefresh"
                >
                    {{
                        canRefresh
                            ? '다시 조회'
                            : `방금 갱신함 · ${formatCooldown(remaining)} 후 다시 조회`
                    }}
                </BaseButton>
            </div>
        </template>
    </div>
</template>

<style scoped>
.account-refresh {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    min-height: 100vh;
    padding: var(--tt-space-5);
    padding-bottom: calc(var(--tt-tabbar-height) + var(--tt-space-6));
    background: var(--tt-bg-subtle);
}

.account-refresh__progress {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--tt-space-2);
    padding: var(--tt-space-6) 0;
}

.account-refresh__spinner {
    width: 32px;
    height: 32px;
    border: 3px solid var(--tt-primary-subtle);
    border-top-color: var(--tt-primary);
    border-radius: var(--tt-radius-full);
    animation: account-refresh-spin 0.8s linear infinite;
}

@keyframes account-refresh-spin {
    to {
        transform: rotate(360deg);
    }
}

@media (prefers-reduced-motion: reduce) {
    .account-refresh__spinner {
        animation-duration: 2.4s;
    }
}

.account-refresh__progress-title {
    margin: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
}

.account-refresh__progress-sub {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.account-refresh__summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4) var(--tt-space-5);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-surface-inverse);
}

.account-refresh__summary-label {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-subtle);
}

.account-refresh__summary-time {
    margin: 2px 0 0;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-inverse);
}

.account-refresh__chip {
    flex-shrink: 0;
    padding: var(--tt-space-2) var(--tt-space-3);
    border-radius: var(--tt-radius-md);
    background: var(--tt-surface-strong-muted);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-inverse);
}

.account-refresh__list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.account-refresh__item {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
}

/* 참고화면 0-4: 은행은 진한 노랑, 카드는 연파랑. */
.account-refresh__item-body {
    flex: 1;
    min-width: 0;
}

.account-refresh__item-sub--fail {
    color: var(--tt-danger);
}

.account-refresh__item-name {
    margin: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.account-refresh__item-sub {
    margin: 2px 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

/* 목록 행 금액은 본문 산스 + section. 모노 34px 는 요약 카드 전용이다 (DESIGN_SYSTEM.md). */
.account-refresh__item-amount {
    margin: 0;
    flex-shrink: 0;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.account-refresh__notice {
    margin: 0;
    padding: var(--tt-space-4);
    border-radius: var(--tt-radius-md);
    background: var(--tt-accent-subtle);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.account-refresh__notice strong {
    color: var(--tt-text);
}

.account-refresh__error {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

.account-refresh__cta {
    margin-top: auto;
    padding-top: var(--tt-space-4);
}
</style>
