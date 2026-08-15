<!--
  용도: "순자산 추이" 상세 화면. 6개월 순자산/부채 막대 차트 + 선택한 달의 총자산·총부채를 보여준다.
  언제 쓰는지: router/index.js 의 /asset/trend 라우트. AssetHomeView 의 "추이 보기"에서 진입한다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchNetWorthTrend } from '@/api/asset';
import NetWorthTrendSummaryCard from '@/components/asset/NetWorthTrendSummaryCard.vue';
import NetWorthTrendChart from '@/components/asset/NetWorthTrendChart.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import StateError from '@/components/common/StateError.vue';
import { formatWon } from '@/utils/asset';

const router = useRouter();
const trendData = ref(null);
const loading = ref(false);
const errorMessage = ref('');
const selectedMonthIndex = ref(0);

async function load() {
    loading.value = true;
    errorMessage.value = '';
    try {
        trendData.value = await fetchNetWorthTrend();
        selectedMonthIndex.value = trendData.value.months.length - 1;
    } catch (err) {
        errorMessage.value = err.message ?? '순자산 추이를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

const selectedMonthLabel = computed(() => trendData.value?.months[selectedMonthIndex.value] ?? '');
// 선택한 달에 자산 스냅샷이 없으면 netWorth 가 null 로 들어온다 — "0원"이 아니라
// "데이터 없음"으로 구분해서 보여준다.
const selectedHasData = computed(
    () => (trendData.value?.netWorth[selectedMonthIndex.value] ?? null) !== null,
);
const selectedTotalDebt = computed(
    () => trendData.value?.totalDebt[selectedMonthIndex.value] ?? null,
);
const selectedTotalAssets = computed(() => {
    if (!selectedHasData.value) {
        return null;
    }
    return trendData.value.netWorth[selectedMonthIndex.value] + (selectedTotalDebt.value ?? 0);
});

function goBack() {
    router.push({ name: 'asset' });
}

onMounted(load);
</script>

<template>
    <div class="net-worth-trend">
        <header class="net-worth-trend__header">
            <button type="button" aria-label="자산 홈으로 돌아가기" @click="goBack">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m15 4-8 8 8 8" />
                </svg>
            </button>
            <h1>순자산 추이</h1>
        </header>

        <StateLoading v-if="loading" message="순자산 추이를 불러오는 중" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="load" />
        <template v-else-if="trendData">
            <NetWorthTrendSummaryCard :trend="trendData.netWorth" />

            <section class="net-worth-trend__chart-card">
                <NetWorthTrendChart
                    :months="trendData.months"
                    :net-worth="trendData.netWorth"
                    :total-debt="trendData.totalDebt"
                    v-model:selected-month-index="selectedMonthIndex"
                />
            </section>

            <div class="net-worth-trend__totals">
                <div class="net-worth-trend__totals-card">
                    <p class="net-worth-trend__totals-label">{{ selectedMonthLabel }} 총자산</p>
                    <p class="net-worth-trend__totals-value">
                        {{ selectedHasData ? formatWon(selectedTotalAssets) : '데이터 없음' }}
                    </p>
                </div>
                <div class="net-worth-trend__totals-card">
                    <p class="net-worth-trend__totals-label">{{ selectedMonthLabel }} 총부채</p>
                    <p class="net-worth-trend__totals-value net-worth-trend__totals-value--debt">
                        {{ selectedHasData ? formatWon(selectedTotalDebt) : '데이터 없음' }}
                    </p>
                </div>
            </div>

            <p class="net-worth-trend__footnote">
                월말 스냅샷 기준 · 데이터 누락 월은 미집계로 구분해요
            </p>
        </template>
    </div>
</template>

<style scoped>
.net-worth-trend {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.net-worth-trend__header {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    margin-bottom: var(--tt-space-2);
}

.net-worth-trend__header h1 {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.net-worth-trend__header button {
    display: grid;
    width: 32px;
    height: 40px;
    padding: 0;
    color: var(--tt-text);
    background: transparent;
    border: 0;
    cursor: pointer;
    place-items: center;
}

.net-worth-trend__header svg {
    width: 28px;
    height: 28px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.2;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.net-worth-trend__chart-card {
    padding: var(--tt-space-5);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.net-worth-trend__totals {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--tt-space-3);
}

.net-worth-trend__totals-card {
    padding: var(--tt-space-4);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
}

.net-worth-trend__totals-label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.net-worth-trend__totals-value {
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.net-worth-trend__totals-value--debt {
    color: var(--tt-danger);
}

.net-worth-trend__footnote {
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}
</style>
