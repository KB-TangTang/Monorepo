<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ChallengePageHeader from '@/components/challenge/ChallengePageHeader.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import UserAvatar from '@/components/common/UserAvatar.vue';
import PersonalRankingMonthPicker from '@/components/challenge/personal/ranking/PersonalRankingMonthPicker.vue';
import tangtangMascot from '@/assets/images/tangtang.png';
import firstRankTangi from '@/assets/images/ranking/tang-ranking-first.png';
import secondRankTangi from '@/assets/images/ranking/tang-ranking-second.png';
import thirdRankTangi from '@/assets/images/ranking/tang-ranking-third.png';
import { fetchMissionRankingMonths, fetchMissionRankings } from '@/api/personalMission';

const route = useRoute();
const router = useRouter();
const isMonthPickerOpen = ref(false);
const ranking = ref(null);
const isLoading = ref(false);
const errorMessage = ref('');
const now = new Date();
const currentPeriod = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;
const yearMonthPattern = /^\d{4}-(0[1-9]|1[0-2])$/;
const rankingMonths = ref(createRankingMonths([now.getFullYear()], new Set()));

const initialPeriod =
    typeof route.query.month === 'string' && yearMonthPattern.test(route.query.month)
        ? route.query.month
        : currentPeriod;

const selectedPeriod = ref(initialPeriod);

const hasRanking = computed(() => Boolean(ranking.value));
const podium = computed(() => ranking.value?.topRankings.slice(0, 3) ?? []);
const rankingList = computed(() => ranking.value?.topRankings.slice(3, 10) ?? []);

const podiumImages = {
    1: firstRankTangi,
    2: secondRankTangi,
    3: thirdRankTangi,
};

const selectedMonth = computed(() =>
    rankingMonths.value.find((month) => month.value === selectedPeriod.value),
);

const availablePeriods = computed(() => rankingMonths.value.map((month) => month.value));

const currentPeriodIndex = computed(() => availablePeriods.value.indexOf(selectedPeriod.value));
const isCertificateAvailable = computed(() => selectedPeriod.value < currentPeriod);

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

function createRankingMonths(years, availablePeriodsSet) {
    return [...new Set(years)]
        .sort((a, b) => a - b)
        .flatMap((year) =>
            Array.from({ length: 12 }, (_, index) => {
                const month = index + 1;
                const value = `${year}-${String(month).padStart(2, '0')}`;
                return {
                    value,
                    year,
                    month,
                    available: availablePeriodsSet.has(value),
                };
            }),
        );
}

async function loadRankingMonths() {
    try {
        const result = await fetchMissionRankingMonths();
        const availablePeriodsSet = new Set(result?.yearMonths ?? []);
        const years = [
            now.getFullYear(),
            ...[...availablePeriodsSet].map((period) => Number(period.slice(0, 4))),
        ];
        rankingMonths.value = createRankingMonths(years, availablePeriodsSet);
    } catch {
        // 월 목록 조회가 실패해도 현재 월 랭킹 본문은 계속 조회한다.
    }
}

async function loadRanking() {
    isLoading.value = true;
    errorMessage.value = '';

    try {
        ranking.value = await fetchMissionRankings(selectedPeriod.value);
    } catch (error) {
        ranking.value = null;
        errorMessage.value = error.message || '랭킹을 불러오지 못했어요.';
    } finally {
        isLoading.value = false;
    }
}

async function updatePeriod(period) {
    selectedPeriod.value = period;

    router.replace({
        name: 'personalRanking',
        query: { month: period },
    });

    await loadRanking();
}

async function movePeriod(direction) {
    const period = availablePeriods.value[currentPeriodIndex.value + direction];

    if (period) {
        await updatePeriod(period);
    }
}

/* 다음 단계에서 월 선택 바텀시트가 이 함수를 호출합니다. */
function selectPeriod(period) {
    if (!rankingMonths.value.some((month) => month.value === period)) {
        return;
    }

    /* BaseBottomSheet가 추가한 뒤로가기 history를 정리한 뒤 URL을 갱신합니다. */
    if (window.history.state?.ttOverlay) {
        window.addEventListener('popstate', () => updatePeriod(period), { once: true });
        return;
    }

    updatePeriod(period);
}

onMounted(async () => {
    await loadRankingMonths();
    await loadRanking();
});
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

        <p v-if="isLoading" class="personal-ranking__status" role="status">
            랭킹을 불러오고 있어요.
        </p>

        <template v-else-if="hasRanking">
            <section class="personal-ranking__podium-section">
                <header class="personal-ranking__podium-header">
                    <h2>이번 달 명예의 전당</h2>
                    <button v-if="isCertificateAvailable" type="button" @click="openCertificate">
                        ♙ 인증서 발급
                    </button>
                </header>
                <div class="personal-ranking__podium">
                    <article
                        v-for="member in podium"
                        :key="member.rank"
                        :class="`personal-ranking__podium-item--${member.rank}`"
                        class="personal-ranking__podium-item"
                    >
                        <span class="personal-ranking__podium-crown" aria-hidden="true">
                            <small>{{ member.rank }}</small>
                        </span>
                        <span class="personal-ranking__podium-avatar">
                            <img :src="podiumImages[member.rank]" alt="" />
                        </span>
                        <strong class="personal-ranking__podium-name">{{ member.nickname }}</strong>
                        <small class="personal-ranking__podium-score">
                            {{ member.totalScore.toLocaleString('ko-KR') }}점
                        </small>
                    </article>
                </div>
            </section>

            <section class="personal-ranking__list" aria-labelledby="personal-ranking-list-title">
                <header class="personal-ranking__list-header">
                    <h2 id="personal-ranking-list-title">전체 랭킹</h2>
                    <p><span aria-hidden="true">↻</span> 매월 1일 초기화</p>
                </header>
                <ol>
                    <li v-for="member in rankingList" :key="member.rank">
                        <strong>{{ member.rank }}위</strong>
                        <UserAvatar
                            :image-url="member.profileImageUrl"
                            :name="member.nickname"
                            :size="40"
                        />
                        <span>{{ member.nickname }}</span>
                        <b>{{ member.totalScore.toLocaleString('ko-KR') }}점</b>
                    </li>
                </ol>
            </section>

            <div class="personal-ranking__mine-dock">
                <section class="personal-ranking__mine">
                    <p class="personal-ranking__mine-rank">
                        <span>내 순위</span>
                        <strong>{{ ranking.myRanking.rank.toLocaleString('ko-KR') }}위</strong>
                    </p>
                    <UserAvatar
                        :image-url="ranking.myRanking.profileImageUrl"
                        :name="ranking.myRanking.nickname"
                        :size="48"
                    />
                    <b>{{ ranking.myRanking.nickname }}</b>
                    <span class="personal-ranking__mine-percentile">
                        상위 {{ ranking.myRanking.topPercent }}%
                    </span>
                    <strong class="personal-ranking__mine-score">
                        {{ ranking.myRanking.totalScore.toLocaleString('ko-KR') }}점
                    </strong>
                </section>
            </div>
        </template>

        <StateEmpty
            v-else
            :title="errorMessage ? '랭킹을 불러오지 못했어요' : '아직 랭킹 데이터가 없어요'"
            :description="
                errorMessage ||
                `${selectedMonth?.year}.${String(selectedMonth?.month).padStart(2, '0')} 랭킹이 집계되면 명예의 전당에서 만나요.`
            "
        >
            <template #icon>
                <img
                    :src="tangtangMascot"
                    alt="쉬고 있는 탕이"
                    class="personal-ranking__empty-mascot"
                />
            </template>
        </StateEmpty>

        <PersonalRankingMonthPicker
            v-model="isMonthPickerOpen"
            :months="rankingMonths"
            :selected-period="selectedPeriod"
            @select="selectPeriod"
        />
    </main>
</template>

<style scoped src="./PersonalRankingView.css"></style>
