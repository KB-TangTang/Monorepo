import { defineStore } from 'pinia';
import {
    MOCK_DATA_REQUIREMENTS,
    MOCK_PROSECUTORS,
    MOCK_TODAY_BRIEFING,
    MOCK_WEEKLY_WATCHLIST,
    MOCK_WATCHLIST_META,
    MOCK_WEEKLY_VERDICT,
    MOCK_MONTHLY_SCORE,
    MOCK_COMMON_MISSION,
} from '@/fixtures/personalChallenge';
import { MOCK_PERSONAL_MISSION_PROFILE } from '@/fixtures/personalMission';
import {
    hasEnoughPersonalMissionData,
    toMissionVerdictModel,
    toTodayMissionBriefing,
} from '@/services/personalMissionFlow';
import {
    acknowledgeMissionVerdict as requestMissionVerdictAcknowledge,
    fetchMissionCategoryAnalysis,
    fetchMissionMonthlyScore,
    fetchPendingMissionVerdict,
    fetchMissionStreak,
    fetchTodayMission,
    reassignTodayMission as requestTodayMissionReassignment,
    syncPersonalMissionUnlock as requestPersonalMissionUnlockSync,
    acknowledgePersonalMissionUnlock as requestPersonalMissionUnlockAcknowledge,
} from '@/api/personalMission';
import { CHALLENGE_CONSENT_STATE } from '@/services/challengeConsent';
import { normalizeMonthlyScore } from '@/services/missionMonthlyScore';
import { updateMyDifficulty } from '@/api/user';
import tangiVerdictSuccess from '@/assets/images/prosecutor_tangtang/field_verification_v1_one_leg_no_emphasis.png';
import tangiVerdictFail from '@/assets/images/prosecutor_tangtang/scene_raid_v1_one_leg_no_emphasis.png';

const STORAGE_KEY = 'tangtang-personal-mission-challenge';

/*
 * 2026-08-11 이전에 저장된 검사 성향 ID 를 새 코드로 옮긴다.
 * localStorage 에 남은 구 값을 그대로 두면 MOCK_PROSECUTORS 에서 못 찾아
 * 선택된 탕이가 사라진 것처럼 보인다.
 */
const LEGACY_PROSECUTOR_ID = { TOUGH: 'HARD', STRICT: 'NORMAL', LENIENT: 'EASY' };
const DEFAULT_PROSECUTOR_ID = 'NORMAL';
const TODAY_MISSION_RETRY_COUNT = 5;
const TODAY_MISSION_RETRY_DELAY_MS = 200;
const DIFFICULTY_IDS = { EASY: 1, NORMAL: 2, HARD: 3 };

function delay(milliseconds) {
    return new Promise((resolve) => window.setTimeout(resolve, milliseconds));
}

function normalizeProsecutorId(id) {
    return LEGACY_PROSECUTOR_ID[id] ?? id ?? DEFAULT_PROSECUTOR_ID;
}

/*
 * 개인 미션 챌린지 v4 — 주간 로테이션 상태
 *
 * 여러 라우트에서 공통으로 사용하는 상태를 Pinia에 저장한다.
 * 기존 v3 에서 난이도·튜토리얼 플로우가 제거되고,
 * 담당 탕이(검사) 전역 설정 + 판정 미확인 상태가 추가됐다.
 */
export const usePersonalMissionChallengeStore = defineStore('personalMissionChallenge', {
    state: () => ({
        profile: MOCK_PERSONAL_MISSION_PROFILE,
        hasAgreed: false,
        consentState: null,
        todayMission: null,
        categoryAnalysis: null,
        missionStreak: null,
        selectedProsecutorId: DEFAULT_PROSECUTOR_ID,
        selectedDifficultyId: DEFAULT_PROSECUTOR_ID,
        pendingVerdict: null,
        courtMode: 'supreme',
        isHydrated: false,
        missionUnlockStatus: 'UNTRACKED',

        /* mock 데이터 (API 교체 대상) */
        briefing: MOCK_TODAY_BRIEFING,
        watchlist: MOCK_WEEKLY_WATCHLIST,
        watchlistMeta: MOCK_WATCHLIST_META,
        monthlyScore: MOCK_MONTHLY_SCORE,
        dataRequirements: MOCK_DATA_REQUIREMENTS,
        commonMission: MOCK_COMMON_MISSION,
        weeklyVerdict: MOCK_WEEKLY_VERDICT,
    }),

    getters: {
        hasEnoughData(state) {
            if (state.categoryAnalysis) {
                return Boolean(state.categoryAnalysis.relativeEligible);
            }
            return hasEnoughPersonalMissionData(state.profile);
        },

        isAccountLinked(state) {
            return state.dataRequirements.accountLinked;
        },

        selectedProsecutor(state) {
            return MOCK_PROSECUTORS.find((p) => p.id === state.selectedProsecutorId);
        },

        hasPendingVerdict(state) {
            return state.pendingVerdict !== null;
        },

        /*
         * 화면 상태 분기 (우선순위 순서):
         * 1. 미동의 → consent
         * 2. 계좌 미연동 → no-account
         * 3. 데이터 부족 → insufficient
         * 4. 판정 미확인 → verdict
         * 5. 정상 → active
         */
        screenState() {
            if (this.consentState === null) return 'loading';
            if (this.consentState === 'ERROR') return 'error';
            if (this.consentState === CHALLENGE_CONSENT_STATE.FIRST) return 'consent';
            if (
                this.consentState === CHALLENGE_CONSENT_STATE.WITHDRAWN &&
                !this.todayMission &&
                !this.hasPendingVerdict
            ) {
                return 'withdrawn';
            }
            if (this.consentState === CHALLENGE_CONSENT_STATE.WITHDRAWN) {
                return this.hasPendingVerdict ? 'verdict' : 'active';
            }
            if (!this.isAccountLinked) return 'no-account';
            if (!this.hasEnoughData) return 'insufficient';
            if (this.hasPendingVerdict) return 'verdict';
            return 'active';
        },

        todayBriefing(state) {
            return toTodayMissionBriefing(state.todayMission) ?? state.briefing;
        },
    },

    actions: {
        hydrate() {
            if (this.isHydrated) return;

            const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');

            this.hasAgreed = saved.hasAgreed ?? false;
            this.selectedProsecutorId = normalizeProsecutorId(saved.selectedProsecutorId);
            this.pendingVerdict = null;
            this.courtMode = saved.courtMode ?? 'supreme';
            this.isHydrated = true;
        },

        save() {
            localStorage.setItem(
                STORAGE_KEY,
                JSON.stringify({
                    hasAgreed: this.hasAgreed,
                    selectedProsecutorId: this.selectedProsecutorId,
                    courtMode: this.courtMode,
                }),
            );
        },

        agree() {
            this.hasAgreed = true;
            this.consentState = CHALLENGE_CONSENT_STATE.ACTIVE;
            this.save();
        },

        setConsentState(consentState) {
            this.consentState = consentState;
            this.hasAgreed = consentState === CHALLENGE_CONSENT_STATE.ACTIVE;
        },

        async loadTodayMission() {
            try {
                this.todayMission = await fetchTodayMission();
                return true;
            } catch (error) {
                if (error.code === 'TODAY_MISSION_NOT_FOUND') {
                    this.todayMission = null;
                    return false;
                }
                throw error;
            }
        },

        async loadCategoryAnalysis() {
            this.categoryAnalysis = await fetchMissionCategoryAnalysis();
        },

        async loadMissionStreak() {
            this.missionStreak = await fetchMissionStreak();
        },

        async loadMissionMonthlyScore() {
            const monthlyScore = await fetchMissionMonthlyScore();
            this.monthlyScore = normalizeMonthlyScore(monthlyScore);
        },

        async syncMissionUnlock() {
            const result = await requestPersonalMissionUnlockSync(this.hasEnoughData);
            this.missionUnlockStatus = result.status;
            return Boolean(result.showUnlock);
        },

        async acknowledgeMissionUnlock() {
            const result = await requestPersonalMissionUnlockAcknowledge();
            this.missionUnlockStatus = result.status;
            return result;
        },

        async loadPendingVerdict() {
            const verdict = await fetchPendingMissionVerdict();
            this.pendingVerdict = toMissionVerdictModel(verdict, {
                success: tangiVerdictSuccess,
                fail: tangiVerdictFail,
            });
        },

        async waitForTodayMission() {
            for (let attempt = 0; attempt < TODAY_MISSION_RETRY_COUNT; attempt += 1) {
                if (await this.loadTodayMission()) {
                    return true;
                }
                if (attempt < TODAY_MISSION_RETRY_COUNT - 1) {
                    await delay(TODAY_MISSION_RETRY_DELAY_MS);
                }
            }
            return false;
        },

        async reassignTodayMission() {
            this.todayMission = await requestTodayMissionReassignment();
            return this.todayMission;
        },

        selectProsecutor(prosecutorId) {
            this.selectedProsecutorId = prosecutorId;
            this.save();
        },

        async saveProsecutorDifficulty(prosecutorId) {
            const user = await updateMyDifficulty(prosecutorId);
            if (Number(user?.difficultyId) !== DIFFICULTY_IDS[prosecutorId]) {
                throw new Error(
                    '담당 검사 난이도가 서버에 저장되지 않았어요. 서버를 다시 실행한 뒤 시도해 주세요.',
                );
            }
            this.selectedProsecutorId = prosecutorId;
            this.selectedDifficultyId = prosecutorId;
            this.save();
            return user;
        },

        selectDifficulty(difficultyId) {
            this.selectedDifficultyId = difficultyId;
        },

        async saveDifficulty() {
            const difficultyName = this.selectedDifficultyId;
            const user = await updateMyDifficulty(difficultyName);
            if (Number(user?.difficultyId) !== DIFFICULTY_IDS[difficultyName]) {
                throw new Error(
                    '선택한 난이도가 서버에 저장되지 않았어요. 서버를 다시 실행한 뒤 시도해주세요.',
                );
            }
            return user;
        },

        async acknowledgeVerdict() {
            if (!this.pendingVerdict) return;
            await requestMissionVerdictAcknowledge(this.pendingVerdict.assignmentId);
            this.pendingVerdict = null;
        },

        resetDemo() {
            localStorage.removeItem(STORAGE_KEY);

            this.hasAgreed = false;
            this.consentState = CHALLENGE_CONSENT_STATE.FIRST;
            this.todayMission = null;
            this.selectedProsecutorId = DEFAULT_PROSECUTOR_ID;
            this.pendingVerdict = null;
            this.courtMode = 'supreme';
            this.isHydrated = true;
        },

        setDemoWithdrawnWithoutMission() {
            this.hasAgreed = false;
            this.consentState = CHALLENGE_CONSENT_STATE.WITHDRAWN;
            this.todayMission = null;
        },

        /* 데모용: 판정 테스트 */
        setDemoVerdict(verdict) {
            this.pendingVerdict = verdict;
        },
    },
});
