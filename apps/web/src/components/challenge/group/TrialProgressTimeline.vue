<!--
  재판 진행 타임라인 — 4단계 (기소 접수 → 변론 제출 → 배심원 투표 → 최종 판결).
  상태에 따라 완료(체크) / 활성(pulse) / 대기(빈 원) 표시.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    /** 'INDICTED' | 'DEFENSE_SUBMITTED' | 'VOTING' | 'VERDICT_DONE' */
    status: { type: String, required: true },
    /** 사건번호 e.g. '2026-재판-0619' */
    caseNumber: { type: String, default: '' },
    /** 각 단계 시각. { indictedAt, defenseSubmittedAt, voteDeadline, verdictAt } */
    steps: { type: Object, required: true },
    /** 투표 참여 정보. { votedCount, totalVoters } */
    vote: { type: Object, default: () => ({ votedCount: 0, totalVoters: 0 }) },
});

const STATUS_ORDER = ['INDICTED', 'DEFENSE_SUBMITTED', 'VOTING', 'VERDICT_DONE'];

const currentIdx = computed(() => STATUS_ORDER.indexOf(props.status));

function stepState(idx) {
    if (idx < currentIdx.value) return 'done';
    if (idx === currentIdx.value) return 'active';
    return 'pending';
}

const stepItems = computed(() => [
    {
        label: '기소 접수',
        time: props.steps.indictedAt,
        state: stepState(0),
    },
    {
        label: '변론 제출',
        time: props.steps.defenseSubmittedAt ?? '--:--',
        state: stepState(1),
    },
    {
        label: currentIdx.value === 2 ? '배심원 투표 진행 중' : '배심원 투표',
        time: currentIdx.value <= 2 ? `~${props.steps.voteDeadline}` : props.steps.voteDeadline,
        sub: currentIdx.value === 2
            ? `익명 투표 · ${props.vote.votedCount}명이 참여했어요`
            : null,
        state: stepState(2),
    },
    {
        label: '최종 판결',
        time: props.steps.verdictAt ?? props.steps.voteDeadline,
        state: stepState(3),
    },
]);
</script>

<template>
    <div class="timeline">
        <div class="timeline__header">진행 단계 · {{ caseNumber }}</div>
        <div class="timeline__track">
            <div class="timeline__line"></div>
            <div
                v-for="(step, i) in stepItems"
                :key="i"
                class="timeline__step"
            >
                <!-- 완료 -->
                <div v-if="step.state === 'done'" class="timeline__dot timeline__dot--done">
                    <svg width="10" height="8" viewBox="0 0 10 8" fill="none">
                        <path d="M1 4l2.6 2.6L9 1" stroke="#fff" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                    </svg>
                </div>
                <!-- 활성 (pulse) -->
                <div v-else-if="step.state === 'active'" class="timeline__dot timeline__dot--active">
                    <span class="timeline__pulse"></span>
                    <span class="timeline__pulse timeline__pulse--delayed"></span>
                    <span class="timeline__dot-inner">
                        <span class="timeline__dot-core"></span>
                    </span>
                </div>
                <!-- 대기 -->
                <div v-else class="timeline__dot timeline__dot--pending"></div>

                <div class="timeline__content">
                    <div class="timeline__row">
                        <span
                            class="timeline__label"
                            :class="{ 'timeline__label--pending': step.state === 'pending' }"
                        >{{ step.label }}</span>
                        <span
                            class="timeline__time"
                            :class="{
                                'timeline__time--active': step.state === 'active',
                                'timeline__time--pending': step.state === 'pending',
                            }"
                        >{{ step.time }}</span>
                    </div>
                    <div v-if="step.sub" class="timeline__sub">{{ step.sub }}</div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.timeline {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    padding: var(--tt-card-padding);
    box-shadow: var(--tt-elevation-2);
}

.timeline__header {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
    letter-spacing: 0.04em;
}

.timeline__track {
    position: relative;
    margin-top: 13px;
}

.timeline__line {
    position: absolute;
    left: 9px;
    top: 10px;
    bottom: 10px;
    width: 2px;
    background: var(--tt-bg-fill);
}

.timeline__step {
    position: relative;
    display: flex;
    gap: var(--tt-space-3);
    align-items: flex-start;
}

.timeline__step + .timeline__step {
    margin-top: var(--tt-card-padding);
}

/* ── 점 공통 ── */
.timeline__dot {
    flex: none;
    width: 20px;
    height: 20px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
}

.timeline__dot--done {
    background: var(--tt-success);
}

.timeline__dot--pending {
    background: var(--tt-bg);
    border: 2px solid var(--tt-border-divider);
}

/* ── 활성 pulse ── */
.timeline__dot--active {
    position: relative;
}

.timeline__pulse {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: var(--tt-accent);
    animation: tl-ripple 2.2s ease-out infinite;
}

.timeline__pulse--delayed {
    animation-delay: 0.7s;
}

.timeline__dot-inner {
    position: absolute;
    inset: 0;
    border-radius: 50%;
    background: var(--tt-accent);
    display: flex;
    align-items: center;
    justify-content: center;
}

.timeline__dot-core {
    display: block;
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--tt-text-inverse);
}

@keyframes tl-ripple {
    0% { transform: scale(1); opacity: 0.55; }
    100% { transform: scale(1.8); opacity: 0; }
}

/* ── 텍스트 ── */
.timeline__content {
    flex: 1;
    min-width: 0;
}

.timeline__row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.timeline__label {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.timeline__label--pending {
    color: var(--tt-text-hint);
}

.timeline__time {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
    font-family: var(--tt-font-mono);
}

.timeline__time--active {
    color: var(--tt-accent-warn);
}

.timeline__time--pending {
    color: var(--tt-border-divider);
}

.timeline__sub {
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-medium);
    margin-top: 3px;
}
</style>
