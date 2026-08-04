<script setup>
import { computed, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
    fetchMonthlyConsumptionMonths,
    fetchMonthlyConsumptionReport,
} from '@/api/monthlyConsumption';
import MonthlyCategoryReport from '@/components/report/monthly-consumption/MonthlyCategoryReport.vue';
import MonthlyReportMonthPicker from '@/components/report/monthly-consumption/MonthlyReportMonthPicker.vue';
import MonthlyVerdictSummary from '@/components/report/monthly-consumption/MonthlyVerdictSummary.vue';
import ChallengeReportToggle from '@/components/challenge/report/ChallengeReportToggle.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import {
    formatPeriod,
    formatWon,
    getPreviousPeriod,
    resolveReportState,
} from '@/utils/monthlyConsumption';

const route = useRoute();
const router = useRouter();
const report = ref(null);
const months = ref([]);
const loading = ref(false);
const errorMessage = ref('');
const isMonthPickerOpen = ref(false);
const selectedPeriod = computed(() =>
    typeof route.query.month === 'string' ? route.query.month : getPreviousPeriod(),
);
const state = computed(() =>
    resolveReportState({ loading: loading.value, error: errorMessage.value, report: report.value }),
);
const trendMax = computed(() =>
    Math.max(...(report.value?.monthlyTrend.map((item) => item.amount) ?? [1])),
);

async function loadMonths() {
    months.value = await fetchMonthlyConsumptionMonths();
}

async function loadReport() {
    loading.value = true;
    errorMessage.value = '';
    report.value = null;
    try {
        report.value = await fetchMonthlyConsumptionReport(selectedPeriod.value);
    } catch (error) {
        errorMessage.value = error.message ?? '소비 리포트를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

function selectPeriod(period) {
    router.replace({ name: 'monthlyConsumptionReport', query: { month: period } });
}

function openChallengeReport() {
    router.push({ name: 'challengeReport', query: { month: selectedPeriod.value } });
}

function openSavingsStatement() {
    router.push({ name: 'fixedExpenseSavings' });
}

function openMonthlyReport() {
    router.push({ name: 'monthlyConsumptionReport', query: { month: selectedPeriod.value } });
}

onMounted(async () => {
    try {
        await loadMonths();
    } catch (error) {
        errorMessage.value = error.message ?? '조회 가능한 달을 불러오지 못했습니다.';
    }
});

watch(selectedPeriod, loadReport, { immediate: true });
</script>

<template>
    <article class="monthly-report">
        <header class="monthly-report__header">
            <h1>월간 판결문</h1>
            <button type="button" @click="isMonthPickerOpen = true">지난달 보기 ›</button>
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
        <template v-else>
            <p class="monthly-report__period">{{ formatPeriod(report.period) }}</p>
            <MonthlyVerdictSummary :report="report" />

            <BaseCard
                class="monthly-report__honor"
                clickable
                padding="none"
                @click="openChallengeReport"
            >
                <p>{{ Number(report.period.slice(5)) }}월 명예 법정</p>
                <strong
                    >{{ report.honorRank }}위
                    <span>상위 {{ report.honorPercentile }}%</span></strong
                >
                <b>자세히 ›</b>
            </BaseCard>

            <section class="monthly-report__message" aria-labelledby="message-title">
                <h2 id="message-title">탕탕이 한마디</h2>
                <p>{{ report.comment }}</p>
            </section>

            <section class="monthly-report__trend" aria-labelledby="trend-title">
                <h2 id="trend-title">최근 6개월</h2>
                <BaseCard padding="lg">
                    <p>
                        월 평균 <strong>{{ formatWon(report.averageSpent) }}</strong>
                    </p>
                    <div
                        class="monthly-report__chart"
                        role="img"
                        aria-label="최근 6개월 소비 그래프"
                    >
                        <div
                            v-for="item in report.monthlyTrend"
                            :key="item.month"
                            class="monthly-report__bar-item"
                        >
                            <span
                                :style="{
                                    height: `${Math.max(35, (item.amount / trendMax) * 100)}%`,
                                }"
                                :class="{
                                    'monthly-report__bar--current':
                                        item.month === Number(report.period.slice(5)),
                                }"
                            ></span>
                            <b
                                :class="{
                                    'monthly-report__month--current':
                                        item.month === Number(report.period.slice(5)),
                                }"
                                >{{ item.month }}월</b
                            >
                        </div>
                    </div>
                </BaseCard>
            </section>

            <BaseCard class="monthly-report__savings" padding="none">
                <div class="monthly-report__savings-content">
                    <div class="monthly-report__savings-title-row">
                        <h2>절약 감정서</h2>
                        <span aria-hidden="true"></span>
                    </div>
                    <p>절약 리포트를 확인해보세요</p>
                </div>
                <BaseButton class="monthly-report__savings-button" @click="openSavingsStatement">
                    확인하기
                </BaseButton>
            </BaseCard>

            <MonthlyCategoryReport :report="report" />
        </template>

        <MonthlyReportMonthPicker
            v-model="isMonthPickerOpen"
            :months="months"
            :selected-period="selectedPeriod"
            @select="selectPeriod"
        />
        <ChallengeReportToggle v-if="state === 'ready'" @open-monthly-report="openMonthlyReport" />
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
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-4);
}
.monthly-report__header h1 {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
}
.monthly-report__header button {
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
.monthly-report__honor {
    position: relative;
    overflow: hidden;
    padding: var(--tt-space-6) !important;
    color: var(--tt-text-inverse);
    background: var(--tt-primary);
    border: 0;
    border-radius: var(--tt-radius-xl);
}
.monthly-report__honor p {
    font-size: var(--tt-fs-body);
    color: var(--tt-brand-100);
}
.monthly-report__honor strong {
    display: block;
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
}
.monthly-report__honor strong span {
    margin-left: var(--tt-space-2);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-brand-100);
}
.monthly-report__honor b {
    position: absolute;
    right: var(--tt-space-6);
    bottom: var(--tt-space-6);
    color: var(--tt-accent);
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
    font-family: var(--tt-font-mono);
    color: var(--tt-text);
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
}
.monthly-report__bar-item span {
    width: min(42px, 70%);
    min-height: 30px;
    background: var(--tt-brand-200);
    border-radius: var(--tt-radius-md) var(--tt-radius-md) var(--tt-radius-sm) var(--tt-radius-sm);
}
.monthly-report__bar-item .monthly-report__bar--current {
    background: var(--tt-surface-strong);
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
@media (max-width: 360px) {
    .monthly-report {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }
    .monthly-report__honor strong {
        font-size: var(--tt-fs-numeric);
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
}
</style>
