<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchChallengeReport, fetchChallengeReportMonths } from '@/api/challengeReport';
import ChallengeMonthPicker from '@/components/challenge/report/ChallengeMonthPicker.vue';
import ChallengeDifficultySheet from '@/components/challenge/report/ChallengeDifficultySheet.vue';
import ChallengeReportContent from '@/components/challenge/report/ChallengeReportContent.vue';
import ChallengeReportOnboarding from '@/components/challenge/report/ChallengeReportOnboarding.vue';
import ChallengeReportToggle from '@/components/challenge/report/ChallengeReportToggle.vue';
import ChallengeSavingsGuide from '@/components/challenge/report/ChallengeSavingsGuide.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import { fetchMissionRankings } from '@/api/personalMission';
import { useAuthStore } from '@/stores/auth';
import { usePersonalMissionChallengeStore } from '@/stores/personalMission';
import {
    getPreviousPeriod,
    getEmptyReportCopy,
    resolveChallengeReportState,
} from '@/utils/challengeReport';
import { hasSeenNetSavingsGuide, markNetSavingsGuideSeen } from '@/services/challengeReportGuide';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const personalMissionStore = usePersonalMissionChallengeStore();
const report = ref(null);
const months = ref([]);
const selectedPeriod = ref('');
const loading = ref(true);
const errorMessage = ref('');
const isMonthPickerOpen = ref(false);
const isGuideOpen = ref(false);
const isDifficultySheetOpen = ref(false);
const isDifficultySaving = ref(false);
const difficultyError = ref('');
const entryState = ref(null);

const DIFFICULTY_NAME_BY_ID = { 1: 'EASY', 2: 'NORMAL', 3: 'HARD' };

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
const currentProsecutorId = computed(
    () =>
        DIFFICULTY_NAME_BY_ID[authStore.user?.difficultyId] ??
        personalMissionStore.selectedProsecutorId ??
        'NORMAL',
);

async function loadMonths() {
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
        const [challengeReport, ranking] = await Promise.all([
            fetchChallengeReport(selectedPeriod.value),
            fetchMissionRankings(selectedPeriod.value),
        ]);
        report.value = {
            ...challengeReport,
            ranking: ranking?.myRanking ?? null,
        };
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

function openDifficultySheet() {
    difficultyError.value = '';
    isDifficultySheetOpen.value = true;
}

async function changeDifficulty(prosecutorId) {
    if (isDifficultySaving.value) {
        return;
    }

    isDifficultySaving.value = true;
    difficultyError.value = '';
    try {
        const user = await personalMissionStore.saveProsecutorDifficulty(prosecutorId);
        authStore.mergeUser(user);
        isDifficultySheetOpen.value = false;
    } catch (error) {
        difficultyError.value = error.message ?? '담당 검사 난이도를 저장하지 못했어요.';
    } finally {
        isDifficultySaving.value = false;
    }
}

function startPersonalChallenge() {
    router.push({ name: 'personalMissionChallenge' });
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
        <ChallengeReportOnboarding
            v-else-if="['empty', 'not-agreed', 'preparing'].includes(state)"
            :state="state"
            :period="selectedPeriod"
            :title="emptyCopy.title"
            :description="emptyCopy.description"
            @start-challenge="startPersonalChallenge"
        />
        <ChallengeReportContent
            v-else
            :report="report"
            :show-comparison="report.hasPreviousComparison ?? !selectedMonth?.firstReport"
            @change-difficulty="openDifficultySheet"
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
        <ChallengeDifficultySheet
            v-model="isDifficultySheetOpen"
            :current-prosecutor-id="currentProsecutorId"
            :loading="isDifficultySaving"
            :error-message="difficultyError"
            @confirm="changeDifficulty"
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

@media (max-width: 360px) {
    .challenge-report {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }
}
</style>
