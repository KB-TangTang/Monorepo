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
import PersonalTutorialOverlay from '@/components/challenge/personal/PersonalTutorialOverlay.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import { usePersonalMissionChallengeStore } from '@/stores/personalMission';
import { useConsentStore } from '@/stores/consent';
import { useAuthStore } from '@/stores/auth';
import {
    formatCourtDate,
    calculateDataProgress,
    formatWon,
    formatMissionAssignmentSummary,
    toWatchCategoryModel,
    toWeeklyVerdictModel,
} from '@/services/personalMissionFlow';
import { hasSeenPersonalTutorial, markPersonalTutorialSeen } from '@/services/tutorialGuide';
import { MOCK_VERDICT_SUCCESS, MOCK_VERDICT_FAIL } from '@/fixtures/personalChallenge';
import courtSupreme from '@/assets/images/court/court_supreme.png';
import withdrawnTangi from '@/assets/images/emotions/13_sobbing.png';
import { CHALLENGE_CONSENT_STATE, resolveChallengeConsentState } from '@/services/challengeConsent';

const router = useRouter();
const store = usePersonalMissionChallengeStore();
const consentStore = useConsentStore();
const authStore = useAuthStore();

const isConsentOpen = ref(false);
const isConsentSubmitting = ref(false);
const consentError = ref('');
const pageError = ref('');
const isTangiSheetOpen = ref(false);
const isVerdictOpen = ref(false);
const showTutorial = ref(false);
const isDevelopment = import.meta.env.DEV;
const isReassigning = ref(false);
const devActionMessage = ref('');

const courtDate = computed(() => formatCourtDate());
const shortDate = computed(() => {
    const d = new Date();
    return `${d.getMonth() + 1}월 ${d.getDate()}일`;
});

const dataProgress = computed(() => calculateDataProgress(store.dataRequirements));
const watchCategoryModel = computed(() => toWatchCategoryModel(store.categoryAnalysis));
const weeklyVerdictModel = computed(() => toWeeklyVerdictModel(store.missionStreak));

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

onMounted(async () => {
    store.hydrate();

    try {
        await consentStore.loadMyConsents();
        const challengeConsent = consentStore.myConsents.find((item) => item.type === 'CHALLENGE');
        const consentState = resolveChallengeConsentState(challengeConsent);
        store.setConsentState(consentState);

        if (consentState !== CHALLENGE_CONSENT_STATE.FIRST) {
            await Promise.all([
                store.loadTodayMission(),
                store.loadCategoryAnalysis(),
                store.loadMissionStreak(),
                store.loadMissionMonthlyScore(),
            ]);
        }
    } catch (err) {
        store.consentState = 'ERROR';
        pageError.value = err.message ?? '챌린지 정보를 불러오지 못했어요.';
    }

    if (store.consentState === CHALLENGE_CONSENT_STATE.FIRST) {
        isConsentOpen.value = true;
        return;
    }

    if (store.screenState === 'error' || store.screenState === 'withdrawn') return;

    if (!hasSeenPersonalTutorial()) {
        showTutorial.value = true;
        return;
    }

    if (store.hasPendingVerdict) {
        isVerdictOpen.value = true;
    }
});

async function handleAgree() {
    if (isConsentSubmitting.value) {
        return;
    }
    isConsentSubmitting.value = true;
    consentError.value = '';
    try {
        await consentStore.save('CHALLENGE', [{ type: 'CHALLENGE', agreed: true }]);
        store.agree();
        await store.waitForTodayMission();
        await Promise.all([
            store.loadCategoryAnalysis(),
            store.loadMissionStreak(),
            store.loadMissionMonthlyScore(),
        ]);
        isConsentOpen.value = false;
        if (!hasSeenPersonalTutorial()) {
            afterOverlayClosed(() => {
                showTutorial.value = true;
            });
        }
    } catch (err) {
        consentError.value = err.message ?? '챌린지 참여 동의를 저장하지 못했어요.';
    } finally {
        isConsentSubmitting.value = false;
    }
}

/*
 * 완료 저장은 서버(tbl_user.tutorial_seen_at)로 나간다 — 비동기다.
 * 저장 실패는 markPersonalTutorialSeen() 안에서 삼키므로(다음에 한 번 더 뜨는 게 전부)
 * 여기서 따로 오류를 처리하지 않는다.
 */
async function onTutorialComplete() {
    await markPersonalTutorialSeen();
}

function openTangiSheet() {
    isTangiSheetOpen.value = true;
}

async function handleProsecutorConfirm(prosecutorId) {
    try {
        const user = await store.saveProsecutorDifficulty(prosecutorId);
        authStore.mergeUser(user);
        devActionMessage.value = `${store.selectedProsecutor?.name} 난이도로 저장됐어요. 오늘 미션 재배정을 누르면 새 난이도가 적용됩니다.`;
    } catch (err) {
        devActionMessage.value = err.message ?? '담당 검사 난이도를 저장하지 못했어요.';
    }
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

function openConsentManage() {
    router.push({ name: 'myConsents' });
}

function handleConsentLater() {
    afterOverlayClosed(() => {
        router.push({ name: 'home' });
    });
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

function showWithdrawnWithoutMissionDemo() {
    store.setDemoWithdrawnWithoutMission();
}

async function reassignTodayMission() {
    if (isReassigning.value) return;
    isReassigning.value = true;
    devActionMessage.value = '';
    try {
        const previousMission = store.todayMission;
        const reassignedMission = await store.reassignTodayMission();
        devActionMessage.value = `${formatMissionAssignmentSummary(previousMission)} → ${formatMissionAssignmentSummary(reassignedMission)}`;
    } catch (err) {
        devActionMessage.value = err.message ?? '오늘 미션 재배정에 실패했어요.';
    } finally {
        isReassigning.value = false;
    }
}
</script>

<template>
    <div class="personal-home">
        <!-- 오버레이 -->
        <PersonalTutorialOverlay v-model="showTutorial" @complete="onTutorialComplete" />
        <PersonalMissionConsentSheet
            v-model="isConsentOpen"
            :loading="isConsentSubmitting"
            :error-message="consentError"
            @agree="handleAgree"
            @later="handleConsentLater"
        />
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
            :quote="'&quot;오늘은 ' + store.todayBriefing.categoryName + '을\n지켜보겠습니다&quot;'"
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

        <!-- 철회 후 오늘 미션 없음: 법원·날짜·알림 헤더는 기본 크기로 유지 -->
        <PersonalCourtHeader
            v-else-if="store.screenState === 'withdrawn'"
            :court-image="courtSupreme"
            :date="courtDate"
        />

        <!-- 메인 컨텐츠 -->
        <main
            class="personal-home__content"
            :class="{ 'personal-home__content--withdrawn': store.screenState === 'withdrawn' }"
        >
            <StateLoading v-if="store.screenState === 'loading'" />

            <StateError
                v-else-if="store.screenState === 'error'"
                title="챌린지 정보를 불러오지 못했어요"
                :message="pageError"
                :retryable="false"
            />

            <section
                v-else-if="store.screenState === 'withdrawn'"
                class="personal-home__withdrawn-state"
            >
                <img
                    :src="withdrawnTangi"
                    alt="챌린지 참여 중지를 아쉬워하는 탕이"
                    class="personal-home__withdrawn-tangi"
                />
                <h2>챌린지 참여가 중지되었어요</h2>
                <p>다시 참여하려면 마이페이지의 동의 관리에서 챌린지 동의를 변경해주세요.</p>
                <BaseButton variant="secondary" @click="openConsentManage">
                    동의 관리로 이동
                </BaseButton>
            </section>

            <!-- 화면 01: 기본 (진행 중) -->
            <template v-if="store.screenState === 'active' || store.screenState === 'verdict'">
                <div
                    v-if="store.consentState === CHALLENGE_CONSENT_STATE.WITHDRAWN"
                    class="personal-home__withdrawn-notice"
                >
                    챌린지 동의를 철회해 오늘 미션까지만 확인할 수 있어요. 내일부터 새 미션이
                    배정되지 않아요.
                </div>
                <PersonalBriefingCard
                    :mission-title="store.todayBriefing.missionTitle"
                    :mission-content="store.todayBriefing.missionContent"
                    :category-name="store.todayBriefing.categoryName"
                    :alibi-condition="store.todayBriefing.alibiCondition"
                    :current-amount="store.todayBriefing.currentAmount"
                    :limit-amount="store.todayBriefing.limitAmount"
                    :prosecutor-name="store.selectedProsecutor?.name"
                    :prosecutor-image="store.selectedProsecutor?.image"
                    @prosecutor-click="openTangiSheet"
                />

                <PersonalWatchlistCard
                    :items="watchCategoryModel.items"
                    :analysis-period="watchCategoryModel.period"
                />

                <PersonalScoreCard
                    :week-days="weeklyVerdictModel.days"
                    :streak-days="weeklyVerdictModel.streakDays"
                    :prosecutor-image="store.selectedProsecutor?.image"
                    :score="store.monthlyScore.score"
                    :percentile="store.monthlyScore.percentile"
                    :next-tier-gap="store.monthlyScore.nextTierGap"
                    :tier-progress="store.monthlyScore.tierProgress"
                    @report-click="openPersonalRanking"
                />

                <div class="personal-home__verdict-info">
                    <svg
                        class="personal-home__gavel-icon"
                        width="14"
                        height="14"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        aria-hidden="true"
                    >
                        <path
                            d="M14.5 3.5l6 6M4 20l6.5-6.5M2 22l2-2M14 4l-9.5 9.5c-.4.4-.4 1 0 1.4l4.1 4.1c.4.4 1 .4 1.4 0L19.5 9.5"
                        />
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
                            <span
                                class="personal-home__condition-icon personal-home__condition-icon--done"
                            >
                                <svg
                                    width="14"
                                    height="14"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="var(--tt-success)"
                                    stroke-width="3"
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                >
                                    <path d="M5 12.5l4.5 4.5L19 7" />
                                </svg>
                            </span>
                            <span class="personal-home__condition-label">계좌 연동</span>
                            <span
                                class="personal-home__condition-value personal-home__condition-value--done"
                                >완료</span
                            >
                        </div>
                        <div class="personal-home__condition">
                            <span
                                class="personal-home__condition-icon personal-home__condition-icon--progress"
                            >
                                <svg
                                    width="14"
                                    height="14"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="var(--tt-accent-deep)"
                                    stroke-width="2.2"
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                >
                                    <circle cx="12" cy="12" r="8" />
                                    <path d="M12 8v4.5l3 1.8" />
                                </svg>
                            </span>
                            <span class="personal-home__condition-label"
                                >최근 28일 소비 데이터</span
                            >
                            <span
                                class="personal-home__condition-value personal-home__condition-value--progress"
                            >
                                {{ store.dataRequirements.availableDays }}일째
                            </span>
                        </div>
                        <div class="personal-home__condition">
                            <span
                                class="personal-home__condition-icon personal-home__condition-icon--pending"
                            >
                                <svg
                                    width="14"
                                    height="14"
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="var(--tt-text-muted)"
                                    stroke-width="2.6"
                                    stroke-linecap="round"
                                >
                                    <circle cx="6" cy="12" r="1" />
                                    <circle cx="12" cy="12" r="1" />
                                    <circle cx="18" cy="12" r="1" />
                                </svg>
                            </span>
                            <span class="personal-home__condition-label">전체 소비 50건 확보</span>
                            <span
                                class="personal-home__condition-value personal-home__condition-value--pending"
                            >
                                {{ store.dataRequirements.transactionCount }} /
                                {{ store.dataRequirements.requiredTransactionCount }}
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
                        <svg
                            width="15"
                            height="15"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="var(--tt-success)"
                            stroke-width="2.4"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        >
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
            <span v-if="devActionMessage" class="personal-home__dev-message">{{
                devActionMessage
            }}</span>
            <button
                type="button"
                class="personal-home__dev-btn"
                :disabled="isReassigning"
                @click="reassignTodayMission"
            >
                {{ isReassigning ? '재배정 중...' : '오늘 미션 재배정' }}
            </button>
            <button type="button" class="personal-home__dev-btn" @click="resetDemo">초기화</button>
            <button
                type="button"
                class="personal-home__dev-btn"
                @click="showWithdrawnWithoutMissionDemo"
            >
                철회·미션 없음 화면
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
