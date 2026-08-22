<!--
  용도: 홈 탭. 「오늘 쓴 돈 → 오늘의 미션 → 진행 중인 재판 → 나의 기록 → 자산 → 명예의 전당 →
        다가오는 고정지출」 순으로 오늘 확인할 것을 위에서 아래로 늘어놓는다 (이슈 #450).
  이 파일은 데이터를 모아 컴포넌트에 넘기고 이동만 시킨다 — 표시 규칙은 components/home/* 과
  utils/home.js 에 있다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchAssetSummary } from '@/api/asset';
import { fetchChallengeReport, fetchChallengeReportMonths } from '@/api/challengeReport';
import { fetchFixedExpenseOverview } from '@/api/fixedExpense';
import { fetchMyGroupChallenges, fetchMyTrials } from '@/api/groupChallenge';
import { fetchHomeSpendingSummary } from '@/api/ledger';
import { fetchMissionStreak, fetchTodayMission } from '@/api/personalMission';
import PersonalMissionHonorBanner from '@/components/challenge/personal/PersonalMissionHonorBanner.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import TheNotificationBell from '@/components/common/TheNotificationBell.vue';
import HomeAssetCard from '@/components/home/HomeAssetCard.vue';
import HomeFixedExpenseList from '@/components/home/HomeFixedExpenseList.vue';
import HomeMissionBubble from '@/components/home/HomeMissionBubble.vue';
import HomeRecordStats from '@/components/home/HomeRecordStats.vue';
import HomeSpendingCard from '@/components/home/HomeSpendingCard.vue';
import HomeTrialCard from '@/components/home/HomeTrialCard.vue';
import {
    formatHomeAmount,
    getCurrentYearMonth,
    getHomeGroupTrialRow,
    getHomePersonalTrialRow,
    getHomeReportEmptyCopy,
    getHomeWeeklyVerdicts,
    toHomeMission,
    toHomeReportSummary,
    toHomeSpending,
} from '@/utils/home';

const router = useRouter();

const isLoading = ref(true);
const errorMessage = ref('');
const mission = ref(null);
const spending = ref(toHomeSpending(null));
const streak = ref(null);
const assetSummary = ref(null);
const fixedExpense = ref(null);
const habitSummary = ref(null);
const reportStatus = ref('loading');
const groupTrials = ref([]);
const activeGroupCount = ref(0);
const hasGroupSummaryError = ref(false);

const currentPeriod = getCurrentYearMonth();

const personalTrialRow = computed(() =>
    getHomePersonalTrialRow(mission.value, streak.value?.streakCount),
);
const groupTrialRow = computed(() =>
    getHomeGroupTrialRow({
        trials: groupTrials.value,
        activeGroupCount: activeGroupCount.value,
        failed: hasGroupSummaryError.value,
    }),
);
const weeklyVerdicts = computed(() =>
    streak.value ? getHomeWeeklyVerdicts(streak.value.weeklyResults) : null,
);
const reportEmptyCopy = computed(() => getHomeReportEmptyCopy(reportStatus.value));

async function loadHome() {
    isLoading.value = true;
    errorMessage.value = '';

    const results = await Promise.allSettled([
        fetchTodayMission(),
        fetchMissionStreak(),
        fetchAssetSummary(),
        fetchFixedExpenseOverview(),
        loadLatestReport(),
        fetchHomeSpendingSummary(),
    ]);
    const [missionResult, streakResult, assetResult, fixedResult, reportResult, spendingResult] =
        results;

    mission.value =
        missionResult.status === 'fulfilled' ? toHomeMission(missionResult.value) : null;
    /* 실패하면 세 값이 전부 null 이 되어 카드가 「—」·「집계 중」으로 버틴다 — 나머지 홈은 그대로 뜬다. */
    spending.value = toHomeSpending(
        spendingResult.status === 'fulfilled' ? spendingResult.value : null,
    );
    streak.value = streakResult.status === 'fulfilled' ? streakResult.value : null;
    assetSummary.value = assetResult.status === 'fulfilled' ? assetResult.value : null;
    fixedExpense.value = fixedResult.status === 'fulfilled' ? fixedResult.value : null;
    habitSummary.value = reportResult.status === 'fulfilled' ? reportResult.value.summary : null;
    reportStatus.value = reportResult.status === 'fulfilled' ? reportResult.value.status : 'error';

    if (results.every((result) => result.status === 'rejected')) {
        errorMessage.value = '홈 정보를 불러오지 못했습니다.';
    }
    isLoading.value = false;
}

async function loadLatestReport() {
    const availability = await fetchChallengeReportMonths();
    const latestMonth = availability.months?.find((month) => month.available);
    if (!latestMonth) {
        return {
            status:
                availability.entryState === 'NOT_AGREED'
                    ? 'not-agreed'
                    : availability.entryState === 'PREPARING_FIRST_REPORT'
                      ? 'preparing'
                      : 'empty',
            summary: null,
        };
    }

    const summary = toHomeReportSummary(await fetchChallengeReport(latestMonth.value));
    return { status: summary ? 'ready' : 'empty', summary };
}

/*
 * 그룹 요약은 홈 전체 로딩과 분리한다 — 재판 목록이 느려도 위쪽 카드가 같이 멈추면 안 된다.
 * 조회에 실패하면 「할 일 없음」이 아니라 실패를 그대로 알린다.
 */
async function loadGroupSummary() {
    hasGroupSummaryError.value = false;

    const [trialsResult, activeGroupsResult] = await Promise.allSettled([
        fetchMyTrials(),
        fetchMyGroupChallenges(['ACTIVE']),
    ]);

    groupTrials.value = trialsResult.status === 'fulfilled' ? trialsResult.value : [];
    activeGroupCount.value =
        activeGroupsResult.status === 'fulfilled' ? activeGroupsResult.value.length : 0;

    const cannotDecideEmptyState =
        groupTrials.value.length === 0 && activeGroupsResult.status === 'rejected';
    hasGroupSummaryError.value = trialsResult.status === 'rejected' || cannotDecideEmptyState;
}

function goToLedger() {
    router.push({ name: 'ledger' });
}

function goToFixedExpense() {
    router.push({ name: 'fixedExpenseManagement' });
}

function goToFixedExpenseDetail(id) {
    router.push({ name: 'fixedExpenseDetail', params: { id } });
}

function goToPersonalChallenge() {
    router.push({ name: 'personalMissionChallenge' });
}

function goToGroupChallenge() {
    if (hasGroupSummaryError.value) {
        loadGroupSummary();
        return;
    }

    router.push({ name: 'groupChallengeList' });
}

function goToTrialRecords() {
    router.push({ name: 'monthlyConsumptionReport' });
}

function goToAsset() {
    router.push({ name: 'asset' });
}

function goToPersonalRanking() {
    router.push({ name: 'personalRanking', query: { month: currentPeriod } });
}

function goToChallengeReport() {
    router.push({
        name: 'challengeReport',
        query: habitSummary.value?.period ? { month: habitSummary.value.period } : {},
    });
}

onMounted(() => {
    loadHome();
    loadGroupSummary();
});
</script>

<template>
    <main class="home">
        <header class="home__header">
            <h1 class="home__title">홈</h1>

            <TheNotificationBell />
        </header>

        <StateLoading v-if="isLoading" message="홈 정보를 불러오는 중" />

        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="loadHome" />

        <div v-else class="home__stack">
            <HomeSpendingCard
                :today-amount="spending.todayAmount"
                :month-amount="spending.monthAmount"
                :change-rate="spending.changeRate"
                @open-ledger="goToLedger"
                @open-fixed-expense="goToFixedExpense"
            />

            <HomeMissionBubble :mission="mission" @open="goToPersonalChallenge" />

            <HomeTrialCard
                :personal="personalTrialRow"
                :group="groupTrialRow"
                @open-personal="goToPersonalChallenge"
                @open-group="goToGroupChallenge"
                @open-records="goToTrialRecords"
            />

            <section aria-labelledby="home-record-title">
                <h2 id="home-record-title" class="home__section-title">나의 기록</h2>

                <HomeRecordStats
                    :streak-days="streak?.streakCount ?? null"
                    :weekly="weeklyVerdicts"
                    :saved-amount="habitSummary?.savedAmount ?? null"
                />
            </section>

            <HomeAssetCard :summary="assetSummary" @open="goToAsset" />

            <PersonalMissionHonorBanner
                title="명예의 전당"
                description="이번 달 절약 랭킹을 확인해 보세요"
                @open="goToPersonalRanking"
            />

            <HomeFixedExpenseList
                :items="fixedExpense?.confirmed ?? []"
                :monthly-amount="fixedExpense?.summary?.expectedMonthlyAmount ?? null"
                @open-all="goToFixedExpense"
                @open-item="goToFixedExpenseDetail"
            />

            <button type="button" class="home-habit-card" @click="goToChallengeReport">
                <span v-if="habitSummary" class="home-habit-card__content">
                    <small>{{ habitSummary.month }}월 소비습관 변화</small>
                    <strong
                        ><b>{{ formatHomeAmount(habitSummary.savedAmount) }}원</b> 아꼈어요</strong
                    >
                    <em v-if="habitSummary.topCategoryName">
                        {{ habitSummary.topCategoryName }}에서 가장 큰 변화가 있었어요
                    </em>
                    <em v-else>확정된 챌린지 결과를 확인해 보세요</em>
                </span>
                <span v-else class="home-habit-card__content home-habit-card__content--empty">
                    <small>소비습관 변화 리포트</small>
                    <strong>{{ reportEmptyCopy.title }}</strong>
                    <em>{{ reportEmptyCopy.description }}</em>
                </span>
                <span class="home-habit-card__action">자세히 보기</span>
            </button>
        </div>
    </main>
</template>

<style scoped src="./HomeView.css"></style>
