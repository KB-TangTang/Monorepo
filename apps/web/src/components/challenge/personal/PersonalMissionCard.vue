<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { calculatePersonalMissionProgress, formatWon } from '@/services/personalMissionFlow';

const props = defineProps({
    mission: {
        type: Object,
        required: true,
    },
    difficulty: {
        type: Object,
        default: null,
    },
    personalized: {
        type: Boolean,
        default: false,
    },
});

const targetAmount = computed(() => {
    return props.difficulty?.targetAmount ?? props.mission.targetAmount;
});

const progress = computed(() => {
    return calculatePersonalMissionProgress(props.mission.currentAmount, targetAmount.value);
});

const remainingAmount = computed(() => {
    return Math.max(targetAmount.value - props.mission.currentAmount, 0);
});
</script>

<template>
    <BaseCard padding="lg">
        <article class="mission-card">
            <header>
                <span>
                    {{ personalized ? difficulty.label : '공통 미션' }}
                    · {{ mission.missionTypeLabel }}
                </span>

                <small>2026-미션-0729</small>
            </header>

            <h2>{{ mission.title }}</h2>
            <p>{{ mission.description }}</p>

            <template v-if="personalized">
                <div class="mission-card__amount">
                    <strong>{{ formatWon(mission.currentAmount) }}</strong>
                    <span>/ 목표 {{ formatWon(targetAmount) }}</span>
                    <em>여유 {{ formatWon(remainingAmount) }}</em>
                </div>

                <div
                    class="mission-card__progress"
                    role="progressbar"
                    :aria-valuenow="progress"
                    aria-valuemin="0"
                    aria-valuemax="100"
                >
                    <span :style="{ width: `${progress}%` }"></span>
                </div>

                <div class="mission-card__reward">
                    🏆 성공 시 +{{ difficulty.rewardPoints }}점 · 연속 보너스 +5점
                </div>
            </template>

            <div v-else class="mission-card__common-result">
                ✓ 현재까지 {{ mission.category }} 지출
                <strong>{{ formatWon(mission.currentAmount) }}</strong>
                · 성공 시 +{{ mission.rewardPoints }}점
            </div>
        </article>
    </BaseCard>
</template>

<style scoped>
.mission-card header {
    display: flex;
    justify-content: space-between;
    color: var(--tt-text-muted);
}

.mission-card header span {
    padding: var(--tt-space-1) var(--tt-space-2);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-full);
}

.mission-card header small {
    font-family: var(--tt-font-mono);
}

.mission-card h2 {
    margin: var(--tt-space-4) 0 var(--tt-space-1);
    font-size: var(--tt-fs-section);
}

.mission-card > p {
    margin: 0;
    color: var(--tt-text-muted);
}

.mission-card__amount {
    display: flex;
    align-items: baseline;
    margin-top: var(--tt-space-5);
}

.mission-card__amount span {
    margin-left: var(--tt-space-1);
    color: var(--tt-text-muted);
}

.mission-card__amount em {
    margin-left: auto;
    font-style: normal;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-success);
}

.mission-card__progress {
    height: 8px;
    margin-top: var(--tt-space-3);
    overflow: hidden;
    background: var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.mission-card__progress span {
    display: block;
    height: 100%;
    background: var(--tt-success);
    border-radius: inherit;
}

.mission-card__reward,
.mission-card__common-result {
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-3);
    font-weight: var(--tt-fw-bold);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-md);
}

.mission-card__common-result strong {
    color: var(--tt-success);
}
</style>
