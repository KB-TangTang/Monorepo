<script setup>
import { useRoute, useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import GroupAiVerdictResultCard from '@/components/challenge/group/GroupAiVerdictResultCard.vue';
import GroupTieScoreCard from '@/components/challenge/group/GroupTieScoreCard.vue';
import { getAiVerdict } from '@/fixtures/groupChallengeAiVerdict';

const route = useRoute();
const router = useRouter();
const verdict = getAiVerdict(route.query.outcome);

function goGroupHome() {
    router.push({ name: 'groupChallenge' });
}

function goDetail() {
    router.push({ name: 'groupAiVerdictDetail', params: route.params });
}
</script>

<template>
    <main class="result-page">
        <DefenseCourtHeader>
            <template #nav-right
                ><span class="result-page__case">{{ verdict.caseNumber }}</span></template
            >
            <h1 class="result-page__title">최종 판결</h1>
            <p class="result-page__subtitle">
                {{ verdict.challengeName }} · {{ verdict.evaluationLabel }}
            </p>
        </DefenseCourtHeader>

        <section class="result-page__body">
            <GroupAiVerdictResultCard :verdict="verdict" />
            <GroupTieScoreCard
                :guilty-votes="verdict.guiltyVotes"
                :innocent-votes="verdict.innocentVotes"
                :outcome="verdict.outcome"
                class="result-page__score"
            />
        </section>

        <footer class="result-page__footer">
            <BaseButton variant="dark" size="lg" block @click="goGroupHome"
                >그룹 화면으로 돌아가기</BaseButton
            >
            <button type="button" class="result-page__detail" @click="goDetail">
                세부 정보 보기
            </button>
        </footer>
    </main>
</template>

<style scoped>
.result-page {
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    background: var(--tt-bg-subtle);
}
.result-page__case {
    color: var(--tt-gray-400);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
}
.result-page__title {
    margin-top: var(--tt-space-2);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
}
.result-page__subtitle {
    margin-top: var(--tt-space-2);
    color: var(--tt-gray-400);
    font-size: var(--tt-fs-caption);
}
.result-page__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: var(--tt-space-4);
    padding: var(--tt-space-4) var(--tt-space-5);
}
.result-page__score {
    margin-top: auto;
}
.result-page__footer {
    padding: var(--tt-space-3) var(--tt-space-5) var(--tt-space-6);
}
.result-page__detail {
    display: block;
    width: 100%;
    margin-top: var(--tt-space-3);
    padding: var(--tt-space-2);
    color: var(--tt-primary);
    background: transparent;
    border: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    cursor: pointer;
}
</style>
