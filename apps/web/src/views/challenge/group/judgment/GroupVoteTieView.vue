<script setup>
import { useRoute, useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import DefenseCourtHeader from '@/components/challenge/group/DefenseCourtHeader.vue';
import GroupTieScoreCard from '@/components/challenge/group/GroupTieScoreCard.vue';
import reviewImage from '@/assets/images/judgment/review_1.png';
import { getAiVerdict } from '@/fixtures/groupChallengeAiVerdict';

const route = useRoute();
const router = useRouter();
const verdict = getAiVerdict();

function startAiVerdict() {
    router.push({
        name: 'groupAiVerdict',
        params: route.params,
    });
}
</script>

<template>
    <main class="tie-page">
        <DefenseCourtHeader>
            <template #nav-right
                ><span class="tie-page__case">{{ verdict.caseNumber }}</span></template
            >
            <span class="tie-page__tag">그룹 투표 종료</span>
            <h1 class="tie-page__title">의견이 팽팽하게<br />맞섰어요</h1>
            <p class="tie-page__subtitle">{{ verdict.challengeName }} · 최종 투표 결과</p>
        </DefenseCourtHeader>

        <section class="tie-page__body">
            <GroupTieScoreCard
                :guilty-votes="verdict.guiltyVotes"
                :innocent-votes="verdict.innocentVotes"
            />
            <div class="tie-page__review">
                <span class="tie-page__review-copy">판사 탕이가 판결 기록을 검토 중이에요</span>
                <img :src="reviewImage" alt="서류를 검토하는 판사 탕탕이" />
            </div>
        </section>

        <footer class="tie-page__footer">
            <BaseButton variant="dark" size="lg" block @click="startAiVerdict"
                >판사 탕이에게 판결 받기</BaseButton
            >
        </footer>
    </main>
</template>

<style scoped>
.tie-page {
    min-height: 100dvh;
    display: flex;
    flex-direction: column;
    background: var(--tt-bg-subtle);
}
.tie-page__case {
    color: var(--tt-gray-400);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
}
.tie-page__tag {
    display: inline-flex;
    padding: 5px var(--tt-space-3);
    color: var(--tt-accent-700);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}
.tie-page__title {
    margin-top: var(--tt-space-3);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    letter-spacing: -0.01em;
}
.tie-page__subtitle {
    margin-top: var(--tt-space-2);
    color: var(--tt-gray-400);
    font-size: var(--tt-fs-caption);
}
.tie-page__body {
    display: flex;
    flex: 1;
    flex-direction: column;
    padding: var(--tt-space-4) var(--tt-space-5) 0;
    overflow: hidden;
}
.tie-page__review {
    position: relative;
    display: flex;
    flex: 1;
    align-items: end;
    justify-content: center;
    min-height: 280px;
    overflow: hidden;
}
.tie-page__review::before {
    position: absolute;
    bottom: 42px;
    width: 260px;
    height: 160px;
    background: radial-gradient(ellipse, var(--tt-accent-subtle), transparent 68%);
    border-radius: var(--tt-radius-full);
    content: '';
    animation: tie-focus 2.8s ease-in-out infinite;
}
.tie-page__review-copy {
    position: absolute;
    top: var(--tt-space-5);
    z-index: 1;
    padding: var(--tt-space-2) var(--tt-space-3);
    color: var(--tt-accent-700);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}
.tie-page__review img {
    position: relative;
    width: min(75vw, 280px);
    object-fit: contain;
}
.tie-page__footer {
    padding: var(--tt-space-3) var(--tt-space-5) var(--tt-space-6);
}
@keyframes tie-focus {
    50% {
        transform: scale(1.08);
        opacity: 0.62;
    }
}
@media (prefers-reduced-motion: reduce) {
    .tie-page__review::before {
        animation-duration: 0.01ms;
    }
}
</style>
