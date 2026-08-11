<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
    fetchMonthlyConsumptionMonths,
    fetchMonthlyConsumptionReport,
} from '@/api/monthlyConsumption';
import MonthlyCategoryReport from '@/components/report/monthly-consumption/MonthlyCategoryReport.vue';
import MonthlyReportOnboarding from '@/components/report/monthly-consumption/MonthlyReportOnboarding.vue';
import MonthlyReportMonthPicker from '@/components/report/monthly-consumption/MonthlyReportMonthPicker.vue';
import MonthlySavingsCompleteTicket from '@/components/report/monthly-consumption/MonthlySavingsCompleteTicket.vue';
import MonthlyVerdictSummary from '@/components/report/monthly-consumption/MonthlyVerdictSummary.vue';
import ChallengeReportToggle from '@/components/challenge/report/ChallengeReportToggle.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import savingTangi from '@/assets/images/emotions/42_thumbs_up.png';
import { useAuthStore } from '@/stores/auth';
import {
    buildMonthlyTrendSlots,
    formatPeriod,
    formatWon,
    resolveSelectedReportPeriod,
    resolveReportState,
    resolveFixedExpenseStatus,
} from '@/utils/monthlyConsumption';
import { resolveDisplayName } from '@/utils/user';

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const report = ref(null);
const months = ref([]);
const loading = ref(true);
const errorMessage = ref('');
const isMonthPickerOpen = ref(false);
const selectedPeriod = ref('');
const selectedTrendMonth = ref(null);
const state = computed(() =>
    resolveReportState({ loading: loading.value, error: errorMessage.value, report: report.value }),
);
const displayName = computed(() => resolveDisplayName(auth.user) || '고객');
const isFirstReport = computed(() => state.value === 'first-report');
const fixedExpenseStatus = computed(() =>
    resolveFixedExpenseStatus(report.value?.fixedExpenseCandidates),
);
const trendMax = computed(() =>
    Math.max(...(report.value?.monthlyTrend?.map((item) => item.amount) ?? [1])),
);
const trendMin = computed(() =>
    Math.min(...(report.value?.monthlyTrend?.map((item) => item.amount) ?? [0])),
);
const selectedTrend = computed(
    () =>
        report.value?.monthlyTrend?.find((item) => item.month === selectedTrendMonth.value) ??
        report.value?.monthlyTrend?.at(-1) ??
        null,
);
const trendMonths = computed(() => {
    return buildMonthlyTrendSlots(report.value?.period, report.value?.monthlyTrend);
});

async function loadMonths() {
    months.value = await fetchMonthlyConsumptionMonths();
}

async function loadReport() {
    loading.value = true;
    errorMessage.value = '';
    report.value = null;
    try {
        report.value = await fetchMonthlyConsumptionReport(selectedPeriod.value);
        selectedTrendMonth.value = Number(selectedPeriod.value.slice(5));
    } catch (error) {
        errorMessage.value = error.message ?? '소비 리포트를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

function selectPeriod(period) {
    if (period === selectedPeriod.value) {
        return;
    }

    selectedPeriod.value = period;
    router.replace({ name: 'monthlyConsumptionReport', query: { month: period } });
    loadReport();
}

function openSavingsStatement() {
    router.push({ name: 'fixedExpenseSavings' });
}

function openMonthlyReport() {
    router.push({ name: 'monthlyConsumptionReport', query: { month: selectedPeriod.value } });
}

function openTrialReport() {
    router.push({ name: 'challengeReport', query: { month: selectedPeriod.value } });
}

function selectTrend(month) {
    selectedTrendMonth.value = month;
}

function getTrendBarHeight(amount) {
    if (trendMax.value === trendMin.value) {
        return 65;
    }

    return 35 + ((amount - trendMin.value) / (trendMax.value - trendMin.value)) * 65;
}

onMounted(async () => {
    try {
        await loadMonths();
        const requestedPeriod = typeof route.query.month === 'string' ? route.query.month : '';
        selectedPeriod.value = resolveSelectedReportPeriod(months.value, requestedPeriod);
        if (!selectedPeriod.value) {
            throw new Error('조회 가능한 소비 리포트가 없습니다.');
        }
        await loadReport();
    } catch (error) {
        errorMessage.value = error.message ?? '조회 가능한 달을 불러오지 못했습니다.';
        loading.value = false;
    }
});
</script>

<template>
    <article class="monthly-report">
        <header class="monthly-report__header">
            <button
                type="button"
                class="monthly-report__back"
                aria-label="뒤로 가기"
                @click="router.back()"
            ></button>
            <h1>월간 판결문</h1>
            <button
                type="button"
                class="monthly-report__previous"
                @click="isMonthPickerOpen = true"
            >
                지난달 보기 ›
            </button>
        </header>

        <StateLoading
            v-if="state === 'loading'"
            size="lg"
            message="이번 달 소비를 정리하고 있어요"
        />
        <StateError v-else-if="state === 'error'" :message="errorMessage" @retry="loadReport" />
        <StateEmpty
            v-else-if="state === 'empty'"
            title="아직 판결문이 없어요"
            description="한 달의 소비 기록이 모이면 월간 판결문을 준비해 드릴게요."
        />
        <template v-else-if="state === 'onboarding'">
            <p class="monthly-report__period">{{ formatPeriod(report.period) }}</p>
            <MonthlyReportOnboarding :display-name="displayName" />
        </template>
        <template v-else>
            <p class="monthly-report__period">{{ formatPeriod(report.period) }}</p>
            <MonthlyVerdictSummary :report="report" :show-comparison="!isFirstReport" />

            <section class="monthly-report__message" aria-labelledby="message-title">
                <h2 id="message-title">탕이의 한마디</h2>
                <p>{{ report.comment }}</p>
            </section>

            <section class="monthly-report__trend" aria-labelledby="trend-title">
                <h2 id="trend-title">최근 6개월</h2>
                <BaseCard v-if="isFirstReport" class="monthly-report__first-flow" padding="lg">
                    <div class="monthly-report__first-flow-head">
                        <p>
                            {{ Number(report.period.slice(5)) }}월 소비
                            <strong>{{ formatWon(report.totalSpent) }}</strong>
                        </p>
                        <span>첫 번째 기록</span>
                    </div>
                    <div class="monthly-report__first-flow-chart" aria-label="첫 소비 기록 흐름">
                        <span
                            v-for="item in trendMonths"
                            :key="item.month"
                            :class="{ 'monthly-report__first-flow-month--active': item.active }"
                        >
                            <i></i>{{ item.month }}월
                        </span>
                    </div>
                    <p class="monthly-report__first-flow-note">
                        이번 달부터 소비 흐름을 차곡차곡 쌓아갈게요
                    </p>
                </BaseCard>
                <BaseCard v-else padding="lg">
                    <p>
                        {{ selectedTrend?.month }}월 소비
                        <strong>{{
                            formatWon(selectedTrend?.amount ?? report.averageSpent)
                        }}</strong>
                    </p>
                    <div class="monthly-report__chart" aria-label="최근 6개월 소비 그래프">
                        <button
                            v-for="item in trendMonths"
                            :key="item.month"
                            type="button"
                            class="monthly-report__bar-item"
                            :class="{
                                'monthly-report__bar-item--selected':
                                    item.month === selectedTrend?.month,
                                'monthly-report__bar-item--empty': !item.hasData,
                            }"
                            :disabled="!item.hasData"
                            :aria-pressed="item.hasData && item.month === selectedTrend?.month"
                            :aria-label="
                                item.hasData
                                    ? `${item.month}월 소비 ${formatWon(item.amount)}`
                                    : `${item.month}월 소비 내역 없음`
                            "
                            @click="selectTrend(item.month)"
                        >
                            <span
                                :style="
                                    item.hasData
                                        ? { height: `${getTrendBarHeight(item.amount)}%` }
                                        : undefined
                                "
                                :class="{
                                    'monthly-report__bar--current':
                                        item.month === selectedTrend?.month,
                                }"
                            ></span>
                            <b
                                :class="{
                                    'monthly-report__month--current':
                                        item.month === selectedTrend?.month,
                                }"
                                >{{ item.month }}월</b
                            >
                        </button>
                    </div>
                </BaseCard>
            </section>

            <MonthlySavingsCompleteTicket v-if="fixedExpenseStatus === 'clear'" />
            <BaseCard v-else class="monthly-report__savings" padding="none">
                <div class="monthly-report__savings-content">
                    <div class="monthly-report__savings-title-row">
                        <h2>절약 감정서</h2>
                        <span aria-hidden="true"></span>
                    </div>
                    <p v-if="fixedExpenseStatus === 'detected'">
                        고정 지출로 의심되는 내역이
                        {{ report.fixedExpenseCandidates.length }}건 있어요
                    </p>
                    <p v-else>절약 리포트를 확인해보세요</p>
                </div>
                <BaseButton class="monthly-report__savings-button" @click="openSavingsStatement">
                    확인하기
                </BaseButton>
            </BaseCard>

            <MonthlyCategoryReport :report="report" :show-comparison="!isFirstReport" />

            <aside v-if="isFirstReport" class="monthly-report__start-saving">
                <h2>탕이와 함께<br />절약해봐요</h2>
                <p>이번 달 소비를 첫 기준으로 삼아<br />다음 달부터 변화를 알려드릴게요.</p>
                <img :src="savingTangi" alt="엄지를 들어 응원하는 탕이" />
            </aside>
        </template>

        <MonthlyReportMonthPicker
            v-model="isMonthPickerOpen"
            :months="months"
            :selected-period="selectedPeriod"
            @select="selectPeriod"
        />
        <ChallengeReportToggle
            v-if="['onboarding', 'first-report', 'ready'].includes(state)"
            active="monthly"
            @open-monthly-report="openMonthlyReport"
            @open-trial-report="openTrialReport"
        />
    </article>
</template>

<style scoped>
.monthly-report {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-6);
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-6) var(--tt-space-5) var(--tt-space-12);
    background: var(--tt-bg-subtle);
}
.monthly-report__header {
    display: grid;
    grid-template-columns: 44px 1fr auto;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-4);
}
.monthly-report__back {
    display: grid;
    width: 44px;
    height: 44px;
    margin-left: calc(var(--tt-space-2) * -1);
    padding: 0;
    background: transparent;
    border: 0;
    cursor: pointer;
    place-items: center;
}
.monthly-report__back::before {
    width: 14px;
    height: 14px;
    content: '';
    border-bottom: 2.5px solid var(--tt-text);
    border-left: 2.5px solid var(--tt-text);
    transform: rotate(45deg);
}
.monthly-report__header h1 {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
}
.monthly-report__previous {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    background: transparent;
    border: 0;
    cursor: pointer;
}
.monthly-report__period {
    align-self: flex-start;
    margin-bottom: calc(var(--tt-space-4) * -1);
    padding: var(--tt-space-2) var(--tt-space-4);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-full);
}
.monthly-report__message {
    overflow: hidden;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
}
.monthly-report__message h2 {
    padding: var(--tt-space-4) var(--tt-space-5);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
    border-bottom: 1px solid var(--tt-border);
}
.monthly-report__message p {
    position: relative;
    padding: var(--tt-space-5) var(--tt-space-5) var(--tt-space-5)
        calc(var(--tt-space-5) + var(--tt-space-4));
    font-size: var(--tt-fs-body);
    line-height: var(--tt-lh-normal);
}
.monthly-report__message p::before {
    position: absolute;
    left: var(--tt-space-5);
    content: '•';
    color: var(--tt-border-strong);
}
.monthly-report__trend {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}
.monthly-report__trend h2 {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}
.monthly-report__trend p {
    color: var(--tt-text-soft);
}
.monthly-report__trend p strong {
    display: block;
    font-family: var(--tt-font-mono);
    color: var(--tt-text);
}
.monthly-report__first-flow-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--tt-space-4);
}
.monthly-report__first-flow-head > span {
    padding: var(--tt-space-2) var(--tt-space-3);
    color: var(--tt-accent-strong);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
    white-space: nowrap;
}
.monthly-report__first-flow-chart {
    display: grid;
    grid-template-columns: repeat(6, 1fr);
    align-items: end;
    gap: var(--tt-space-2);
    height: 126px;
    margin-top: var(--tt-space-4);
}
.monthly-report__first-flow-chart > span {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: flex-end;
    gap: var(--tt-space-2);
    height: 100%;
    color: var(--tt-border-strong);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}
.monthly-report__first-flow-chart i {
    width: 100%;
    max-width: 38px;
    height: 8px;
    background: repeating-linear-gradient(90deg, var(--tt-border) 0 4px, transparent 4px 7px);
    border-radius: var(--tt-radius-md);
}
.monthly-report__first-flow-chart .monthly-report__first-flow-month--active {
    color: var(--tt-text);
}
.monthly-report__first-flow-chart .monthly-report__first-flow-month--active i {
    height: 92px;
    background: linear-gradient(var(--tt-surface-strong-deep), var(--tt-surface-strong));
    box-shadow: var(--tt-elevation-2);
}
.monthly-report__first-flow .monthly-report__first-flow-note {
    margin-top: var(--tt-space-4);
    padding-top: var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    text-align: center;
    border-top: 1px solid var(--tt-border);
}
.monthly-report__chart {
    display: flex;
    align-items: end;
    justify-content: space-around;
    height: 148px;
    margin-top: var(--tt-space-5);
}
.monthly-report__bar-item {
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    justify-content: end;
    gap: var(--tt-space-2);
    height: 100%;
    padding: 0;
    cursor: pointer;
    background: transparent;
    border: 0;
}
.monthly-report__bar-item span {
    width: min(42px, 70%);
    min-height: 30px;
    background: var(--tt-brand-200);
    border-radius: var(--tt-radius-md) var(--tt-radius-md) var(--tt-radius-sm) var(--tt-radius-sm);
    transform-origin: bottom center;
    animation: monthly-report-bar-fill 640ms ease-out both;
}
.monthly-report__bar-item--empty {
    cursor: default;
}
.monthly-report__bar-item--empty span {
    height: 8px;
    min-height: 8px;
    background: repeating-linear-gradient(90deg, var(--tt-border) 0 4px, transparent 4px 7px);
    box-shadow: none;
    animation: none;
}

.monthly-report__bar-item:nth-child(2) span {
    animation-delay: 80ms;
}

.monthly-report__bar-item:nth-child(3) span {
    animation-delay: 160ms;
}

.monthly-report__bar-item:nth-child(4) span {
    animation-delay: 240ms;
}

.monthly-report__bar-item:nth-child(5) span {
    animation-delay: 320ms;
}

.monthly-report__bar-item:nth-child(6) span {
    animation-delay: 400ms;
}

@keyframes monthly-report-bar-fill {
    from {
        transform: scaleY(0);
    }

    to {
        transform: scaleY(1);
    }
}

@media (prefers-reduced-motion: reduce) {
    .monthly-report__bar-item span {
        animation: none;
    }
}
.monthly-report__bar-item .monthly-report__bar--current {
    background: var(--tt-surface-strong);
}
.monthly-report__bar-item--selected .monthly-report__bar--current {
    box-shadow: 0 0 0 3px var(--tt-primary-subtle);
}
.monthly-report__bar-item b {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-soft);
}
.monthly-report__bar-item .monthly-report__month--current {
    color: var(--tt-text);
    font-weight: var(--tt-fw-black);
}
.monthly-report__savings {
    position: relative;
    overflow: hidden;
    min-height: 138px;
    padding: 0 !important;
    color: var(--tt-text-inverse);
    background: var(--tt-primary);
    border: 0;
    border-radius: var(--tt-radius-xl);
}
.monthly-report__savings :deep(.tt-card__body) {
    display: grid;
    grid-template-columns: minmax(0, 1fr) auto;
    align-items: center;
    gap: var(--tt-space-3);
    min-height: 138px;
    padding: var(--tt-space-4) var(--tt-space-6) var(--tt-space-4) var(--tt-space-8);
}
.monthly-report__savings::before,
.monthly-report__savings::after {
    position: absolute;
    top: 50%;
    width: 28px;
    height: 28px;
    content: '';
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-full);
    transform: translateY(-50%);
}
.monthly-report__savings::before {
    left: -12px;
}
.monthly-report__savings::after {
    right: -12px;
}
.monthly-report__savings-content {
    position: relative;
    z-index: var(--tt-z-base);
    transform: translateY(-10px);
}
.monthly-report__savings-title-row {
    position: relative;
}
.monthly-report__savings-title-row span {
    position: absolute;
    top: 50%;
    right: -112px;
    left: 132px;
    border-top: 2px dashed color-mix(in srgb, var(--tt-primary) 55%, var(--tt-bg));
}
.monthly-report__savings h2 {
    position: relative;
    z-index: var(--tt-z-base);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
}
.monthly-report__savings p {
    margin-top: var(--tt-space-3);
    font-size: var(--tt-fs-body);
    color: var(--tt-brand-100);
}
.monthly-report__savings-button {
    z-index: var(--tt-z-base);
    flex: none;
    width: 100px;
    height: 44px;
    padding: 0;
    color: var(--tt-text) !important;
    background: var(--tt-accent) !important;
    border-radius: var(--tt-radius-lg);
    cursor: pointer;
    transform: translateY(20px);
}
.monthly-report__start-saving {
    position: relative;
    min-height: 148px;
    overflow: hidden;
    padding: var(--tt-space-6) 138px var(--tt-space-6) var(--tt-space-5);
    background: linear-gradient(135deg, var(--tt-bg), var(--tt-accent-subtle));
    border: 1px solid var(--tt-accent-subtle-border);
    border-radius: var(--tt-radius-xl);
}
.monthly-report__start-saving::after {
    position: absolute;
    right: -30px;
    bottom: -68px;
    width: 178px;
    height: 178px;
    content: '';
    background: color-mix(in srgb, var(--tt-accent) 18%, transparent);
    border-radius: var(--tt-radius-full);
}
.monthly-report__start-saving h2,
.monthly-report__start-saving p {
    position: relative;
    z-index: 1;
}
.monthly-report__start-saving h2 {
    font-size: var(--tt-fs-section);
    line-height: 1.35;
    font-weight: var(--tt-fw-black);
}
.monthly-report__start-saving p {
    margin-top: var(--tt-space-2);
    color: var(--tt-accent-strong);
    font-size: var(--tt-fs-caption);
    line-height: 1.6;
}
.monthly-report__start-saving img {
    position: absolute;
    right: var(--tt-space-2);
    bottom: -4px;
    z-index: 1;
    width: 134px;
    height: 134px;
    object-fit: contain;
}
@media (max-width: 360px) {
    .monthly-report {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }
    .monthly-report__savings {
        min-height: 138px;
    }
    .monthly-report__savings :deep(.tt-card__body) {
        gap: var(--tt-space-3);
        min-height: 138px;
        padding-right: var(--tt-space-5);
        padding-left: var(--tt-space-5);
    }
    .monthly-report__savings-button {
        width: 92px;
    }
    .monthly-report__start-saving {
        padding-right: 112px;
    }
    .monthly-report__start-saving img {
        right: -4px;
        width: 120px;
        height: 120px;
    }
}
</style>
