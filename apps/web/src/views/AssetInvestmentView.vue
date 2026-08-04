<!--
  용도: 자산 홈에서 "투자·증권" 항목을 눌렀을 때 진입하는 상세 화면. 총 평가금액 · 손익 + 보유 종목별 수익률을 보여준다.
  언제 쓰는지: router/index.js 의 /asset/investment 라우트. 백엔드 연동 전까지 api/asset.js 가 목업 데이터를 반환한다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { fetchInvestmentAccountDetail } from '@/api/asset';
import AssetDetailHeader from '@/components/asset/AssetDetailHeader.vue';
import AssetTotalCard from '@/components/asset/AssetTotalCard.vue';
import AssetHoldingRow from '@/components/asset/AssetHoldingRow.vue';
import BaseBadge from '@/components/common/BaseBadge.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import StateError from '@/components/common/StateError.vue';
import { formatWon, formatSignedWon, formatSignedPercent } from '@/utils/asset';

const detail = ref(null);
const loading = ref(false);
const errorMessage = ref('');

const gainAmount = computed(() =>
    detail.value ? detail.value.totalValuation - detail.value.totalCost : 0,
);
const gainRate = computed(() =>
    detail.value && detail.value.totalCost !== 0 ? gainAmount.value / detail.value.totalCost : 0,
);
const gainVariant = computed(() => (gainAmount.value < 0 ? 'guilty' : 'innocent'));

async function load() {
    loading.value = true;
    errorMessage.value = '';
    try {
        detail.value = await fetchInvestmentAccountDetail();
    } catch (err) {
        errorMessage.value = err.message ?? '투자증권 정보를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

onMounted(load);
</script>

<template>
    <div class="asset-investment">
        <AssetDetailHeader title="투자증권" />

        <StateLoading v-if="loading" message="투자증권 정보를 불러오는 중" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="load" />
        <template v-else-if="detail">
            <AssetTotalCard label="총 평가금액" :amount="detail.totalValuation">
                <div class="asset-investment__gain-row">
                    <BaseBadge :variant="gainVariant">
                        {{ formatSignedWon(gainAmount) }} ({{ formatSignedPercent(gainRate) }})
                    </BaseBadge>
                    <span class="asset-investment__cost"
                        >원금 {{ formatWon(detail.totalCost) }}</span
                    >
                </div>
            </AssetTotalCard>

            <section class="asset-investment__list">
                <div class="asset-investment__list-head">
                    <h2 class="asset-investment__title">보유 종목 {{ detail.holdings.length }}</h2>
                    <span class="asset-investment__as-of">시세 {{ detail.asOfLabel }} 기준</span>
                </div>
                <ul class="asset-investment__items">
                    <AssetHoldingRow
                        v-for="holding in detail.holdings"
                        :key="holding.code"
                        :badge="holding.badge"
                        :name="holding.name"
                        :quantity="holding.quantity"
                        :amount="holding.amount"
                        :gain-amount="holding.gainAmount"
                        :return-rate="holding.returnRate"
                        :tone="holding.tone"
                    />
                </ul>
            </section>

            <p class="asset-investment__notice">
                실시간 시세 미수신 시 마지막 수신 값으로 표시돼요
            </p>
        </template>
    </div>
</template>

<style scoped>
.asset-investment {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.asset-investment__gain-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.asset-investment__cost {
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-border-strong);
}

.asset-investment__list-head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-bottom: var(--tt-space-3);
}

.asset-investment__title {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.asset-investment__as-of {
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.asset-investment__items {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.asset-investment__notice {
    padding: var(--tt-space-3) var(--tt-space-4);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-md);
}
</style>
