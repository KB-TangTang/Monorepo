<!--
  용도: 연결된 계좌의 상태 조회 · 재동기화 · 재연동 · 해제 (AC_01_04). 자산 탭 소속 화면.
  언제 쓰는지: 자산 탭 → 연결 계좌 관리. 연결 완료 후에도 여기로 온다.
  쓰면 안 되는 경우: 연결 플로우 중간. 그건 /accounts/link/* 다.

  레이아웃은 Figma 확정본(1620:7659) 기준이다 —
  연파랑 요약 카드 · `내 계좌와 카드` 섹션 + 우측 동기화 시각 · 흰 카드 행 · 점선 추가 연결 CTA.
  행 액션은 `⋮` 더보기(AccountActionSheet)로 접는다.

  AC_02_04(동기화 상태)·AC_02_06(재연동)을 별도 화면으로 만들지 않고 목록 행에 흡수했다.
  해제는 반드시 DisconnectConfirmSheet 를 거친다 — 명세가 "해제 전 영향 범위 안내"를 요구한다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import AccountActionSheet from '@/components/account/AccountActionSheet.vue';
import ConnectedAccountRow from '@/components/account/ConnectedAccountRow.vue';
import DisconnectConfirmSheet from '@/components/account/DisconnectConfirmSheet.vue';
import { useAccountStore } from '@/stores/account';
import { SYNC_STATUS, formatSyncTime } from '@/utils/account';

const router = useRouter();
const store = useAccountStore();
const { connectedAccounts, nextAutoSyncAt, loading, error } = storeToRefs(store);

const actionOpen = ref(false);
const actionTarget = ref(null);
const sheetOpen = ref(false);
const sheetTarget = ref(null);
const disconnecting = ref(false);

const hasAccounts = computed(() => connectedAccounts.value.length > 0);
const nextSyncLabel = computed(() => formatSyncTime(nextAutoSyncAt.value));

/*
 * 요약 카드는 **계좌 단위**로 센다. 아래 목록이 계좌 단위이고 `정상`도 계좌마다 붙는 상태다.
 * ⚠ 전에는 기관 수와 정상 계좌 수를 섞어 비교해 "2개 중 3개 정상 연결" 같은 문장이 나왔다.
 *   한 기관에 계좌가 여러 개일 수 있으니 두 수는 애초에 비교 대상이 아니다.
 */
const accountCount = computed(() => connectedAccounts.value.length);
const normalCount = computed(
    () =>
        connectedAccounts.value.filter((account) => account.syncStatus === SYNC_STATUS.NORMAL)
            .length,
);
const allNormal = computed(() => accountCount.value === normalCount.value);

const lastSyncLabel = computed(() => {
    const times = connectedAccounts.value.map((account) => account.lastSyncAt).filter(Boolean);
    return times.length ? formatSyncTime(times.sort().at(-1)) : '';
});

onMounted(() => {
    store.loadConnectedAccounts().catch(() => {});
});

function goAddInstitution() {
    router.push({ name: 'accountLinkInstitutions', query: { mode: 'add' } });
}

function goRefresh() {
    if (actionOpen.value) {
        leaveTo({ name: 'accountRefresh' });
        return;
    }
    router.push({ name: 'accountRefresh' });
}

function openActions(account) {
    actionTarget.value = account;
    actionOpen.value = true;
}

/*
 * 해제 확인은 액션 시트를 **닫지 않고 위에 겹쳐** 띄운다.
 * useOverlay 의 스크롤 잠금은 참조 카운트라 겹침을 견디지만, 히스토리 항목은
 * 먼저 닫히는 쪽의 back() 이 나중에 열린 쪽 것을 삼킨다. 겹쳐 두면 그 충돌이 없다.
 */
function openDisconnect(account) {
    sheetTarget.value = account;
    sheetOpen.value = true;
}

/** 확인 시트를 닫을 때 액션 시트도 함께 접는다 — 사용자는 한 흐름으로 느낀다. */
function closeSheets() {
    sheetOpen.value = false;
    actionOpen.value = false;
    sheetTarget.value = null;
}

async function confirmDisconnect(account) {
    if (!account) {
        return;
    }
    disconnecting.value = true;
    try {
        await store.disconnect(account.accountId);
        closeSheets();
    } catch {
        /* 실패 메시지는 store.error 로 노출된다. 시트는 닫지 않는다. */
    } finally {
        disconnecting.value = false;
    }
}

function onResync(account) {
    /* 화면 이동이 없는 액션은 시트를 닫고 그 자리에서 처리한다. */
    actionOpen.value = false;
    store.resync(account.accountId).catch(() => {});
}

function onReconnect(account) {
    /* 왜 끊겼는지·무엇이 멈췄는지 먼저 알린다(AC_02_06). 재인증은 그 화면에서 시작한다. */
    leaveTo({ name: 'accountReconnect', params: { accountId: account.accountId } });
}

/**
 * 시트에서 다른 화면으로 나갈 때.
 * 시트가 닫히며 history.back() 을 부르므로, 라우팅을 같은 tick 에 하면 서로 밀어낸다.
 * 닫힘이 히스토리에 반영된 뒤 이동한다.
 */
function leaveTo(target) {
    actionOpen.value = false;
    setTimeout(() => router.push(target), 80);
}
</script>

<template>
    <div class="connected-accounts">
        <h1 class="connected-accounts__title">연결 계좌 관리</h1>

        <!-- 확정본의 연파랑 상태 카드 -->
        <div v-if="hasAccounts" class="connected-accounts__summary">
            <p class="connected-accounts__summary-label">계좌 연결 상태</p>
            <p class="connected-accounts__summary-value">
                <template v-if="allNormal">계좌 {{ accountCount }}개 정상 연결</template>
                <template v-else>
                    계좌 {{ accountCount }}개 중 {{ normalCount }}개 정상 연결
                </template>
            </p>
            <p v-if="nextAutoSyncAt" class="connected-accounts__next">
                다음 자동 동기화 · {{ nextSyncLabel }}
            </p>
        </div>

        <StateLoading v-if="loading && !hasAccounts" message="연결된 계좌를 불러오는 중" />
        <StateError
            v-else-if="error && !hasAccounts"
            :message="error"
            @retry="store.loadConnectedAccounts"
        />
        <StateEmpty
            v-else-if="!hasAccounts"
            title="연결된 계좌가 없어요"
            description="금융기관을 연결하면 거래내역을 모아드릴게요."
        >
            <template #action>
                <BaseButton variant="dark" @click="goAddInstitution">금융기관 연결</BaseButton>
            </template>
        </StateEmpty>

        <template v-else>
            <p v-if="error" class="connected-accounts__error">{{ error }}</p>

            <div class="connected-accounts__section">
                <h2 class="connected-accounts__section-title">내 계좌와 카드</h2>
                <span v-if="lastSyncLabel" class="connected-accounts__section-meta">
                    동기화 {{ lastSyncLabel }}
                </span>
            </div>

            <div class="connected-accounts__list">
                <ConnectedAccountRow
                    v-for="account in connectedAccounts"
                    :key="account.accountId"
                    :account="account"
                    @more="openActions"
                />
            </div>

            <!-- 확정본: 점선 테두리 + 파란 글자. 목록에 이어 붙는 '더하기' 자리다. -->
            <button class="connected-accounts__add" type="button" @click="goAddInstitution">
                ＋ 금융기관 추가 연결
            </button>
        </template>

        <AccountActionSheet
            v-model="actionOpen"
            :account="actionTarget"
            @resync="onResync"
            @reconnect="onReconnect"
            @refresh="goRefresh"
            @disconnect="openDisconnect"
        />

        <DisconnectConfirmSheet
            v-model="sheetOpen"
            :account="sheetTarget"
            :loading="disconnecting"
            @confirm="confirmDisconnect"
            @update:model-value="(open) => (open ? null : closeSheets())"
        />
    </div>
</template>

<style scoped>
.connected-accounts {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    min-height: 100vh;
    padding: var(--tt-space-6) var(--tt-space-5);
    padding-bottom: calc(var(--tt-tabbar-height) + var(--tt-space-6));
    background: var(--tt-bg-subtle);
    overflow: hidden;
}

.connected-accounts__title {
    position: relative;
    margin: 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.connected-accounts__summary {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-1);
    padding: var(--tt-space-5);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-primary-subtle);
}

.connected-accounts__summary-label {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-primary);
}

.connected-accounts__summary-value {
    margin: 0;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.connected-accounts__next {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.connected-accounts__section {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-2);
}

.connected-accounts__section-title {
    margin: 0;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.connected-accounts__section-meta {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.connected-accounts__error {
    margin: 0;
    padding: var(--tt-space-3);
    border-radius: var(--tt-radius-md);
    background: var(--tt-danger-subtle);
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

.connected-accounts__list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.connected-accounts__add {
    height: 56px;
    border: 1px dashed var(--tt-primary);
    border-radius: var(--tt-radius-lg);
    background: transparent;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    cursor: pointer;
}

.connected-accounts__add:hover {
    background: var(--tt-primary-subtle);
}
</style>
