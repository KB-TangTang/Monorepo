<!--
  용도: 사용자의 챌린지·그룹 재판·순자산·명예 법정 현황을 요약하는 홈 화면.
  챌린지 데이터 유무에 따라 참여 유도 카드와 진행 현황 카드를 전환한다.
-->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchHome } from '@/api/home';
import BaseBadge from '@/components/common/BaseBadge.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import TheNotificationBell from '@/components/common/TheNotificationBell.vue';
import { MOCK_HOME_ACTIVE, MOCK_HOME_EMPTY } from '@/fixtures/home';
import { clampHomeProgress, formatHomeAmount, formatHomeRate } from '@/utils/home';
import challengeImage from '@/assets/images/tang_home.png';
import honorCourtImage from '@/assets/images/emotions/56_with_trophy_ver4.png';

const router = useRouter();
const route = useRoute();

const homeData = ref(null);
const isLoading = ref(false);
const errorMessage = ref('');
const animatedProgress = ref(0);
const isDevelopment = import.meta.env.DEV;
let progressAnimationFrame = 0;
let progressAnimationTimer = 0;

const challenge = computed(() => homeData.value?.challenge ?? null);
const pendingVote = computed(() => homeData.value?.pendingVote ?? null);
const assetSummary = computed(() => homeData.value?.assetSummary ?? null);
const honorCourt = computed(() => homeData.value?.honorCourt ?? null);

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
    homeData.value = route.query.homeState === 'empty' ? MOCK_HOME_EMPTY : MOCK_HOME_ACTIVE;
    // // 백엔드 홈 API가 준비되기 전까지 개발 환경에서는 데이터가 있는 시안을 기본으로 보여준다.
    // if (isDevelopment) {
    //     homeData.value = route.query.homeState === 'empty' ? MOCK_HOME_EMPTY : MOCK_HOME_ACTIVE;
    //     return;
    // }
    //
    // isLoading.value = true;
    // errorMessage.value = '';
    //
    // try {
    //     homeData.value = await fetchHome();
    // } catch (error) {
    //     errorMessage.value = error.message ?? '홈 정보를 불러오지 못했습니다.';
    // } finally {
    //     isLoading.value = false;
    // }
}

function goToPersonalChallenge() {
    router.push({ name: 'personalMissionChallenge' });
}

function goToGroupChallenge() {
    router.push({ name: 'groupChallengeList' });
}

function goToAsset() {
    router.push({ name: 'asset' });
}

function goToPersonalRanking() {
    router.push({ name: 'personalRanking' });
}

function goToChallengeReport() {
    router.push({ name: 'challengeReport' });
}

onMounted(loadHome);
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

                <h1 class="home__title">{{ homeData?.userName ?? '사용자' }}님, 오늘도 탕탕!</h1>

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

                        <template v-if="challenge">
                            <strong class="summary-card__value">
                                투표 대기 {{ pendingVote?.count ?? 0 }}건
                            </strong>
                            <span
                                v-if="pendingVote?.closingSoonHours"
                                class="summary-card__caption summary-card__caption--danger"
                            >
                                마감 {{ pendingVote.closingSoonHours }}시간 전
                            </span>
                        </template>

                        <p v-else class="summary-card__empty-message">
                            새로운 사건에 참여해<br />
                            첫 판결을 내려보세요
                        </p>
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
                                {{ formatHomeAmount(assetSummary.netAssetAmount) }}
                            </strong>
                            <span
                                v-if="challenge"
                                class="summary-card__caption summary-card__caption--success"
                            >
                                전월 대비 {{ formatHomeRate(assetSummary.monthlyChangeRate) }}
                            </span>
                        </template>

                        <span v-else class="summary-card__empty-message">
                            자산 연결이 필요합니다
                        </span>
                    </BaseCard>
                </div>
            </section>

            <BaseCard
                v-if="honorCourt || !challenge"
                class="honor-court"
                :class="{ 'honor-court--empty': !challenge }"
                clickable
                padding="md"
                @click="challenge ? goToPersonalRanking() : goToPersonalChallenge()"
            >
                <div v-if="honorCourt && challenge" class="honor-court__content">
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
                    <h2 class="honor-court__empty-title">명예의 전당에 도전하세요</h2>
                    <p class="honor-court__empty-description">
                        미션에 참여해 순위에 이름을 올려보세요
                    </p>
                </div>

                <img class="honor-court__image" :src="honorCourtImage" alt="" />
            </BaseCard>

            <button
                v-if="honorCourt && challenge"
                type="button"
                class="home-habit-card"
                @click="goToChallengeReport"
            >
                <span class="home-habit-card__content">
                    <small>이번 달 소비습관 변화</small>
                    <strong><b>74,000원</b> 아꼈어요</strong>
                    <em>카페에서 가장 큰 변화가 있었어요</em>
                </span>
                <span class="home-habit-card__action">자세히 보기</span>
            </button>
        </template>
    </main>
</template>

<style scoped src="./HomeView.css"></style>
