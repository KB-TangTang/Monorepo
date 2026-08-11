import { defineStore } from 'pinia';
import {
    MOCK_DATA_REQUIREMENTS,
    MOCK_PROSECUTORS,
    MOCK_TODAY_BRIEFING,
    MOCK_VERDICT_SUCCESS,
    MOCK_WEEKLY_WATCHLIST,
    MOCK_WATCHLIST_META,
    MOCK_MONTHLY_SCORE,
    MOCK_COMMON_MISSION,
} from '@/fixtures/personalChallenge';
import {
    MOCK_PERSONAL_MISSION_PROFILE,
} from '@/fixtures/personalMission';
import {
    hasEnoughPersonalMissionData,
} from '@/services/personalMissionFlow';

const STORAGE_KEY = 'tangtang-personal-mission-challenge';

/*
 * 2026-08-11 이전에 저장된 검사 성향 ID 를 새 코드로 옮긴다.
 * localStorage 에 남은 구 값을 그대로 두면 MOCK_PROSECUTORS 에서 못 찾아
 * 선택된 탕이가 사라진 것처럼 보인다.
 */
const LEGACY_PROSECUTOR_ID = { TOUGH: 'HARD', STRICT: 'NORMAL', LENIENT: 'EASY' };
const DEFAULT_PROSECUTOR_ID = 'NORMAL';

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
            if (!this.hasAgreed) return 'consent';
            if (!this.isAccountLinked) return 'no-account';
            if (!this.hasEnoughData) return 'insufficient';
            if (this.hasPendingVerdict) return 'verdict';
            return 'active';
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
            this.save();
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
            this.selectedProsecutorId = DEFAULT_PROSECUTOR_ID;
            this.pendingVerdict = null;
            this.courtMode = 'supreme';
            this.isHydrated = true;
        },

        /* 데모용: 판정 테스트 */
        setDemoVerdict(verdict) {
            this.pendingVerdict = verdict;
            this.save();
        },
    },
});
