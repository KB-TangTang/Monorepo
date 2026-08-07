import { defineStore } from 'pinia';
import {
    MOCK_ABSOLUTE_MISSION,
    MOCK_PERSONAL_MISSION_PROFILE,
    MOCK_RELATIVE_MISSION,
    PERSONAL_MISSION_DIFFICULTIES,
} from '@/fixtures/personalMission';
import {
    hasEnoughPersonalMissionData,
    selectPersonalMissionType,
    shouldShowPersonalMissionUnlock,
} from '@/services/personalMissionFlow';

const STORAGE_KEY = 'tangtang-personal-mission-challenge';

/*
 * 여러 라우트에서 공통으로 사용하는 상태
 *
 * 개인 미션 홈과 난이도 설정 화면이 같은 동의 여부와 난이도를
 * 사용하기 때문에 Pinia에 저장
 */
export const usePersonalMissionChallengeStore = defineStore('personalMissionChallenge', {
    state: () => ({
        profile: MOCK_PERSONAL_MISSION_PROFILE,
        hasAgreed: false,
        hasSeenTutorial: false,
        wasDataInsufficient: false,
        hasSeenDataUnlock: false,
        hasCompletedSetup: false,
        selectedDifficultyId: 'NORMAL',
        missionType: 'ABSOLUTE',
        isHydrated: false,
    }),

    getters: {
        hasEnoughData(state) {
            return hasEnoughPersonalMissionData(state.profile);
        },

        shouldShowDataUnlock() {
            return shouldShowPersonalMissionUnlock({
                hasAgreed: this.hasAgreed,
                hasEnoughData: this.hasEnoughData,
                wasDataInsufficient: this.wasDataInsufficient,
                hasSeenDataUnlock: this.hasSeenDataUnlock,
            });
        },

        selectedDifficulty(state) {
            return PERSONAL_MISSION_DIFFICULTIES.find(
                (difficulty) => difficulty.id === state.selectedDifficultyId,
            );
        },

        currentMission(state) {
            return state.missionType === 'RELATIVE' ? MOCK_RELATIVE_MISSION : MOCK_ABSOLUTE_MISSION;
        },
    },

    actions: {
        /*
         * localStorage에 저장했던 임시 상태를 불러옴
         * 실제 서버 연동 후에는 사용자 설정 조회 API로 교체할 부분임
         */
        hydrate() {
            if (this.isHydrated) {
                return;
            }

            const savedState = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '{}');

            this.hasAgreed = savedState.hasAgreed ?? false;
            this.hasSeenTutorial = savedState.hasSeenTutorial ?? false;
            this.hasCompletedSetup = savedState.hasCompletedSetup ?? false;
            this.wasDataInsufficient =
                savedState.wasDataInsufficient ??
                (this.hasAgreed && !this.hasSeenTutorial && !this.hasCompletedSetup);
            this.hasSeenDataUnlock = savedState.hasSeenDataUnlock ?? false;
            this.selectedDifficultyId = savedState.selectedDifficultyId ?? 'NORMAL';
            this.missionType = savedState.missionType ?? selectPersonalMissionType(this.profile);
            this.isHydrated = true;
        },

        save() {
            localStorage.setItem(
                STORAGE_KEY,
                JSON.stringify({
                    hasAgreed: this.hasAgreed,
                    hasSeenTutorial: this.hasSeenTutorial,
                    wasDataInsufficient: this.wasDataInsufficient,
                    hasSeenDataUnlock: this.hasSeenDataUnlock,
                    hasCompletedSetup: this.hasCompletedSetup,
                    selectedDifficultyId: this.selectedDifficultyId,
                    missionType: this.missionType,
                }),
            );
        },

        agree() {
            this.hasAgreed = true;
            this.wasDataInsufficient = !this.hasEnoughData;
            this.missionType = selectPersonalMissionType(this.profile);
            this.save();
        },

        acknowledgeDataUnlock() {
            this.hasSeenDataUnlock = true;
            this.hasSeenTutorial = true;
            this.missionType = 'RELATIVE';
            this.save();
        },

        selectDifficulty(difficultyId) {
            this.selectedDifficultyId = difficultyId;
        },

        completeTutorial() {
            this.hasSeenTutorial = true;
            this.save();
        },

        /** 마이페이지 > 튜토리얼 다시 보기. 플래그만 되돌리고 재생은 홈 화면이 한다 */
        replayTutorial() {
            this.hasSeenTutorial = false;
            this.wasDataInsufficient = false;
            this.hasSeenDataUnlock = false;
            this.save();
        },

        completeDifficultySetup() {
            this.hasCompletedSetup = true;
            this.save();
        },

        resetDemo() {
            localStorage.removeItem(STORAGE_KEY);

            this.hasAgreed = false;
            this.hasSeenTutorial = false;
            this.hasCompletedSetup = false;
            this.selectedDifficultyId = 'NORMAL';
            this.missionType = 'ABSOLUTE';
            this.isHydrated = true;
        },
    },
});
