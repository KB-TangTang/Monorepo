<script setup>
import BaseCard from '@/components/common/BaseCard.vue';
import verdictImage from '@/assets/images/emotions/49_verdict.png';
import guiltyStampImage from '@/assets/images/judgment/guilty_stamp.png';
import innocentStampImage from '@/assets/images/judgment/innocent_stamp.png';

const props = defineProps({
    verdict: { type: Object, required: true },
});

const stampImage = props.verdict.outcome === 'GUILTY' ? guiltyStampImage : innocentStampImage;
</script>

<template>
    <BaseCard class="verdict-card" :class="`verdict-card--${verdict.outcome}`" padding="none">
        <div class="verdict-card__masthead">FINAL VERDICT · {{ verdict.caseNumber }}</div>
        <div class="verdict-card__stamp-ring">
            <img
                :src="stampImage"
                :alt="`${verdict.label} 판결 인장`"
                class="verdict-card__stamp"
            />
        </div>
        <img :src="verdictImage" alt="판결을 내린 탕탕이" class="verdict-card__mascot" />
        <h2 class="verdict-card__title">{{ verdict.resultTitle }}</h2>
        <p class="verdict-card__description">{{ verdict.resultDescription }}</p>
    </BaseCard>
</template>

<style scoped>
.verdict-card {
    position: relative;
    min-height: 455px;
    padding: var(--tt-space-5) var(--tt-space-5) var(--tt-space-7);
    overflow: hidden;
    text-align: center;
}

.verdict-card::before {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 104px;
    background: var(--tt-surface-strong);
    border-radius: 0 0 48% 48%;
    content: '';
}

.verdict-card__masthead {
    position: relative;
    color: var(--tt-accent);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    padding-top: var(--tt-space-2);
    letter-spacing: 0.15em;
}

.verdict-card__stamp-ring {
    position: relative;
    display: block;
    width: 120px;
    height: 120px;
    margin: var(--tt-space-4) auto var(--tt-space-2);
    padding: var(--tt-space-1);
    background: var(--tt-bg);
    border: var(--tt-space-1) solid var(--tt-bg);
    border-radius: var(--tt-radius-full);
    box-shadow: var(--tt-elevation-2);
}

.verdict-card--GUILTY .verdict-card__stamp-ring {
    background: var(--tt-danger-subtle);
}
.verdict-card--INNOCENT .verdict-card__stamp-ring {
    background: var(--tt-success-subtle);
}

.verdict-card__stamp {
    position: absolute;
    top: 50%;
    left: 50%;
    width: 116%;
    height: 116%;
    object-fit: contain;
    transform: translate(-50%, -50%);
}

.verdict-card__mascot {
    display: block;
    width: 134px;
    height: 134px;
    margin: 0 auto;
    object-fit: contain;
}

.verdict-card__title {
    white-space: pre-line;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    letter-spacing: -0.01em;
}

.verdict-card__description {
    margin-top: var(--tt-space-3);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-body);
}
</style>
