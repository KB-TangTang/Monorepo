import { defineStore } from 'pinia';
import {
    MOCK_DATA_REQUIREMENTS,
    MOCK_PROSECUTORS,
    MOCK_TODAY_BRIEFING,
    MOCK_VERDICT_SUCCESS,
    MOCK_WEEKLY_WATCHLIST,
    MOCK_WATCHLIST_META,
    MOCK_WEEKLY_VERDICT,
    MOCK_MONTHLY_SCORE,
    MOCK_COMMON_MISSION,
} from '@/fixtures/personalChallenge';
import { MOCK_PERSONAL_MISSION_PROFILE } from '@/fixtures/personalMission';
import {
    hasEnoughPersonalMissionData,
    toTodayMissionBriefing,
} from '@/services/personalMissionFlow';
import { fetchTodayMission, reassignTodayMission as requestTodayMissionReassignment } from '@/api/personalMission';
import { CHALLENGE_CONSENT_STATE } from '@/services/challengeConsent';

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
        selectedProsecutorId: DEFAULT_PROSECUTOR_ID,
        pendingVerdict: null,
        courtMode: 'supreme',
        isHydrated: false,

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
            if (this.consentState === CHALLENGE_CONSENT_STATE.WITHDRAWN && !this.todayMission) {
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
            this.pendingVerdict = saved.pendingVerdict ?? null;
            this.courtMode = saved.courtMode ?? 'supreme';
            this.isHydrated = true;
        },

        save() {
            localStorage.setItem(
                STORAGE_KEY,
                JSON.stringify({
                    hasAgreed: this.hasAgreed,
                    selectedProsecutorId: this.selectedProsecutorId,
                    pendingVerdict: this.pendingVerdict,
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

        acknowledgeVerdict() {
            this.pendingVerdict = null;
            this.save();
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
            this.save();
        },
    },
});
