<!--
  용도: "자산" 탭의 실제 진입 화면. 순자산 요약 · 자산 구성 · 계좌 목록을 순서대로 보여준다.
  언제 쓰는지: router/index.js 의 /asset 라우트. GET /api/assets/summary(이슈 #240) 결과를 보여준다.
-->
<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchAssetSummary } from '@/api/asset';
import AssetNetWorthCard from '@/components/asset/AssetNetWorthCard.vue';
import AssetCompositionCard from '@/components/asset/AssetCompositionCard.vue';
import AssetAccountList from '@/components/asset/AssetAccountList.vue';
import AssetLedgerToggle from '@/components/asset/AssetLedgerToggle.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import StateError from '@/components/common/StateError.vue';

const router = useRouter();
const summary = ref(null);
const loading = ref(false);
const errorMessage = ref('');

async function load() {
    loading.value = true;
    errorMessage.value = '';
    try {
        summary.value = await fetchAssetSummary();
    } catch (err) {
        errorMessage.value = err.message ?? '자산 정보를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

function goToTrend() {
    router.push({ name: 'assetNetWorthTrend' });
}

function goToConnectedAccounts() {
    router.push({ name: 'connectedAccounts' });
}

function goToLedger() {
    router.push({ name: 'ledger' });
}

onMounted(load);
</script>

<template>
    <div class="asset-home">
        <header class="asset-home__header">
            <h1 class="asset-home__title">내 자산</h1>
            <button type="button" class="asset-home__link" @click="goToConnectedAccounts">
                연결 관리 ›
            </button>
        </header>

        <StateLoading v-if="loading" message="자산 정보를 불러오는 중" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="load" />
        <template v-else-if="summary">
            <AssetNetWorthCard
                :net-worth="summary.netWorth"
                :month-over-month-change="summary.monthOverMonthChange"
                :trend="summary.trend"
                @view-trend="goToTrend"
            />
            <AssetCompositionCard :composition="summary.composition" />
            <AssetAccountList :accounts="summary.accounts" />
        </template>

        <AssetLedgerToggle active="asset" @open-ledger="goToLedger" />
    </div>
</template>

<style scoped>
.asset-home {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.asset-home__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.asset-home__title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.asset-home__link {
    /* 터치 영역을 넓히되, 음수 마진으로 글자 위치는 기존 span 과 같게 둔다. */
    margin-right: calc(var(--tt-space-2) * -1);
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
