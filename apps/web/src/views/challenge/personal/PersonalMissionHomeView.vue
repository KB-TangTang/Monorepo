<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import PersonalMissionConsentSheet from '@/components/challenge/personal/PersonalMissionConsentSheet.vue';
import PersonalMissionCard from '@/components/challenge/personal/PersonalMissionCard.vue';
import PersonalMissionDataGuide from '@/components/challenge/personal/PersonalMissionDataGuide.vue';
import PersonalMissionHonorBanner from '@/components/challenge/personal/PersonalMissionHonorBanner.vue';
import PersonalMissionStreakCard from '@/components/challenge/personal/PersonalMissionStreakCard.vue';
import PersonalMissionTutorialModal from '@/components/challenge/personal/PersonalMissionTutorialModal.vue';
import PersonalMissionUnlockSheet from '@/components/challenge/personal/PersonalMissionUnlockSheet.vue';
import { MOCK_PERSONAL_MISSION_STREAK } from '@/fixtures/personalMission';
import { usePersonalMissionChallengeStore } from '@/stores/personalMission';

const router = useRouter();
const challengeStore = usePersonalMissionChallengeStore();
const isConsentOpen = ref(false);
const isTutorialOpen = ref(false);
const isDataUnlockOpen = ref(false);
const isDevelopment = import.meta.env.DEV;

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
    challengeStore.hydrate();

    if (!challengeStore.hasAgreed) {
        isConsentOpen.value = true;
        return;
    }

    if (challengeStore.shouldShowDataUnlock) {
        isDataUnlockOpen.value = true;
        return;
    }

    if (challengeStore.hasEnoughData && !challengeStore.hasSeenTutorial) {
        isTutorialOpen.value = true;
        return;
    }

    if (challengeStore.hasEnoughData && !challengeStore.hasCompletedSetup) {
        router.replace({
            name: 'personalMissionChallengeDifficulty',
        });
    }
});

function handleAgree() {
    challengeStore.agree();

    if (challengeStore.hasEnoughData) {
        afterOverlayClosed(() => {
            isTutorialOpen.value = true;
        });
    }
}

function finishTutorial() {
    challengeStore.completeTutorial();
    isTutorialOpen.value = false;
    afterOverlayClosed(() => {
        router.push({ name: 'personalMissionChallengeDifficulty' });
    });
}

function setUnlockedMissionDifficulty() {
    challengeStore.acknowledgeDataUnlock();
    isDataUnlockOpen.value = false;
    afterOverlayClosed(() => {
        router.push({ name: 'personalMissionChallengeDifficulty' });
    });
}

function viewUnlockedMission() {
    challengeStore.acknowledgeDataUnlock();
    challengeStore.completeDifficultySetup();
    isDataUnlockOpen.value = false;
}

function resetDemo() {
    challengeStore.resetDemo();
    isConsentOpen.value = true;
}

function openPersonalRanking() {
    router.push({ name: 'personalRanking' });
}

function linkMoreAccounts() {
    router.push({ name: 'accountLinkInstitutions', query: { mode: 'add' } });
}
</script>

<template>
    <div class="personal-mission-home">
        <PersonalMissionConsentSheet v-model="isConsentOpen" @agree="handleAgree" />
        <PersonalMissionTutorialModal
            v-model="isTutorialOpen"
            @complete="finishTutorial"
            @skip="finishTutorial"
        />
        <PersonalMissionUnlockSheet
            v-model="isDataUnlockOpen"
            :difficulty="challengeStore.selectedDifficulty"
            @set-difficulty="setUnlockedMissionDifficulty"
            @view-mission="viewUnlockedMission"
        />

        <header class="personal-mission-home__hero">
            <p>오늘의 재판 · 7월 29일</p>

            <h1 v-if="challengeStore.hasEnoughData">오늘의 미션이<br />도착했어요</h1>

            <h1 v-else>오늘은 공통<br />미션이에요</h1>
        </header>

        <main class="personal-mission-home__content">
            <PersonalMissionCard
                :mission="challengeStore.currentMission"
                :difficulty="challengeStore.selectedDifficulty"
                :personalized="challengeStore.hasEnoughData"
            />

            <PersonalMissionHonorBanner
                v-if="challengeStore.hasEnoughData"
                @open="openPersonalRanking"
            />

            <PersonalMissionDataGuide
                v-if="!challengeStore.hasEnoughData"
                :profile="challengeStore.profile"
                @link-account="linkMoreAccounts"
            />

            <PersonalMissionStreakCard v-else :days="MOCK_PERSONAL_MISSION_STREAK" />
        </main>

        <ChallengeModeTabBar active-mode="personal" />

        <button
            v-if="isDevelopment"
            type="button"
            class="personal-mission-home__reset"
            @click="resetDemo"
        >
            데모 초기화
        </button>
    </div>
</template>

<style scoped src="./PersonalMissionHomeView.css"></style>
