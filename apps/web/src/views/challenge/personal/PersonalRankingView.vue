<script setup>
import { computed, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import ChallengePageHeader from '@/components/challenge/ChallengePageHeader.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import PersonalRankingMonthPicker from '@/components/challenge/personal/ranking/PersonalRankingMonthPicker.vue';
import PersonalRankingSummaryCard from '@/components/challenge/personal/ranking/PersonalRankingSummaryCard.vue';
import tangtangMascot from '@/assets/images/tangtang.png';
import { MOCK_PERSONAL_RANKING_MONTHS, MOCK_PERSONAL_RANKINGS } from '@/fixtures/personalRanking';

const route = useRoute();
const router = useRouter();
const isMonthPickerOpen = ref(false);

/*
 * URL에 유효한 month query가 있으면 해당 월을 사용하고,
 * 없거나 데이터가 없는 월이면 가장 최근 조회 가능 월을 기본값으로 사용합니다.
 */
const latestAvailablePeriod = [...MOCK_PERSONAL_RANKING_MONTHS]
    .reverse()
    .find((month) => month.available)?.value;

const initialPeriod =
    typeof route.query.month === 'string' &&
    MOCK_PERSONAL_RANKING_MONTHS.some((month) => month.value === route.query.month)
        ? route.query.month
        : latestAvailablePeriod;

const selectedPeriod = ref(initialPeriod);

/* 선택 월이 바뀌면 화면에 표시할 랭킹 데이터도 자동으로 변경됩니다. */
const ranking = computed(() => MOCK_PERSONAL_RANKINGS[selectedPeriod.value]);
const hasRanking = computed(() => Boolean(ranking.value));

const selectedMonth = computed(() =>
    MOCK_PERSONAL_RANKING_MONTHS.find((month) => month.value === selectedPeriod.value),
);

const availablePeriods = computed(() => MOCK_PERSONAL_RANKING_MONTHS.map((month) => month.value));

const currentPeriodIndex = computed(() =>
    availablePeriods.value.indexOf(selectedPeriod.value),
);

const previousPeriod = computed(() => availablePeriods.value[currentPeriodIndex.value - 1]);
const nextPeriod = computed(() => availablePeriods.value[currentPeriodIndex.value + 1]);

function goBack() {
    router.push({ name: 'personalMissionChallenge' });
}

function openCertificate() {
    router.push({
        name: 'personalCertificate',
        query: { month: selectedPeriod.value },
    });
}

function updatePeriod(period) {
    selectedPeriod.value = period;

    router.replace({
        name: 'personalRanking',
        query: { month: period },
    });
}

function movePeriod(direction) {
    const period = availablePeriods.value[currentPeriodIndex.value + direction];

    if (period) {
        updatePeriod(period);
    }
}

/* 다음 단계에서 월 선택 바텀시트가 이 함수를 호출합니다. */
function selectPeriod(period) {
    if (!MOCK_PERSONAL_RANKING_MONTHS.some((month) => month.value === period)) {
        return;
    }

    /* BaseBottomSheet가 추가한 뒤로가기 history를 정리한 뒤 URL을 갱신합니다. */
    if (window.history.state?.ttOverlay) {
        window.addEventListener('popstate', () => updatePeriod(period), { once: true });
        return;
    }

    updatePeriod(period);
}
</script>

<template>
    <main class="personal-ranking">
        <ChallengePageHeader
            title="명예의 전당"
            back-label="개인 미션 홈으로 돌아가기"
            @back="goBack"
        />

        <section class="personal-ranking__period-navigation" aria-label="랭킹 조회 월">
            <button
                type="button"
                aria-label="이전 달 랭킹 보기"
                :disabled="!previousPeriod"
                @click="movePeriod(-1)"
            >
                ‹
            </button>
            <button type="button" aria-label="랭킹 조회 월 선택" @click="isMonthPickerOpen = true">
                {{ selectedMonth?.year }}.{{ String(selectedMonth?.month).padStart(2, '0') }}
            </button>
            <button
                type="button"
                aria-label="다음 달 랭킹 보기"
                :disabled="!nextPeriod"
                @click="movePeriod(1)"
            >
                ›
            </button>
        </section>

        <template v-if="hasRanking">
            <section class="personal-ranking__podium-section">
                <header class="personal-ranking__podium-header">
                    <h2>이번 달 명예의 전당</h2>
                    <button type="button" @click="openCertificate">명예 인증서 발급 받기</button>
                </header>
                <div class="personal-ranking__podium">
                    <article
                        v-for="member in ranking.podium"
                        :key="member.rank"
                        :class="`personal-ranking__podium-item--${member.rank}`"
                        class="personal-ranking__podium-item"
                    >
                        <span
                            v-if="member.rank === 1"
                            class="personal-ranking__podium-medal"
                            aria-label="월간 랭킹 1위"
                        >
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="1.8"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                aria-hidden="true"
                            >
                                <path d="M8 4h8v4a4 4 0 0 1-8 0V4Z" />
                                <path d="M8 6H5v1a4 4 0 0 0 4 4" />
                                <path d="M16 6h3v1a4 4 0 0 1-4 4" />
                                <path d="M12 12v4" />
                                <path d="M9 20h6" />
                                <path d="M10 16h4v4h-4z" />
                            </svg>
                        </span>
                        <span class="personal-ranking__podium-avatar">
                            {{ member.name.slice(0, 1) }}
                        </span>
                        <strong class="personal-ranking__podium-name">{{ member.name }}</strong>
                        <small class="personal-ranking__podium-score">
                            {{ member.score.toLocaleString('ko-KR') }}점
                        </small>
                        <div class="personal-ranking__podium-rank">{{ member.rank }}</div>
                    </article>
                </div>
            </section>

            <section class="personal-ranking__mine">
                <strong>{{ ranking.myRank.toLocaleString('ko-KR') }}</strong>
                <span class="personal-ranking__avatar">나</span>
                <p>
                    <b>나</b>
                    <small>
                        연속 {{ ranking.streakDays }}일 · 최고 {{ ranking.bestStreakDays }}일
                    </small>
                </p>
                <strong>{{ ranking.score.toLocaleString('ko-KR') }}점</strong>
            </section>

            <PersonalRankingSummaryCard :ranking="ranking" />

            <aside class="personal-ranking__reset-notice">
                <span aria-hidden="true">↻</span>
                <p>매월 1일, 점수는 0점부터 다시 시작해요.</p>
            </aside>
        </template>

        <StateEmpty
            v-else
            title="아직 랭킹 데이터가 없어요"
            :description="`${selectedMonth?.year}.${String(selectedMonth?.month).padStart(2, '0')} 랭킹이 집계되면 명예의 전당에서 만나요.`"
        >
            <template #icon>
                <img :src="tangtangMascot" alt="쉬고 있는 탕이" class="personal-ranking__empty-mascot" />
            </template>
        </StateEmpty>

        <PersonalRankingMonthPicker
            v-model="isMonthPickerOpen"
            :months="MOCK_PERSONAL_RANKING_MONTHS"
            :selected-period="selectedPeriod"
            @select="selectPeriod"
        />

        <ChallengeModeTabBar active-mode="personal" />
    </main>
</template>

<style scoped src="./PersonalRankingView.css"></style>
