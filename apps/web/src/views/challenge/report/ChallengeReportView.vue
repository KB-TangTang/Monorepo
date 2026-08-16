<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import {
    fetchChallengeReport,
    fetchChallengeReportMonths,
    fetchMockChallengeReport,
    fetchMockChallengeReportMonths,
} from '@/api/challengeReport';
import ChallengeMonthPicker from '@/components/challenge/report/ChallengeMonthPicker.vue';
import ChallengeReportContent from '@/components/challenge/report/ChallengeReportContent.vue';
import ChallengeReportToggle from '@/components/challenge/report/ChallengeReportToggle.vue';
import ChallengeSavingsGuide from '@/components/challenge/report/ChallengeSavingsGuide.vue';
import TempChallengeReportSourceToggle from '@/components/challenge/report/TempChallengeReportSourceToggle.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import { fetchMissionRankings } from '@/api/personalMission';
import {
    formatPeriod,
    getPreviousPeriod,
    getEmptyReportCopy,
    resolveChallengeReportState,
} from '@/utils/challengeReport';
import { hasSeenNetSavingsGuide, markNetSavingsGuideSeen } from '@/services/challengeReportGuide';
import { useChallengeReportStore } from '@/stores/challengeReport';

const emit = defineEmits([
    'change-difficulty',
    'open-monthly-report',
    'start-challenge',
]);

const route = useRoute();
const router = useRouter();
const challengeReportStore = useChallengeReportStore();
const report = ref(null);
const months = ref([]);
const selectedPeriod = ref('');
const loading = ref(true);
const errorMessage = ref('');
const isMonthPickerOpen = ref(false);
const isGuideOpen = ref(false);
const entryState = ref(null);

const state = computed(() =>
    resolveChallengeReportState({
        loading: loading.value,
        error: errorMessage.value,
        report: report.value,
        entryState: entryState.value,
    }),
);
const emptyCopy = computed(() => getEmptyReportCopy(report.value, entryState.value));
const selectedMonth = computed(() =>
    months.value.find((month) => month.value === selectedPeriod.value),
);
const pickerPeriod = computed(() => selectedPeriod.value || getPreviousPeriod());

async function loadMonths() {
    if (challengeReportStore.reportSource === 'mock') {
        entryState.value = null;
        months.value = await fetchMockChallengeReportMonths();
        return;
    }

    const availability = await fetchChallengeReportMonths();
    entryState.value = availability.entryState;
    months.value = availability.months ?? [];
}

async function loadReport() {
    loading.value = true;
    errorMessage.value = '';
    report.value = null;
    isGuideOpen.value = false;

    try {
        if (challengeReportStore.reportSource === 'mock') {
            report.value = await fetchMockChallengeReport(selectedPeriod.value);
        } else {
            const [challengeReport, ranking] = await Promise.all([
                fetchChallengeReport(selectedPeriod.value),
                fetchMissionRankings(selectedPeriod.value),
            ]);
            report.value = {
                ...challengeReport,
                ranking: ranking?.myRanking ?? null,
            };
        }
        if (
            resolveChallengeReportState({ report: report.value }) === 'ready' &&
            report.value.netSavings != null
        ) {
            isGuideOpen.value = !(await hasSeenNetSavingsGuide());
        }
    } catch (error) {
        errorMessage.value = error.message ?? '챌린지 리포트를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

async function initialize() {
    loading.value = true;
    errorMessage.value = '';
    report.value = null;
    isGuideOpen.value = false;
    try {
        await loadMonths();
        const requestedPeriod = typeof route.query.month === 'string' ? route.query.month : '';
        const requestedMonth = months.value.find(
            (month) => month.value === requestedPeriod && month.available,
        );
        const latestAvailableMonth = months.value.find((month) => month.available);
        const preparingMonth = months.value.find(
            (month) => month.status === 'PREPARING_FIRST_REPORT',
        );
        selectedPeriod.value =
            requestedMonth?.value ?? latestAvailableMonth?.value ?? preparingMonth?.value ?? '';

        if (
            !selectedPeriod.value &&
            !['NOT_AGREED', 'PREPARING_FIRST_REPORT'].includes(entryState.value)
        ) {
            throw new Error('조회할 수 있는 챌린지 리포트가 없습니다.');
        }
        if (selectedMonth.value?.available) {
            await loadReport();
        } else {
            loading.value = false;
        }
    } catch (error) {
        errorMessage.value = error.message ?? '조회 가능한 달을 불러오지 못했습니다.';
        loading.value = false;
    }
}

async function switchReportSource(source) {
    if (source === challengeReportStore.reportSource || loading.value) {
        return;
    }
    challengeReportStore.setReportSource(source);
    await initialize();
}

async function selectPeriod(period) {
    if (period === selectedPeriod.value) {
        return;
    }
    selectedPeriod.value = period;
    await loadReport();
}

async function acknowledgeGuide() {
    await markNetSavingsGuideSeen();
    isGuideOpen.value = false;
}

function openMonthlyReport() {
    router.push({
        name: 'monthlyConsumptionReport',
        query: selectedPeriod.value ? { month: selectedPeriod.value } : {},
    });
}

function openGroupHistory() {
    router.push({ name: 'groupChallengeList', query: { tab: 'ended' } });
}

onMounted(initialize);
</script>

<template>
    <article class="challenge-report">
        <header class="challenge-report__header">
            <h1>재판 보고서</h1>
            <button
                type="button"
                class="challenge-report__previous"
                @click="isMonthPickerOpen = true"
            >
                지난달 보기 ›
            </button>
        </header>

        <StateLoading
            v-if="state === 'loading'"
            size="lg"
            message="챌린지 기록을 정리하고 있어요"
        />
        <StateError v-else-if="state === 'error'" :message="errorMessage" @retry="initialize" />
        <section
            v-else-if="['empty', 'not-agreed', 'preparing'].includes(state)"
            class="challenge-report__empty"
        >
            <span v-if="selectedPeriod" class="challenge-report__period-pill">{{
                formatPeriod(selectedPeriod)
            }}</span>
            <StateEmpty :title="emptyCopy.title" :description="emptyCopy.description">
                <template #icon><span class="challenge-report__empty-circle"></span></template>
                <template #action>
                    <div class="challenge-report__empty-info">
                        <strong>성적표에서 볼 수 있는 것</strong>
                        <p>미션 성공률 · 카테고리별 절감액</p>
                        <p>난이도별 성과 · 그룹 전적</p>
                    </div>
                    <BaseButton
                        v-if="state !== 'preparing'"
                        block
                        size="lg"
                        @click="emit('start-challenge')"
                    >
                        첫 챌린지 시작하기
                    </BaseButton>
                </template>
            </StateEmpty>
        </section>
        <ChallengeReportContent
            v-else
            :report="report"
            :show-comparison="report.hasPreviousComparison ?? !selectedMonth?.firstReport"
            @change-difficulty="router.push({ name: 'personalMissionChallengeDifficulty' })"
            @open-group-history="openGroupHistory"
        />

        <ChallengeReportToggle
            active="trial"
            @open-monthly-report="openMonthlyReport"
            @open-trial-report="loadReport"
        />
        <ChallengeMonthPicker
            v-model="isMonthPickerOpen"
            :months="months"
            :selected-period="pickerPeriod"
            @select="selectPeriod"
        />
        <ChallengeSavingsGuide v-model="isGuideOpen" @understood="acknowledgeGuide" />
        <TempChallengeReportSourceToggle
            :source="challengeReportStore.reportSource"
            :loading="loading"
            elevated
            @toggle="switchReportSource"
        />
    </article>
</template>

<style scoped>
.challenge-report {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-6);
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-6) var(--tt-space-5) calc(var(--tt-space-12) * 3);
    background: var(--tt-bg-subtle);
}

.challenge-report__header {
    display: grid;
    grid-template-columns: 1fr auto;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-4);
}

.challenge-report__header h1 {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
}

.challenge-report__previous {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.challenge-report__period-pill {
    display: inline-block;
    margin-bottom: calc(var(--tt-space-4) * -1);
    padding: var(--tt-space-2) var(--tt-space-4);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-full);
}

.challenge-report__empty :deep(.tt-state) {
    min-height: 66vh;
    justify-content: flex-start;
    padding-top: var(--tt-space-12);
}

.challenge-report__empty :deep(.tt-state__icon) {
    width: 132px;
    height: 132px;
    margin-bottom: var(--tt-space-6);
    color: transparent;
    background: color-mix(in srgb, var(--tt-bg-subtle) 45%, var(--tt-border));
}

.challenge-report__empty :deep(.tt-state__title) {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
}

.challenge-report__empty :deep(.tt-state__desc) {
    white-space: pre-line;
}

.challenge-report__empty :deep(.tt-state__action) {
    width: 100%;
    margin-top: auto;
}

.challenge-report__empty-info {
    margin-bottom: var(--tt-space-5);
    padding: var(--tt-space-4);
    text-align: left;
    background: color-mix(in srgb, var(--tt-bg-subtle) 45%, var(--tt-border));
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.challenge-report__empty-info p {
    margin-top: var(--tt-space-2);
    color: var(--tt-text-muted);
}

@media (max-width: 360px) {
    .challenge-report {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }
}
</style>
