<script setup>
import { onMounted } from 'vue';
import { useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import PersonalMissionDifficultyOption from '@/components/challenge/personal/PersonalMissionDifficultyOption.vue';
import { PERSONAL_MISSION_DIFFICULTIES } from '@/fixtures/personalMission';
import { usePersonalMissionChallengeStore } from '@/stores/personalMission';

const router = useRouter();
const challengeStore = usePersonalMissionChallengeStore();

onMounted(() => {
    challengeStore.hydrate();

    // 동의하지 않은 사용자가 URL로 직접 들어오면 개인 미션 홈으로 돌려보냅니다.
    if (!challengeStore.hasAgreed) {
        router.replace({ name: 'personalMissionChallenge' });
    }
});

function saveDifficulty() {
    challengeStore.completeDifficultySetup();

    // 저장 완료 후 뒤로가기로 설정 화면에 재진입하지 않도록 replace를 사용합니다.
    router.replace({ name: 'personalMissionChallenge' });
}
</script>

<template>
    <section class="difficulty">
        <header class="difficulty__header">
            <button type="button" aria-label="이전 화면" @click="router.back()">‹</button>
            <strong>개인 미션 난이도 설정</strong>
        </header>

        <div class="difficulty__title">
            <h1>얼마나 독하게<br />아껴볼까요?</h1>
            <p>난이도가 높을수록 목표가 어려워지고, 성공 시 받을 수 있는 점수도 커져요.</p>
        </div>

        <div class="difficulty__options">
            <PersonalMissionDifficultyOption
                v-for="difficulty in PERSONAL_MISSION_DIFFICULTIES"
                :key="difficulty.id"
                :difficulty="difficulty"
                :selected="challengeStore.selectedDifficultyId === difficulty.id"
                @select="challengeStore.selectDifficulty"
            />
        </div>

        <div class="difficulty__notice">
            ⓘ 오늘 배정된 미션은 그대로 유지되고, 내일 자정 배정분부터 새 난이도가 적용돼요.
        </div>

        <BaseButton size="lg" block @click="saveDifficulty">이 난이도로 저장</BaseButton>
    </section>
</template>

<style scoped src="./PersonalMissionDifficultyView.css"></style>
