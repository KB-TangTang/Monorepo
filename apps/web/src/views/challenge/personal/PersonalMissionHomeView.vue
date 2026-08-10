<script setup>
import { onMounted, ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import PersonalMissionConsentSheet from '@/components/challenge/personal/PersonalMissionConsentSheet.vue';
import PersonalCourtHeader from '@/components/challenge/personal/PersonalCourtHeader.vue';
import PersonalBriefingCard from '@/components/challenge/personal/PersonalBriefingCard.vue';
import PersonalWatchlistCard from '@/components/challenge/personal/PersonalWatchlistCard.vue';
import PersonalScoreCard from '@/components/challenge/personal/PersonalScoreCard.vue';
import PersonalTangiSheet from '@/components/challenge/personal/PersonalTangiSheet.vue';
import PersonalVerdictModal from '@/components/challenge/personal/PersonalVerdictModal.vue';
import PersonalNoAccountCard from '@/components/challenge/personal/PersonalNoAccountCard.vue';
import { usePersonalMissionChallengeStore } from '@/stores/personalMission';
import { formatCourtDate, calculateDataProgress, formatWon } from '@/services/personalMissionFlow';
import { MOCK_VERDICT_SUCCESS, MOCK_VERDICT_FAIL } from '@/fixtures/personalChallenge';
import courtSupreme from '@/assets/images/court/court_supreme.png';

const router = useRouter();
const store = usePersonalMissionChallengeStore();

const isConsentOpen = ref(false);
const isTangiSheetOpen = ref(false);
const isVerdictOpen = ref(false);
const isDevelopment = import.meta.env.DEV;

const courtDate = computed(() => formatCourtDate());
const shortDate = computed(() => {
    const d = new Date();
    return `${d.getMonth() + 1}월 ${d.getDate()}일`;
});

const dataProgress = computed(() => calculateDataProgress(store.dataRequirements));

/*
 * BaseModal과 BaseBottomSheet는 열릴 때 뒤로가기용 history 항목을 추가한다.
 * 닫자마자 다음 화면을 열면 이전 오버레이의 history.back()이 새 화면까지 닫을 수 있으므로,
 * 해당 항목이 정리된 뒤 다음 단계로 이동한다.
 */
function afterOverlayClosed(callback) {
    if (!window.history.state?.ttOverlay) {
        callback();
        return;
    }
    window.addEventListener('popstate', callback, { once: true });
}

onMounted(() => {
    store.hydrate();

    if (!store.hasAgreed) {
        isConsentOpen.value = true;
        return;
    }

    if (store.hasPendingVerdict) {
        isVerdictOpen.value = true;
    }
});

function handleAgree() {
    store.agree();
}

function openTangiSheet() {
    isTangiSheetOpen.value = true;
}

function handleProsecutorConfirm(prosecutorId) {
    store.selectProsecutor(prosecutorId);
}

function handleVerdictAcknowledge() {
    store.acknowledgeVerdict();
    isVerdictOpen.value = false;
}

function linkAccount() {
    router.push({ name: 'accountLinkInstitutions', query: { mode: 'add' } });
}

function openPersonalRanking() {
    router.push({ name: 'personalRanking' });
}

function resetDemo() {
    store.resetDemo();
    isConsentOpen.value = true;
}

function setDemoSuccess() {
    store.setDemoVerdict(MOCK_VERDICT_SUCCESS);
    isVerdictOpen.value = true;
}

function setDemoFail() {
    store.setDemoVerdict(MOCK_VERDICT_FAIL);
    isVerdictOpen.value = true;
}
</script>

<template>
    <div class="personal-home">
        <!-- 오버레이 -->
        <PersonalMissionConsentSheet v-model="isConsentOpen" @agree="handleAgree" />
        <PersonalTangiSheet
            v-model="isTangiSheetOpen"
            :current-prosecutor-id="store.selectedProsecutorId"
            @confirm="handleProsecutorConfirm"
        />
        <PersonalVerdictModal
            v-model="isVerdictOpen"
            :verdict="store.pendingVerdict"
            @acknowledge="handleVerdictAcknowledge"
        />

        <!-- 헤더: 기본 모드 (active / verdict) -->
        <PersonalCourtHeader
            v-if="store.screenState === 'active' || store.screenState === 'verdict'"
            :court-image="courtSupreme"
            :date="courtDate"
            :prosecutor-image="store.selectedProsecutor?.image"
            :prosecutor-name="store.selectedProsecutor?.name"
            :quote="'&quot;오늘은 ' + store.briefing.categoryName + '을 지켜보겠습니다&quot;'"
            :has-notification="true"
        />

        <!-- 헤더: 축소 모드 (no-account) -->
        <PersonalCourtHeader
            v-else-if="store.screenState === 'no-account'"
            :court-image="courtSupreme"
            :date="shortDate"
            compact
            compact-title="수사할 증거가<br>없습니다"
        />

        <!-- 헤더: 축소 모드 (insufficient) -->
        <PersonalCourtHeader
            v-else-if="store.screenState === 'insufficient'"
            :court-image="courtSupreme"
            :date="shortDate"
            compact
            compact-title="아직 수사할 증거가<br>모이지 않았습니다"
        />

        <!-- 메인 컨텐츠 -->
        <main class="personal-home__content">
            <!-- 화면 01: 기본 (진행 중) -->
            <template v-if="store.screenState === 'active' || store.screenState === 'verdict'">
                <PersonalBriefingCard
                    :category-name="store.briefing.categoryName"
                    :alibi-condition="store.briefing.alibiCondition"
                    :current-amount="store.briefing.currentAmount"
                    :limit-amount="store.briefing.limitAmount"
                    :streak-days="store.briefing.streakDays"
                    :prosecutor-name="store.selectedProsecutor?.name"
                    :prosecutor-image="store.selectedProsecutor?.image"
                    @prosecutor-click="openTangiSheet"
                />

                <PersonalWatchlistCard
                    :items="store.watchlist"
                    :week-range="store.watchlistMeta.weekRange"
                    :current-index="store.watchlistMeta.currentIndex"
                    :total-count="store.watchlistMeta.totalCount"
                    :comment="store.watchlistMeta.comment"
                    :uncategorized-warning="store.watchlistMeta.uncategorizedWarning"
                />

                <PersonalScoreCard
                    :score="store.monthlyScore.score"
                    :percentile="store.monthlyScore.percentile"
                    :next-tier-gap="store.monthlyScore.nextTierGap"
                    :tier-progress="store.monthlyScore.tierProgress"
                    @report-click="openPersonalRanking"
                />

                <div class="personal-home__verdict-info">
                    <svg class="personal-home__gavel-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                        <path d="M14.5 3.5l6 6M4 20l6.5-6.5M2 22l2-2M14 4l-9.5 9.5c-.4.4-.4 1 0 1.4l4.1 4.1c.4.4 1 .4 1.4 0L19.5 9.5" />
                        <path d="M9.5 8.5l5 5" />
                    </svg>
                    <span>오늘 자정에 판정되고, 곧바로 내일 사건이 배정돼요</span>
                </div>
            </template>

            <!-- 화면 06: 계좌 미연동 -->
            <template v-else-if="store.screenState === 'no-account'">
                <PersonalNoAccountCard @link-account="linkAccount" />
            </template>

            <!-- 화면 05: 증거 부족 -->
            <template v-else-if="store.screenState === 'insufficient'">
                <div class="personal-home__insufficient-banner">
                    <img
                        :src="store.selectedProsecutor?.image"
                        alt=""
                        class="personal-home__insufficient-tangi"
                    />
                    <div class="personal-home__insufficient-text">
                        <div class="personal-home__insufficient-title">
                            증거(소비 기록)가 모이는 동안<br />공통 사건을 배정해 드려요
                        </div>
                        <div class="personal-home__insufficient-sub">
                            증거가 쌓이면 요주의 대상 3곳을 뽑아 맞춤 사건이 열려요.
                        </div>
                    </div>
                </div>

                <div class="personal-home__conditions-card">
                    <div class="personal-home__conditions-title">맞춤 사건이 열리는 조건</div>
                    <div class="personal-home__conditions-list">
                        <div class="personal-home__condition">
                            <span class="personal-home__condition-icon personal-home__condition-icon--done">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                                    stroke="var(--tt-success)" stroke-width="3" stroke-linecap="round"
                                    stroke-linejoin="round">
                                    <path d="M5 12.5l4.5 4.5L19 7" />
                                </svg>
                            </span>
                            <span class="personal-home__condition-label">계좌 연동</span>
                            <span class="personal-home__condition-value personal-home__condition-value--done">완료</span>
                        </div>
                        <div class="personal-home__condition">
                            <span class="personal-home__condition-icon personal-home__condition-icon--progress">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                                    stroke="var(--tt-accent-deep)" stroke-width="2.2"
                                    stroke-linecap="round" stroke-linejoin="round">
                                    <circle cx="12" cy="12" r="8" />
                                    <path d="M12 8v4.5l3 1.8" />
                                </svg>
                            </span>
                            <span class="personal-home__condition-label">최근 28일 소비 데이터</span>
                            <span class="personal-home__condition-value personal-home__condition-value--progress">
                                {{ store.dataRequirements.availableDays }}일째
                            </span>
                        </div>
                        <div class="personal-home__condition">
                            <span class="personal-home__condition-icon personal-home__condition-icon--pending">
                                <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
                                    stroke="var(--tt-text-muted)" stroke-width="2.6"
                                    stroke-linecap="round">
                                    <circle cx="6" cy="12" r="1" />
                                    <circle cx="12" cy="12" r="1" />
                                    <circle cx="18" cy="12" r="1" />
                                </svg>
                            </span>
                            <span class="personal-home__condition-label">전체 소비 50건 확보</span>
                            <span class="personal-home__condition-value personal-home__condition-value--pending">
                                {{ store.dataRequirements.transactionCount }} / {{ store.dataRequirements.requiredTransactionCount }}
                            </span>
                        </div>
                    </div>
                    <div class="personal-home__progress-bar">
                        <div
                            class="personal-home__progress-fill"
                            :style="{ width: dataProgress + '%' }"
                        ></div>
                    </div>
                    <button type="button" class="personal-home__link-more" @click="linkAccount">
                        계좌 더 연동하기 ›
                    </button>
                </div>

                <div class="personal-home__common-mission">
                    <span class="personal-home__common-badge">공통 사건 · 절대형</span>
                    <div class="personal-home__common-title">{{ store.commonMission.title }}</div>
                    <div class="personal-home__common-status">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none"
                            stroke="var(--tt-success)" stroke-width="2.4" stroke-linecap="round"
                            stroke-linejoin="round">
                            <circle cx="12" cy="12" r="8.5" />
                            <path d="M8.5 12.5l2.5 2.5 4.5-5" />
                        </svg>
                        <span>
                            현재까지 {{ store.commonMission.category }} 지출
                            <b>{{ formatWon(store.commonMission.currentAmount) }}</b>
                            · 인정 시 +{{ store.commonMission.rewardPoints }}점
                        </span>
                    </div>
                </div>
            </template>
        </main>

        <ChallengeModeTabBar active-mode="personal" />

        <!-- 데모 버튼 -->
        <div v-if="isDevelopment" class="personal-home__dev-controls">
            <button type="button" class="personal-home__dev-btn" @click="resetDemo">
                초기화
            </button>
            <button type="button" class="personal-home__dev-btn" @click="setDemoSuccess">
                미션 성공 팝업
            </button>
            <button type="button" class="personal-home__dev-btn" @click="setDemoFail">
                미션 실패 팝업
            </button>
        </div>
    </div>
</template>

<style scoped src="./PersonalMissionHomeView.css"></style>
