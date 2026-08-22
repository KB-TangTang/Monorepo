<!--
  투표 결과 프로그레스 바 — 무죄/유죄 투표 비율 시각화.
  01/02(최종 판결) 화면에서 사용.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    /** 'INNOCENT' | 'GUILTY' */
    outcome: { type: String, required: true },
    innocentVotes: { type: Number, required: true },
    guiltyVotes: { type: Number, required: true },
});

const isInnocent = computed(() => props.outcome === 'INNOCENT');
const total = computed(() => props.innocentVotes + props.guiltyVotes);
const winnerVotes = computed(() =>
    isInnocent.value ? props.innocentVotes : props.guiltyVotes,
);
const percent = computed(() =>
    total.value > 0 ? (winnerVotes.value / total.value) * 100 : 0,
);
const scoreText = computed(() => `${props.innocentVotes} : ${props.guiltyVotes}`);
const statusText = computed(() => isInnocent.value ? '무죄 확정' : '유죄 확정');
</script>

<template>
    <div
        class="vote-bar"
        :class="isInnocent ? 'vote-bar--innocent' : 'vote-bar--guilty'"
    >
        <div class="vote-bar__label">최종 투표</div>
        <div class="vote-bar__row">
            <span class="vote-bar__score">{{ scoreText }}</span>
            <span class="vote-bar__status">{{ statusText }}</span>
        </div>
        <div class="vote-bar__track">
            <div
                class="vote-bar__fill"
                :style="{ width: percent + '%' }"
            ></div>
        </div>
    </div>
</template>

<style scoped>
.vote-bar {
    border-radius: var(--tt-radius-lg);
    padding: 14px var(--tt-card-padding);
}

.vote-bar--innocent {
    background: var(--tt-success-subtle);
    border: 1px solid var(--tt-success-subtle-border);
}

.vote-bar--guilty {
    background: var(--tt-danger-subtle);
    border: 1px solid var(--tt-danger-subtle-border);
}

.vote-bar__label {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
}

.vote-bar--innocent .vote-bar__label {
    color: var(--tt-success);
}

.vote-bar--guilty .vote-bar__label {
    color: var(--tt-danger-deep);
}

.vote-bar__row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-top: 5px;
}

.vote-bar__score {
    font-size: 26px;
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    color: var(--tt-text);
}

.vote-bar__status {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
}

.vote-bar--innocent .vote-bar__status {
    color: var(--tt-success-deep);
}

.vote-bar--guilty .vote-bar__status {
    color: var(--tt-danger-deep);
}

.vote-bar__track {
    margin-top: 11px;
    height: 8px;
    border-radius: var(--tt-radius-full);
    overflow: hidden;
}

.vote-bar--innocent .vote-bar__track {
    background: var(--tt-success-subtle-border);
}

.vote-bar--guilty .vote-bar__track {
    background: var(--tt-danger-subtle-border);
}

.vote-bar__fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    transition: width 0.4s ease;
}

.vote-bar--innocent .vote-bar__fill {
    background: var(--tt-success);
}

.vote-bar--guilty .vote-bar__fill {
    background: var(--tt-danger);
}
</style>
