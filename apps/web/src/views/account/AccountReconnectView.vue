<script setup>
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import BaseButton from '@/components/common/BaseButton.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import LinkStepHeader from '@/components/account/LinkStepHeader.vue';
import { useAccountStore } from '@/stores/account';
import { formatSyncTime } from '@/utils/account';

const route = useRoute();
const router = useRouter();
const store = useAccountStore();
const { connectedAccounts, loading } = storeToRefs(store);

const account = computed(() =>
    connectedAccounts.value.find(
        (item) => String(item.accountId) === String(route.params.accountId),
    ),
);

const bankName = computed(() => account.value?.bankName ?? '이 계좌');

const STEPS = [
    '본인 인증을 다시 진행해요',
    '금융기관에 다시 로그인해 연결해요',
    '마지막 정상 시점 이후 거래를 다시 수집해요',
];

const lastNormal = computed(() =>
    account.value?.lastSyncAt ? formatSyncTime(account.value.lastSyncAt) : '',
);

onMounted(() => {
    if (!connectedAccounts.value.length) {
        store.loadConnectedAccounts().catch(() => {});
    }
});

function startReconnect() {
    /* 재연동은 그 기관을 처음부터 다시 인증하는 것이다. 연결 플로우 1단계로 보낸다. */
    store.resetFlow();
    router.push({ name: 'accountLinkInstitutions', query: { mode: 'add' } });
}

function later() {
    router.back();
}
</script>

<template>
    <div class="reconnect">
        <LinkStepHeader title="계좌 재연동" @back="later" />

        <StateLoading v-if="loading && !account" message="계좌 정보를 불러오는 중" />

        <template v-else>
            <div class="reconnect__body">
                <span class="reconnect__pill">재연동 필요</span>
                <h1 class="reconnect__title">{{ bankName }} 연결이<br />끊어졌어요</h1>
                <p class="reconnect__desc">
                    인증 유효기간이 만료돼 거래를<br />더 이상 가져오지 못하고 있어요.
                </p>
            </div>

            <!-- 순서가 있는 절차라 번호를 쓴다. -->
            <ol class="reconnect__steps">
                <li v-for="(step, index) in STEPS" :key="step" class="reconnect__step">
                    <span class="reconnect__step-no" aria-hidden="true">{{ index + 1 }}</span>
                    {{ step }}
                </li>
            </ol>

            <p class="reconnect__warn">
                재연동 전까지 <strong>{{ bankName }} 거래</strong>는 재판·장부에 반영되지 않아요.
                <template v-if="lastNormal">마지막 정상 수집은 {{ lastNormal }} 이에요.</template>
            </p>

            <div class="reconnect__cta">
                <BaseButton variant="dark" block size="lg" @click="startReconnect">
                    지금 재연동하기
                </BaseButton>
                <button class="reconnect__later" type="button" @click="later">나중에 하기</button>
            </div>
        </template>
    </div>
</template>

<style scoped>
.reconnect {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    min-height: 100vh;
    padding: var(--tt-space-5) var(--tt-space-5) var(--tt-space-8);
    background: var(--tt-bg-subtle);
    overflow: hidden;
}

/* 완료 화면과 같은 장치지만 색이 다르다 — 문제 상황이라 로즈 계열이다. */
.reconnect__body {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--tt-space-3);
    padding-top: var(--tt-space-8);
    text-align: center;
}

.reconnect__pill {
    padding: var(--tt-space-1) var(--tt-space-3);
    border-radius: var(--tt-radius-full);
    background: var(--tt-danger-subtle);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger);
}

.reconnect__title {
    margin: 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.reconnect__desc {
    margin: 0;
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.reconnect__steps {
    position: relative;
    display: flex;
    flex-direction: column;
    margin: var(--tt-space-4) 0 0;
    padding: var(--tt-space-2) var(--tt-space-5);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
    list-style: none;
}

.reconnect__step {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4) 0;
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
}

.reconnect__step + .reconnect__step {
    border-top: 1px solid var(--tt-border);
}

.reconnect__step-no {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-surface-strong);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-inverse);
}

.reconnect__warn {
    position: relative;
    margin: 0;
    padding: var(--tt-space-4);
    border-radius: var(--tt-radius-md);
    background: var(--tt-danger-subtle);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.reconnect__warn strong {
    color: var(--tt-danger);
}

.reconnect__cta {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--tt-space-2);
    margin-top: auto;
}

.reconnect__later {
    padding: var(--tt-space-2);
    border: 0;
    background: none;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    cursor: pointer;
}
</style>
