<!--
  용도: 사용자의 챌린지·그룹 재판·순자산·명예 법정 현황을 요약하는 홈 화면.
  챌린지 데이터 유무에 따라 참여 유도 카드와 진행 현황 카드를 전환한다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchAssetSummary } from '@/api/asset';
import { fetchChallengeReport, fetchChallengeReportMonths } from '@/api/challengeReport';
import { fetchMyGroupChallenges, fetchMyTrials } from '@/api/groupChallenge';
import { fetchMissionRankings, fetchTodayMission } from '@/api/personalMission';
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
    getHomeReportEmptyCopy,
    toHomeMission,
    toHomeReportSummary,
} from '@/utils/home';
import { resolveDisplayName } from '@/utils/user';
import homeBenchImage from '@/assets/images/home/home-bench.png';
import homeBushLeftImage from '@/assets/images/home/home-bush-left.png';
import homeBushImage from '@/assets/images/home/home-bush.png';
import homeCloudFarImage from '@/assets/images/home/home-cloud-far.png';
import homeCloudSmallImage from '@/assets/images/home/home-cloud-small.png';
import homeCloudImage from '@/assets/images/home/home-cloud.png';
import districtCourtImage from '@/assets/images/home/home-court-district.png';
import supremeCourtImage from '@/assets/images/home/home-court-supreme.png';
import homeFlowerGroundImage from '@/assets/images/home/home-flower-ground.png';
import homeFlowerImage from '@/assets/images/home/home-flower.png';
import homeFlowerSmallImage from '@/assets/images/home/home-flower-small.png';
import homeLeaf1Image from '@/assets/images/home/home-leaf-1.png';
import homeLeaf2Image from '@/assets/images/home/home-leaf-2.png';
import homeLeaf3Image from '@/assets/images/home/home-leaf-3.png';
import homeLeaf4Image from '@/assets/images/home/home-leaf-4.png';
import homeLeaf5Image from '@/assets/images/home/home-leaf-5.png';
import homeLeaf6Image from '@/assets/images/home/home-leaf-6.png';
import homeLeaf7Image from '@/assets/images/home/home-leaf-7.png';
import homeLeaf8Image from '@/assets/images/home/home-leaf-8.png';
import homeLeaf9Image from '@/assets/images/home/home-leaf-9.png';
import homeTangiImage from '@/assets/images/home/home-tangi.png';
import homeTreeDistantImage from '@/assets/images/home/home-tree-distant.png';
import homeTreeMainImage from '@/assets/images/home/home-tree-main.png';
import homeTreeSmallImage from '@/assets/images/home/home-tree-small.png';
import honorCourtImage from '@/assets/images/emotions/56_with_trophy_ver4.png';

const router = useRouter();
const auth = useAuthStore();

const isLoading = ref(true);
const errorMessage = ref('');
const challenge = ref(null);
const assetSummary = ref(null);
const honorCourt = ref(null);
const habitSummary = ref(null);
const reportStatus = ref('loading');
const groupTrials = ref([]);
const activeGroupCount = ref(0);
const isGroupSummaryLoading = ref(true);
const hasGroupSummaryError = ref(false);

const displayName = computed(() => resolveDisplayName(auth.user) || '사용자');
const currentPeriod = getCurrentYearMonth();
const assetChange = computed(() => getHomeAssetChange(assetSummary.value?.monthOverMonthRate));
const reportEmptyCopy = computed(() => getHomeReportEmptyCopy(reportStatus.value));
const groupStatus = computed(() =>
    getHomeGroupStatus({
        trials: groupTrials.value,
        activeGroupCount: activeGroupCount.value,
        failed: hasGroupSummaryError.value,
    }),
);
const challengeProgress = computed(() => {
    return clampHomeProgress(challenge.value?.progressRate);
});

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
    habitSummary.value = reportResult.status === 'fulfilled' ? reportResult.value.summary : null;
    reportStatus.value = reportResult.status === 'fulfilled' ? reportResult.value.status : 'error';

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

function goToAllTrials() {
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
</script>

<template>
    <main class="home">
        <StateLoading v-if="isLoading" message="홈 정보를 불러오는 중" />

        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="loadHome" />

        <template v-else>
            <header class="home-hero">
                <div class="home-hero__copy">
                    <p>{{ displayName }}님, 안녕하세요 <span aria-hidden="true">👋</span></p>
                    <h1>오늘도 현명한 한 걸음!</h1>
                </div>
                <TheNotificationBell class="home-hero__bell" />

                <div class="home-hero__cloud-track home-hero__cloud-track--near">
                    <img :src="homeCloudImage" alt="" />
                    <img :src="homeCloudImage" alt="" />
                </div>
                <div class="home-hero__cloud-track home-hero__cloud-track--far">
                    <img :src="homeCloudSmallImage" alt="" />
                    <img :src="homeCloudFarImage" alt="" />
                </div>
                <img class="home-hero__tree-distant" :src="homeTreeDistantImage" alt="" />
                <img
                    class="home-hero__tree home-hero__tree--small"
                    :src="homeTreeSmallImage"
                    alt=""
                />
                <img class="home-hero__bench" :src="homeBenchImage" alt="" />
                <img class="home-hero__bush-left" :src="homeBushLeftImage" alt="" />
                <img class="home-hero__bush" :src="homeBushImage" alt="" />
                <img class="home-hero__flower" :src="homeFlowerImage" alt="" />
                <img class="home-hero__flower-small" :src="homeFlowerSmallImage" alt="" />
                <img
                    class="home-hero__tree home-hero__tree--main"
                    :src="homeTreeMainImage"
                    alt=""
                />
                <div class="home-hero__tangi-track">
                    <span aria-hidden="true"></span>
                    <img class="home-hero__tangi" :src="homeTangiImage" alt="산책하는 탕이" />
                </div>
                <img class="home-hero__leaf home-hero__leaf--fall-1" :src="homeLeaf2Image" alt="" />
                <img class="home-hero__leaf home-hero__leaf--fall-2" :src="homeLeaf3Image" alt="" />
                <img class="home-hero__leaf home-hero__leaf--fall-3" :src="homeLeaf4Image" alt="" />
                <img class="home-hero__leaf home-hero__leaf--fall-4" :src="homeLeaf5Image" alt="" />
                <img class="home-hero__leaf home-hero__leaf--fall-5" :src="homeLeaf6Image" alt="" />
                <img
                    class="home-hero__leaf home-hero__leaf--ground-1"
                    :src="homeLeaf1Image"
                    alt=""
                />
                <img
                    class="home-hero__leaf home-hero__leaf--ground-2"
                    :src="homeLeaf7Image"
                    alt=""
                />
                <img
                    class="home-hero__leaf home-hero__leaf--ground-3"
                    :src="homeLeaf8Image"
                    alt=""
                />
                <img
                    class="home-hero__leaf home-hero__leaf--ground-4"
                    :src="homeLeaf3Image"
                    alt=""
                />
                <img
                    class="home-hero__leaf home-hero__leaf--ground-5"
                    :src="homeLeaf9Image"
                    alt=""
                />
                <img class="home-hero__flower-ground" :src="homeFlowerGroundImage" alt="" />
                <span class="home-hero__ground" aria-hidden="true"></span>
            </header>

            <section class="trial-summary" aria-labelledby="trial-summary-title">
                <div class="trial-summary__header">
                    <h2 id="trial-summary-title">이번 달 재판 현황</h2>
                    <button type="button" @click="goToAllTrials">전체보기 ›</button>
                </div>

                <div class="trial-summary__grid">
                    <button
                        type="button"
                        class="trial-card trial-card--personal"
                        @click="goToPersonalChallenge"
                    >
                        <span class="trial-card__court">대법원</span>
                        <span class="trial-card__type">개인챌린지</span>
                        <img
                            class="trial-card__illustration trial-card__illustration--supreme"
                            :src="supremeCourtImage"
                            alt=""
                        />

                        <template v-if="challenge">
                            <small>오늘의 미션</small>
                            <strong class="trial-card__mission">{{ challenge.title }}</strong>
                            <strong class="trial-card__amount">
                                <template v-if="challenge.isAbsoluteMission">
                                    {{ challenge.spentAmount > 0 ? '한도 초과' : '위반 없음' }}
                                </template>
                                <template v-else>
                                    {{ formatHomeAmount(challenge.remainingAmount) }}원 남음
                                </template>
                            </strong>
                            <span
                                class="trial-card__progress"
                                role="progressbar"
                                :aria-label="`${challenge.title} 진행률`"
                                :aria-valuenow="challengeProgress"
                                aria-valuemin="0"
                                aria-valuemax="100"
                            >
                                <span :style="{ width: `${challengeProgress}%` }"></span>
                            </span>
                            <small>
                                사용 {{ formatHomeAmount(challenge.spentAmount) }}원 · 연속
                                {{ challenge.streakDays }}일
                            </small>
                        </template>
                        <template v-else>
                            <strong class="trial-card__empty">오늘의 미션을 시작해보세요</strong>
                        </template>

                        <span class="trial-card__action">입장하기 ›</span>
                    </button>

                    <button
                        type="button"
                        class="trial-card trial-card--group"
                        @click="goToGroupChallenge"
                    >
                        <span class="trial-card__court">지방법원</span>
                        <span class="trial-card__type">그룹챌린지</span>
                        <img
                            class="trial-card__illustration trial-card__illustration--district"
                            :src="districtCourtImage"
                            alt=""
                        />

                        <strong v-if="isGroupSummaryLoading" class="trial-card__group-status">
                            확인 중
                        </strong>
                        <strong v-else-if="hasGroupSummaryError" class="trial-card__group-status">
                            다시 확인
                        </strong>
                        <strong v-else class="trial-card__group-count">
                            {{ activeGroupCount }}<small>건 진행중</small>
                        </strong>

                        <span class="trial-card__action">입장하기 ›</span>
                    </button>
                </div>
            </section>

            <section class="home-summary" aria-labelledby="home-summary-title">
                <h2 id="home-summary-title" class="home-summary__title">지금 확인할 것</h2>

                <div class="home-summary__grid">
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
                                <span class="summary-card__unit">원</span>
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
        </template>
    </main>
</template>

<style scoped src="./HomeView.css"></style>
