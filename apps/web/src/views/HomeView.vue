<!--
  용도: 사용자의 챌린지·그룹 재판·순자산·명예 법정 현황을 요약하는 홈 화면.
  챌린지 데이터 유무에 따라 참여 유도 카드와 진행 현황 카드를 전환한다.
-->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import { fetchAssetSummary } from '@/api/asset';
import { fetchChallengeReport, fetchChallengeReportMonths } from '@/api/challengeReport';
import { fetchMyGroupChallenges, fetchMyTrials } from '@/api/groupChallenge';
import { fetchMissionRankings, fetchTodayMission } from '@/api/personalMission';
import BaseBadge from '@/components/common/BaseBadge.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import TheNotificationBell from '@/components/common/TheNotificationBell.vue';
import { useAuthStore } from '@/stores/auth';
import {
    clampHomeProgress,
    formatHomeAmount,
    getCurrentYearMonth,
    getDaysUntilNextMonth,
    getHomeAssetChange,
    getHomeGroupStatus,
    toHomeMission,
    toHomeReportSummary,
} from '@/utils/home';
import { useCountdown } from '@/utils/useCountdown';
import { resolveDisplayName } from '@/utils/user';
import challengeImage from '@/assets/images/tang_home.png';
import honorCourtImage from '@/assets/images/emotions/56_with_trophy_ver4.png';

const router = useRouter();
const auth = useAuthStore();

const isLoading = ref(true);
const errorMessage = ref('');
const animatedProgress = ref(0);
const challenge = ref(null);
const assetSummary = ref(null);
const honorCourt = ref(null);
const habitSummary = ref(null);
const groupTrials = ref([]);
const activeGroupCount = ref(0);
const isGroupSummaryLoading = ref(true);
const hasGroupSummaryError = ref(false);
let progressAnimationFrame = 0;
let progressAnimationTimer = 0;

const displayName = computed(() => resolveDisplayName(auth.user) || '사용자');
const currentPeriod = getCurrentYearMonth();
const assetChange = computed(() => getHomeAssetChange(assetSummary.value?.monthOverMonthRate));
const groupStatus = computed(() =>
    getHomeGroupStatus({
        trials: groupTrials.value,
        activeGroupCount: activeGroupCount.value,
        failed: hasGroupSummaryError.value,
    }),
);
const groupTodoItems = computed(() => groupTrials.value);
const { countdowns: groupCountdowns } = useCountdown(groupTodoItems);
const groupDeadline = computed(() => {
    const item = groupStatus.value.item;
    return item ? groupCountdowns.value[item.id]?.text : null;
});

const dateLabel = computed(() => {
    const today = new Date();

    return `${today.getMonth() + 1}월 ${today.getDate()}일`;
});

const challengeProgress = computed(() => {
    return clampHomeProgress(challenge.value?.progressRate);
});

function stopProgressAnimation() {
    window.clearTimeout(progressAnimationTimer);
    window.cancelAnimationFrame(progressAnimationFrame);
}

function animateProgress(targetProgress) {
    stopProgressAnimation();
    animatedProgress.value = 0;

    if (targetProgress <= 0) {
        return;
    }

    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const duration = reduceMotion ? 400 : 1400;
    const delay = reduceMotion ? 100 : 200;

    progressAnimationTimer = window.setTimeout(() => {
        const startedAt = performance.now();

        function updateProgress(now) {
            const elapsedRatio = Math.min((now - startedAt) / duration, 1);
            const easedRatio = 1 - Math.pow(1 - elapsedRatio, 3);
            animatedProgress.value = Math.round(targetProgress * easedRatio);

            if (elapsedRatio < 1) {
                progressAnimationFrame = window.requestAnimationFrame(updateProgress);
            }
        }

        progressAnimationFrame = window.requestAnimationFrame(updateProgress);
    }, delay);
}

watch(challengeProgress, animateProgress, { immediate: true });

async function loadHome() {
    isLoading.value = true;
    errorMessage.value = '';

    const [missionResult, assetResult, rankingResult, reportResult] = await Promise.allSettled([
        fetchTodayMission(),
        fetchAssetSummary(),
        fetchMissionRankings(currentPeriod),
        loadLatestReport(),
    ]);

    challenge.value =
        missionResult.status === 'fulfilled' ? toHomeMission(missionResult.value) : null;
    assetSummary.value = assetResult.status === 'fulfilled' ? assetResult.value : null;

    const myRanking = rankingResult.status === 'fulfilled' ? rankingResult.value?.myRanking : null;
    honorCourt.value = myRanking
        ? {
              month: Number(currentPeriod.split('-')[1]),
              rank: myRanking.rank,
              topPercent: myRanking.topPercent,
              reportOpenDays: getDaysUntilNextMonth(),
          }
        : null;
    habitSummary.value = reportResult.status === 'fulfilled' ? reportResult.value : null;

    if (
        [missionResult, assetResult, rankingResult, reportResult].every(
            (r) => r.status === 'rejected',
        )
    ) {
        errorMessage.value = '홈 정보를 불러오지 못했습니다.';
    }
    isLoading.value = false;
}

async function loadLatestReport() {
    const availability = await fetchChallengeReportMonths();
    const latestMonth = availability.months?.find((month) => month.available);
    if (!latestMonth) return null;

    return toHomeReportSummary(await fetchChallengeReport(latestMonth.value));
}

async function loadGroupSummary() {
    isGroupSummaryLoading.value = true;
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
    isGroupSummaryLoading.value = false;
}

function goToPersonalChallenge() {
    router.push({ name: 'personalMissionChallenge' });
}

function goToGroupChallenge() {
    if (hasGroupSummaryError.value) {
        loadGroupSummary();
        return;
    }

    const item = groupStatus.value.item;
    if (item?.type === 'accuse') {
        router.push({
            name: 'defenseViolation',
            params: { id: item.challengeId, indictmentId: item.indictmentId },
        });
        return;
    }
    if (item?.type === 'vote') {
        router.push({
            name: 'voteVerdict',
            params: { id: item.challengeId, indictmentId: item.indictmentId },
        });
        return;
    }

    router.push({ name: 'groupChallengeList' });
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
onBeforeUnmount(stopProgressAnimation);
</script>

<template>
    <main class="home">
        <StateLoading v-if="isLoading" message="홈 정보를 불러오는 중" />

        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="loadHome" />

        <template v-else>
            <header class="home__header">
                <div class="home__status-row">
                    <BaseBadge class="home__date" variant="progress">{{ dateLabel }}</BaseBadge>

                    <TheNotificationBell />
                </div>

                <h1 class="home__title">{{ displayName }}님, 오늘도 탕탕!</h1>

                <p class="home__description">
                    오늘의 재판과 자산을<br />
                    한 번에 확인해요
                </p>
            </header>

            <BaseCard
                v-if="challenge"
                class="challenge-card"
                clickable
                padding="lg"
                @click="goToPersonalChallenge"
            >
                <BaseBadge class="challenge-card__badge">오늘의 메인 챌린지</BaseBadge>

                <h2 class="challenge-card__title">{{ challenge.title }}</h2>

                <p class="challenge-card__summary">
                    선고 한도 {{ formatHomeAmount(challenge.limitAmount) }}원 ·
                    <strong>{{ formatHomeAmount(challenge.remainingAmount) }}원 남음</strong>
                </p>

                <div class="challenge-card__progress-info">
                    <span>
                        {{ formatHomeAmount(challenge.spentAmount) }} /
                        {{ formatHomeAmount(challenge.limitAmount) }}원
                    </span>
                    <strong>{{ animatedProgress }}%</strong>
                </div>

                <div
                    class="challenge-card__progress"
                    role="progressbar"
                    :aria-label="`${challenge.title} 진행률`"
                    :aria-valuenow="challengeProgress"
                    aria-valuemin="0"
                    aria-valuemax="100"
                >
                    <span
                        class="challenge-card__progress-value"
                        :style="{ width: `${challengeProgress}%` }"
                    ></span>
                </div>
            </BaseCard>

            <BaseCard v-else class="challenge-card challenge-card--empty" padding="lg">
                <img class="challenge-card__image" :src="challengeImage" alt="" />

                <div class="challenge-card__empty-content">
                    <BaseBadge class="challenge-card__badge">오늘의 메인 챌린지</BaseBadge>

                    <h2 class="challenge-card__empty-title">
                        챌린지에 참여하고<br />
                        <strong>당신의 자산을 지켜요!</strong>
                    </h2>

                    <p class="challenge-card__empty-description">미션에 참여해보세요</p>

                    <BaseButton
                        class="challenge-card__button"
                        size="md"
                        @click="goToPersonalChallenge"
                    >
                        입장하기
                    </BaseButton>
                </div>
            </BaseCard>

            <section class="home-summary" aria-labelledby="home-summary-title">
                <h2 id="home-summary-title" class="home-summary__title">지금 확인할 것</h2>

                <div class="home-summary__grid">
                    <BaseCard clickable padding="md" @click="goToGroupChallenge">
                        <span class="summary-card__label">
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="1.8"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                aria-hidden="true"
                            >
                                <path d="m4 16 8-8 4 4-8 8H4z" />
                                <path d="m13 7 2-2 4 4-2 2" />
                            </svg>
                            그룹 재판
                        </span>

                        <template v-if="isGroupSummaryLoading">
                            <strong class="summary-card__value">할 일을 확인 중이에요</strong>
                        </template>

                        <template v-else>
                            <strong
                                class="summary-card__value summary-card__value--group"
                                :class="{
                                    'summary-card__value--success': groupStatus.kind === 'cruising',
                                }"
                            >
                                {{ groupStatus.title }}
                            </strong>
                            <span
                                class="summary-card__caption"
                                :class="{
                                    'summary-card__caption--danger':
                                        groupStatus.kind === 'accuse' ||
                                        groupStatus.kind === 'vote' ||
                                        groupStatus.kind === 'error',
                                    'summary-card__caption--success':
                                        groupStatus.kind === 'cruising',
                                }"
                            >
                                <template v-if="groupDeadline"
                                    >마감 {{ groupDeadline }} ·
                                </template>
                                {{ groupStatus.caption }}
                            </span>
                        </template>
                    </BaseCard>

                    <BaseCard clickable padding="md" @click="goToAsset">
                        <span class="summary-card__label">
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="1.8"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                aria-hidden="true"
                            >
                                <path d="M12 4v16M5 7h14M4 7l-2 7h6L6 7M18 7l-2 7h6l-2-7M8 20h8" />
                            </svg>
                            순자산 평결액
                        </span>

                        <template v-if="assetSummary">
                            <strong class="summary-card__amount">
                                {{ formatHomeAmount(assetSummary.netWorth) }}
                            </strong>
                            <span
                                v-if="assetChange"
                                class="summary-card__caption"
                                :class="{
                                    'summary-card__caption--success':
                                        assetChange.tone === 'success',
                                    'summary-card__caption--danger': assetChange.tone === 'danger',
                                }"
                            >
                                {{ assetChange.text }}
                            </span>
                        </template>

                        <span v-else class="summary-card__empty-message">
                            자산 연결이 필요합니다
                        </span>
                    </BaseCard>
                </div>
            </section>

            <BaseCard
                class="honor-court"
                :class="{ 'honor-court--empty': !honorCourt }"
                clickable
                padding="md"
                @click="goToPersonalRanking"
            >
                <div v-if="honorCourt" class="honor-court__content">
                    <h2 class="honor-court__title">{{ honorCourt.month }}월 명예의 전당</h2>

                    <div class="honor-court__ranking">
                        <strong>{{ honorCourt.rank }}위</strong>
                        <span>상위 {{ honorCourt.topPercent }}%</span>
                    </div>

                    <p class="honor-court__description">
                        월간 판결문이 {{ honorCourt.reportOpenDays }}일 후 열려요
                    </p>
                </div>

                <div v-else class="honor-court__content honor-court__empty-content">
                    <h2 class="honor-court__empty-title">
                        {{ Number(currentPeriod.split('-')[1]) }}월 명예의 전당
                    </h2>
                    <p class="honor-court__empty-description">
                        이번 달 랭킹이 집계되면 순위를 알려드릴게요
                    </p>
                </div>

                <img class="honor-court__image" :src="honorCourtImage" alt="" />
            </BaseCard>

            <button
                v-if="habitSummary"
                type="button"
                class="home-habit-card"
                @click="goToChallengeReport"
            >
                <span class="home-habit-card__content">
                    <small>{{ habitSummary.month }}월 소비습관 변화</small>
                    <strong
                        ><b>{{ formatHomeAmount(habitSummary.savedAmount) }}원</b> 아꼈어요</strong
                    >
                    <em v-if="habitSummary.topCategoryName">
                        {{ habitSummary.topCategoryName }}에서 가장 큰 변화가 있었어요
                    </em>
                    <em v-else>확정된 챌린지 결과를 확인해 보세요</em>
                </span>
                <span class="home-habit-card__action">자세히 보기</span>
            </button>
        </template>
    </main>
</template>

<style scoped src="./HomeView.css"></style>
