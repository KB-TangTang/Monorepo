<!--
  목숨 표시 바 — 판결봉(gavel) 아이콘으로 남은/차감된 목숨을 보여준다.
  시작 전에는 전체 목숨, 진행 중에는 남은 + 차감된 목숨을 표시한다.
-->
<script setup>
import { computed } from 'vue';

import gavelAlive from '@/assets/images/challenge_live/gavel-alive.png';
import gavelDepleted from '@/assets/images/challenge_live/gavel-depleted.png';

const props = defineProps({
    livesCount: { type: Number, required: true },
    maxLives: { type: Number, required: true },
    /** 'upcoming' | 'active' */
    mode: { type: String, default: 'active' },
    /** 아이콘 크기 (px) */
    size: { type: Number, default: 19 },
});

const isUpcoming = computed(() => props.mode === 'upcoming');

const label = computed(() => {
    if (isUpcoming.value) return `${props.maxLives}개 · 챌린지 ${props.maxLives}일`;
    return `${props.livesCount} / ${props.maxLives}개`;
});

const aliveCount = computed(() => isUpcoming.value ? props.maxLives : props.livesCount);
const depletedCount = computed(() => isUpcoming.value ? 0 : props.maxLives - props.livesCount);
</script>

<template>
    <div class="lives-bar">
        <div class="lives-bar__header">
            <span class="lives-bar__title">
                {{ isUpcoming ? '시작 시 받는 목숨' : '남은 목숨' }}
            </span>
            <span class="lives-bar__label">{{ label }}</span>
        </div>
        <div class="lives-bar__icons">
            <img
                v-for="i in aliveCount"
                :key="'alive-' + i"
                :src="gavelAlive"
                alt="남은 목숨"
                class="lives-bar__icon"
                :style="{ width: size + 'px', height: size + 'px' }"
            >
            <img
                v-for="i in depletedCount"
                :key="'depleted-' + i"
                :src="gavelDepleted"
                alt="차감된 목숨"
                class="lives-bar__icon"
                :style="{ width: size + 'px', height: size + 'px' }"
            >
        </div>
        <p v-if="isUpcoming" class="lives-bar__hint">
            챌린지 일 수만큼 부여 · 유죄 1건마다 1개 차감
        </p>
    </div>
</template>

<style scoped>
.lives-bar {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 11px 15px;
}

.lives-bar__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.lives-bar__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.lives-bar__label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-gold-deep);
}

.lives-bar__icons {
    display: flex;
    gap: 5px;
    margin-top: 9px;
}

.lives-bar__icon {
    object-fit: contain;
}

.lives-bar__hint {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    margin-top: 8px;
    line-height: 1.5;
}
</style>
