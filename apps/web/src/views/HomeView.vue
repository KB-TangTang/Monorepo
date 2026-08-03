<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchHome } from '@/api/home';
import BaseBadge from '@/components/common/BaseBadge.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import challengeImage from '@/assets/images/challenge_live/concept-1-alive-mirrored.png';

const router = useRouter();

const homeData = ref(null);
const isLoading = ref(false);
const errorMessage = ref('');

const challenge = computed(() => homeData.value?.challenge ?? null);
const pendingVote = computed(() => homeData.value?.pendingVote ?? null);
const assetSummary = computed(() => homeData.value?.assetSummary ?? null);
const honorCourt = computed(() => homeData.value?.honorCourt ?? null);

const dateLabel = computed(() => {
    const today = new Date();

    return `${today.getMonth() + 1}월 ${today.getDate()}일`;
});

const currentMonth = computed(() => new Date().getMonth() + 1);

const challengeProgress = computed(() => {
    const progressRate = Number(challenge.value?.progressRate ?? 0);

    return Math.min(Math.max(progressRate, 0), 100);
});

async function loadHome() {
    isLoading.value = true;
    errorMessage.value = '';

    try {
        homeData.value = await fetchHome();
    } catch (error) {
        errorMessage.value = error.message ?? '홈 정보를 불러오지 못했습니다.';
    } finally {
        isLoading.value = false;
    }
}

function formatAmount(value) {
    return Number(value ?? 0).toLocaleString('ko-KR');
}

function formatRate(value) {
    const rate = Number(value ?? 0);

    return `${rate >= 0 ? '+' : ''}${rate}%`;
}

function goToTrial() {
    router.push('/trial');
}

function goToAsset() {
    router.push('/asset');
}

onMounted(loadHome);
</script>

<template>
    <main class="home">
        <StateLoading v-if="isLoading" message="홈 정보를 불러오는 중" />

        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="loadHome" />

        <template v-else>
            <header class="home__header">
                <div class="home__status-row">
                    <BaseBadge variant="progress"> {{ dateLabel }} · 심리 진행 중 </BaseBadge>

                    <div class="home__notification">
                        <span>알림</span>

                        <span
                            v-if="homeData?.notificationCount > 0"
                            class="home__notification-count"
                        >
                            {{ homeData.notificationCount }}
                        </span>
                    </div>
                </div>

                <h1 class="home__title">{{ homeData?.userName ?? '사용자' }}님, 오늘도 탕탕!</h1>

                <p class="home__description">
                    오늘의 재판과 자산을<br />
                    한 번에 확인해요
                </p>
            </header>

            <!-- 진행 중인 챌린지가 있을 때 -->
            <BaseCard v-if="challenge" class="challenge-card" padding="lg">
                <div class="challenge-card__top">
                    <BaseBadge>오늘의 메인 챌린지</BaseBadge>

                    <span class="challenge-card__case-no">
                        {{ challenge.caseNo }}
                    </span>
                </div>

                <h2 class="challenge-card__title">
                    {{ challenge.title }}
                </h2>

                <p class="challenge-card__summary">
                    선고 한도 {{ formatAmount(challenge.limitAmount) }}원 ·
                    <strong> {{ formatAmount(challenge.remainingAmount) }}원 남음 </strong>
                </p>

                <div class="challenge-card__progress-info">
                    <span>
                        {{ formatAmount(challenge.spentAmount) }}
                        /
                        {{ formatAmount(challenge.limitAmount) }}원
                    </span>

                    <strong>{{ challengeProgress }}%</strong>
                </div>

                <div
                    class="challenge-card__progress"
                    role="progressbar"
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

            <!-- 진행 중인 챌린지가 없을 때 -->
            <BaseCard v-else class="challenge-card challenge-card--empty" padding="lg">
                <div class="challenge-card__empty-content">
                    <BaseBadge>오늘의 메인 챌린지</BaseBadge>

                    <h2 class="challenge-card__empty-title">
                        챌린지에 참여하고<br />
                        <strong>당신의 자산을 지켜요!</strong>
                    </h2>

                    <p class="challenge-card__empty-description">미션에 참여해보세요</p>
                </div>

                <img class="challenge-card__image" :src="challengeImage" alt="" />

                <BaseButton class="challenge-card__button" size="sm" @click="goToTrial">
                    입장하기
                </BaseButton>
            </BaseCard>

            <section class="home-summary" aria-labelledby="home-summary-title">
                <h2 id="home-summary-title" class="home-summary__title">지금 확인할 것</h2>

                <div class="home-summary__grid">
                    <BaseCard clickable padding="md" @click="goToTrial">
                        <span class="summary-card__label"> ⚖ 그룹 재판 </span>

                        <strong class="summary-card__value">
                            투표 대기 {{ pendingVote?.count ?? 0 }}건
                        </strong>

                        <span
                            v-if="pendingVote?.closingSoonHours"
                            class="summary-card__caption summary-card__caption--danger"
                        >
                            마감 {{ pendingVote.closingSoonHours }}시간 전
                        </span>
                    </BaseCard>

                    <BaseCard clickable padding="md" @click="goToAsset">
                        <span class="summary-card__label"> ⚖ 순자산 평결액 </span>

                        <template v-if="assetSummary">
                            <strong class="summary-card__amount">
                                {{ formatAmount(assetSummary.netAssetAmount) }}
                            </strong>

                            <span class="summary-card__caption summary-card__caption--success">
                                전월 대비
                                {{ formatRate(assetSummary.monthlyChangeRate) }}
                            </span>
                        </template>

                        <span v-else class="summary-card__empty"> 자산 연결이 필요합니다 </span>
                    </BaseCard>
                </div>
            </section>

            <BaseCard class="honor-court" padding="md">
                <h2 class="honor-court__title">
                    {{ honorCourt?.month ?? currentMonth }}월 명예 법정
                </h2>

                <template v-if="honorCourt">
                    <div class="honor-court__ranking">
                        <strong>{{ honorCourt.rank }}위</strong>
                        <span>상위 {{ honorCourt.topPercent }}%</span>
                    </div>

                    <p class="honor-court__description">
                        월간 판결문이
                        {{ honorCourt.reportOpenDays }}일 후 열려요
                    </p>
                </template>
            </BaseCard>
        </template>
    </main>
</template>

<style scoped src="./HomeView.css"></style>
