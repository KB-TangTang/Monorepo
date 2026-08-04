<script setup>
import { onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import PersonalMissionConsentSheet from '@/components/challenge/personal/PersonalMissionConsentSheet.vue';
import PersonalMissionCard from '@/components/challenge/personal/PersonalMissionCard.vue';
import PersonalMissionDataGuide from '@/components/challenge/personal/PersonalMissionDataGuide.vue';
import PersonalMissionStreakCard from '@/components/challenge/personal/PersonalMissionStreakCard.vue';
import { MOCK_PERSONAL_MISSION_STREAK } from '@/fixtures/personalMission';
import { usePersonalMissionChallengeStore } from '@/stores/personalMission';

const router = useRouter();
const challengeStore = usePersonalMissionChallengeStore();
const isConsentOpen = ref(false);
const isDevelopment = import.meta.env.DEV;

onMounted(() => {
    challengeStore.hydrate();

    if (!challengeStore.hasAgreed) {
        isConsentOpen.value = true;
        return;
    }

    /*
     * 맞춤 미션에 필요한 소비 데이터가 충분하고 난이도 설정이 끝나지 않았다면
     * 소개 화면을 거치지 않고 난이도 설정으로 바로 이동
     */
    if (challengeStore.hasEnoughData && !challengeStore.hasCompletedSetup) {
        router.replace({
            name: 'personalMissionChallengeDifficulty',
        });
    }
});

function handleAgree() {
    challengeStore.agree();

    if (challengeStore.hasEnoughData) {
        router.push({
            name: 'personalMissionChallengeDifficulty',
        });
    }
}

function resetDemo() {
    challengeStore.resetDemo();
    isConsentOpen.value = true;
}
</script>

<template>
    <div class="personal-mission-home">
        <PersonalMissionConsentSheet v-model="isConsentOpen" @agree="handleAgree" />

        <header class="personal-mission-home__hero">
            <p>오늘의 개인 챌린지 · 7월 29일</p>

            <h1 v-if="challengeStore.hasEnoughData">오늘의 미션이<br />도착했어요</h1>

            <h1 v-else>오늘은 공통<br />미션이에요</h1>
        </header>

        <main class="personal-mission-home__content">
            <PersonalMissionCard
                :mission="challengeStore.currentMission"
                :difficulty="challengeStore.selectedDifficulty"
                :personalized="challengeStore.hasEnoughData"
            />

            <PersonalMissionDataGuide
                v-if="!challengeStore.hasEnoughData"
                :profile="challengeStore.profile"
            />

            <PersonalMissionStreakCard v-else :days="MOCK_PERSONAL_MISSION_STREAK" />

            <div class="personal-mission-home__mode">
                <button type="button" class="personal-mission-home__mode--active">♙ 개인</button>

                <button type="button">♟ 그룹</button>
            </div>
        </main>

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
