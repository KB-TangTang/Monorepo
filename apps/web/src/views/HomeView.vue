<!--
  용도: 홈 탭. 「오늘 쓴 돈 → 오늘의 미션 → 재판 → 소비습관 변화 → 자산 → 명예의 전당」 순으로
        오늘 확인할 것을 위에서 아래로 늘어놓는다 (이슈 #450).
  이 파일은 데이터를 모아 컴포넌트에 넘기고 이동만 시킨다 — 표시 규칙은 components/home/* 과
  utils/home.js 에 있다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchAssetSummary } from '@/api/asset';
import { fetchChallengeReport, fetchChallengeReportMonths } from '@/api/challengeReport';
import { fetchMyGroupChallenges, fetchMyTrials } from '@/api/groupChallenge';
import { fetchHomeSpendingSummary } from '@/api/ledger';
import { fetchMissionStreak, fetchTodayMission } from '@/api/personalMission';
import PersonalMissionHonorBanner from '@/components/challenge/personal/PersonalMissionHonorBanner.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import TheNotificationBell from '@/components/common/TheNotificationBell.vue';
import HomeAssetCard from '@/components/home/HomeAssetCard.vue';
import HomeMissionBubble from '@/components/home/HomeMissionBubble.vue';
import HomeReportCard from '@/components/home/HomeReportCard.vue';
import HomeSpendingCard from '@/components/home/HomeSpendingCard.vue';
import HomeTrialCard from '@/components/home/HomeTrialCard.vue';
import {
    getCurrentYearMonth,
    getHomeGroupTrialRow,
    getHomePersonalTrialRow,
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
const habitSummary = ref(null);
const reportStatus = ref('loading');
const groupTrials = ref([]);
const activeGroupCount = ref(0);
const hasGroupSummaryError = ref(false);

const currentPeriod = getCurrentYearMonth();

const personalTrialRow = computed(() => getHomePersonalTrialRow(streak.value?.streakCount));
const groupTrialRow = computed(() =>
    getHomeGroupTrialRow({
        trials: groupTrials.value,
        activeGroupCount: activeGroupCount.value,
        failed: hasGroupSummaryError.value,
    }),
);

async function loadHome() {
    isLoading.value = true;
    errorMessage.value = '';

    const results = await Promise.allSettled([
        fetchTodayMission(),
        fetchMissionStreak(),
        fetchAssetSummary(),
        loadLatestReport(),
        fetchHomeSpendingSummary(),
    ]);
    const [missionResult, streakResult, assetResult, reportResult, spendingResult] = results;

    mission.value =
        missionResult.status === 'fulfilled' ? toHomeMission(missionResult.value) : null;
    /* 실패하면 세 값이 전부 null 이 되어 카드가 「—」·「집계 중」으로 버틴다 — 나머지 홈은 그대로 뜬다. */
    spending.value = toHomeSpending(
        spendingResult.status === 'fulfilled' ? spendingResult.value : null,
    );
    streak.value = streakResult.status === 'fulfilled' ? streakResult.value : null;
    assetSummary.value = assetResult.status === 'fulfilled' ? assetResult.value : null;
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

function goToPersonalChallenge() {
    router.push({ name: 'personalMissionChallenge' });
}

/*
 * 지방법원 줄은 그룹챌린지 「홈」으로 보낸다 — groupChallengeList 는 「재판 전체보기」라 한 단계 안쪽이다.
 * 위 대법원 줄이 개인챌린지 홈(personalMissionChallenge)으로 가므로 두 줄의 도착지 깊이를 맞춘다.
 */
function goToGroupChallenge() {
    if (hasGroupSummaryError.value) {
        loadGroupSummary();
        return;
    }

    router.push({ name: 'groupChallenge' });
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
            />

            <HomeReportCard
                :summary="habitSummary"
                :status="reportStatus"
                @open="goToChallengeReport"
            />

            <HomeAssetCard :summary="assetSummary" @open="goToAsset" />

            <PersonalMissionHonorBanner
                title="명예의 전당"
                description="이번 달 절약 랭킹을 확인해 보세요"
                @open="goToPersonalRanking"
            />
        </div>
    </main>
</template>

<style scoped src="./HomeView.css"></style>
