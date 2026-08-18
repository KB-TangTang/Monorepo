<!--
  용도: 자산 홈에서 "투자·증권" 항목을 눌렀을 때 진입하는 상세 화면. 총 평가금액 · 손익 + 보유 종목별 수익률을 보여준다.
  언제 쓰는지: router/index.js 의 /asset/investment 라우트. 백엔드 연동 전까지 api/asset.js 가 목업 데이터를 반환한다.
-->
<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { fetchInvestmentAccountDetail } from '@/api/asset';
import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import AssetTotalCard from '@/components/asset/AssetTotalCard.vue';
import AssetHoldingRow from '@/components/asset/AssetHoldingRow.vue';
import BaseBadge from '@/components/common/BaseBadge.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import StateError from '@/components/common/StateError.vue';
import { formatWon, formatSignedWon, formatSignedPercent } from '@/utils/asset';

/*
 * 화면이 열려 있는 동안 시세를 폴링한다. 백엔드 InvestmentPriceRefresher 가 심볼별로
 * 20초(toss.price.cache-ttl-ms) 캐시를 두고 있어, 이보다 짧게 물어봐야 매번 토스를 새로
 * 부르는 게 아니라 그냥 캐시된 값을 받는다 — 그래서 자주 불러도 백엔드 부담이 늘지 않는다.
 */
const POLL_INTERVAL_MS = 8000;

const detail = ref(null);
const loading = ref(false);
const errorMessage = ref('');
let pollTimer = null;

/*
 * 요청 순번 가드(QA 지적사항). setInterval 은 이전 요청이 끝나길 기다리지 않으므로, 느린 요청과
 * 빠른 요청이 겹치면 늦게 도착한(더 오래된) 응답이 이미 반영된 최신 데이터를 덮어쓸 수 있다.
 * 요청을 보낼 때마다 순번을 올려 캡처해두고, 응답이 왔을 때 그 순번이 아직도 최신인지 확인한 뒤
 * 최신일 때만 반영한다 — 더 최신 요청이 이미 나갔다면 이 응답은 버린다.
 */
let requestSeq = 0;

const gainAmount = computed(() =>
    detail.value ? detail.value.totalValuation - detail.value.totalCost : 0,
);
const gainRate = computed(() =>
    detail.value && detail.value.totalCost !== 0 ? gainAmount.value / detail.value.totalCost : 0,
);
const gainVariant = computed(() => (gainAmount.value < 0 ? 'guilty' : 'innocent'));

async function load() {
    const seq = ++requestSeq;
    loading.value = true;
    errorMessage.value = '';
    try {
        const next = await fetchInvestmentAccountDetail();
        if (seq !== requestSeq) return; // 더 최신 요청이 이미 나갔다 — 이 응답은 버린다
        detail.value = next;
    } catch (err) {
        if (seq !== requestSeq) return;
        errorMessage.value = err.message ?? '투자증권 정보를 불러오지 못했습니다.';
    } finally {
        if (seq === requestSeq) loading.value = false;
    }
}

/*
 * 폴링 전용 재조회. load() 와 달리 loading 을 건드리지 않는다 — 8초마다 전체 화면이 스피너로
 * 바뀌면 오히려 거슬린다. 실패해도 errorMessage 를 띄우지 않고 마지막으로 받은 값을 그대로
 * 둔다("실시간 시세 미수신 시 마지막 수신 값으로 표시돼요" 안내와 같은 원칙) — 다음 폴링에서
 * 다시 시도한다. 단, 이 폴링으로 회복되면(재연결 등) 남아있던 에러 배너는 지운다.
 */
async function pollQuietly() {
    const seq = ++requestSeq;
    try {
        const next = await fetchInvestmentAccountDetail();
        if (seq !== requestSeq) return; // 더 최신 요청이 이미 나갔다 — 이 응답은 버린다
        detail.value = next;
        errorMessage.value = '';
    } catch {
        // 조용히 무시 — 다음 폴링에서 다시 시도한다.
    }
}

function stopPolling() {
    if (pollTimer) {
        clearInterval(pollTimer);
        pollTimer = null;
    }
}

function startPolling() {
    stopPolling();
    pollTimer = setInterval(pollQuietly, POLL_INTERVAL_MS);
}

onMounted(() => {
    load();
    startPolling();
});
onUnmounted(stopPolling);
</script>

<template>
    <div class="asset-investment">
        <BaseBackHeader title="투자증권" back-label="자산 홈으로 돌아가기" />

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
