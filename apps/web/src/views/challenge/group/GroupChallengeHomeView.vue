<script setup>
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
</script>

<template>
    <section class="group-challenge-home">
        <div class="group-challenge-home__content">
            <span aria-hidden="true">♟</span>
            <h1>그룹 챌린지</h1>
            <p>그룹 챌린지 화면은 다음 작업에서 연결할 예정이에요.</p>
        </div>

        <ChallengeModeTabBar active-mode="group" />
    </section>
</template>

<style scoped src="./GroupChallengeHomeView.css"></style>
