<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchChallengeReport, fetchChallengeReportMonths } from '@/api/challengeReport';
import ChallengePageHeader from '@/components/challenge/ChallengePageHeader.vue';
import ChallengeMonthPicker from '@/components/challenge/report/ChallengeMonthPicker.vue';
import ChallengeReportContent from '@/components/challenge/report/ChallengeReportContent.vue';
import ChallengeReportToggle from '@/components/challenge/report/ChallengeReportToggle.vue';
import ChallengeSavingsGuide from '@/components/challenge/report/ChallengeSavingsGuide.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import {
    formatPeriod,
    getEmptyReportCopy,
    resolveChallengeReportState,
} from '@/utils/challengeReport';
import { hasSeenNetSavingsGuide, markNetSavingsGuideSeen } from '@/services/challengeReportGuide';

const emit = defineEmits([
    'change-difficulty',
    'open-group-history',
    'open-monthly-report',
    'start-challenge',
]);

const route = useRoute();
const router = useRouter();
const report = ref(null);
const months = ref([]);
const selectedPeriod = ref('');
const loading = ref(true);
const errorMessage = ref('');
const isMonthPickerOpen = ref(false);
const isGuideOpen = ref(false);

const state = computed(() =>
    resolveChallengeReportState({
        loading: loading.value,
        error: errorMessage.value,
        report: report.value,
    }),
);
const emptyCopy = computed(() => getEmptyReportCopy(report.value));

async function loadReport() {
    loading.value = true;
    errorMessage.value = '';
    report.value = null;
    isGuideOpen.value = false;

    try {
        report.value = await fetchChallengeReport(selectedPeriod.value);
        if (resolveChallengeReportState({ report: report.value }) === 'ready') {
            isGuideOpen.value = !(await hasSeenNetSavingsGuide());
        }
    } catch (error) {
        errorMessage.value = error.message ?? '챌린지 리포트를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

async function initialize() {
    try {
        months.value = await fetchChallengeReportMonths();
        const requestedPeriod = typeof route.query.month === 'string' ? route.query.month : '';
        const requestedMonth = months.value.find(
            (month) => month.value === requestedPeriod && month.available,
        );
        const latestAvailableMonth = months.value.find((month) => month.available);
        selectedPeriod.value = requestedMonth?.value ?? latestAvailableMonth?.value ?? '';

        if (!selectedPeriod.value) {
            throw new Error('조회할 수 있는 챌린지 리포트가 없습니다.');
        }
        await loadReport();
    } catch (error) {
        errorMessage.value = error.message ?? '조회 가능한 달을 불러오지 못했습니다.';
        loading.value = false;
    }
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
    router.push({ name: 'monthlyConsumptionReport', query: { month: selectedPeriod.value } });
}

function goBack() {
    if (route.query.from === 'personalRanking') {
        router.push({
            name: 'personalRanking',
            query: { month: selectedPeriod.value },
        });
        return;
    }

    router.back();
}

onMounted(initialize);
</script>

<template>
    <article class="challenge-report">
        <ChallengePageHeader
            class="challenge-report__page-header"
            title="재판 보고서"
            @back="goBack"
        >
            <template v-if="selectedPeriod" #action>
                <button
                    type="button"
                    class="challenge-report__month-button"
                    @click="isMonthPickerOpen = true"
                >
                    지난달 보기 ›
                </button>
            </template>
        </ChallengePageHeader>

        <StateLoading
            v-if="state === 'loading'"
            size="lg"
            message="챌린지 기록을 정리하고 있어요"
        />
        <StateError v-else-if="state === 'error'" :message="errorMessage" @retry="initialize" />
        <section v-else-if="state === 'empty'" class="challenge-report__empty">
            <span class="challenge-report__period-pill">{{ formatPeriod(selectedPeriod) }}</span>
            <StateEmpty :title="emptyCopy.title" :description="emptyCopy.description">
                <template #icon><span class="challenge-report__empty-circle"></span></template>
                <template #action>
                    <div class="challenge-report__empty-info">
                        <strong>성적표에서 볼 수 있는 것</strong>
                        <p>미션 성공률 · 카테고리별 절감액</p>
                        <p>난이도별 성과 · 그룹 전적</p>
                    </div>
                    <BaseButton block size="lg" @click="emit('start-challenge')">
                        첫 챌린지 시작하기
                    </BaseButton>
                </template>
            </StateEmpty>
        </section>
        <ChallengeReportContent
            v-else
            :report="report"
            @change-difficulty="emit('change-difficulty')"
            @open-group-history="emit('open-group-history')"
        />

        <ChallengeReportToggle
            v-if="selectedPeriod"
            active="trial"
            @open-monthly-report="openMonthlyReport"
            @open-trial-report="loadReport"
        />
        <ChallengeMonthPicker
            v-if="selectedPeriod"
            v-model="isMonthPickerOpen"
            :months="months"
            :selected-period="selectedPeriod"
            @select="selectPeriod"
        />
        <ChallengeSavingsGuide v-model="isGuideOpen" @understood="acknowledgeGuide" />
    </article>
</template>

<style scoped>
.challenge-report {
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-6) var(--tt-space-5) calc(var(--tt-space-12) * 3);
    background: var(--tt-bg-subtle);
}

.challenge-report__page-header {
    margin-bottom: var(--tt-space-4);
}

.challenge-report__month-button {
    padding: 0;
    font: inherit;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.challenge-report__period-pill {
    display: inline-block;
    padding: var(--tt-space-1) var(--tt-space-3);
    font-weight: var(--tt-fw-bold);
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
