<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue';
import { calculateRankingProgress } from '@/services/missionMonthlyScore';

const props = defineProps({
    /* 이번 주 판정 */
    weekDays: { type: Array, default: () => [] },
    streakDays: { type: Number, default: 0 },
    prosecutorImage: { type: String, default: '' },

    /* 이번 달 누적 */
    score: { type: Number, required: true },
    topPercent: { type: Number, default: null },
});

const rankingProgress = computed(() => calculateRankingProgress(props.topPercent));
const displayedRankingProgress = ref(0);
let animationFrameId = null;

function animateRankingProgress(progress) {
    if (animationFrameId !== null) cancelAnimationFrame(animationFrameId);
    animationFrameId = requestAnimationFrame(() => {
        displayedRankingProgress.value = progress;
        animationFrameId = null;
    });
}

onMounted(() => animateRankingProgress(rankingProgress.value));
watch(rankingProgress, animateRankingProgress);
onBeforeUnmount(() => {
    if (animationFrameId !== null) cancelAnimationFrame(animationFrameId);
});

defineEmits(['report-click']);

function dayLabel(status) {
    if (status === 'success') return '인정';
    if (status === 'failed') return '기각';
    return '';
}
</script>

<template>
    <div class="score-card">
        <!-- ── 이번 주 판정 ──────────────────── -->
        <div v-if="weekDays.length" class="score-card__weekly">
            <div class="score-card__weekly-header">
                <span class="score-card__weekly-title">이번 주 판정</span>
                <span class="score-card__streak-badge">🔥 연속 {{ streakDays }}일</span>
            </div>

            <div class="score-card__week-grid">
                <div v-for="day in weekDays" :key="day.dow" class="score-card__day">
                    <small class="score-card__dow">{{ day.dow }}</small>
                    <div class="score-card__circle" :class="`score-card__circle--${day.status}`">
                        <template v-if="day.status === 'today'">
                            <img
                                v-if="prosecutorImage"
                                :src="prosecutorImage"
                                alt=""
                                class="score-card__tangi"
                            />
                        </template>
                        <template v-else>
                            {{ dayLabel(day.status) }}
                        </template>
                    </div>
                </div>
            </div>
        </div>

        <div v-if="weekDays.length" class="score-card__divider" />

        <!-- ── 이번 달 누적 ──────────────────── -->
        <div class="score-card__top">
            <div class="score-card__numbers">
                <div class="score-card__overline">이번 달 누적</div>
                <div class="score-card__score-row">
                    <span class="score-card__score">{{ score }}</span>
                    <span class="score-card__unit">점</span>
                </div>
            </div>
            <span class="score-card__percentile">
                {{ topPercent === null ? '순위 집계 전' : `월간 상위 ${topPercent}%` }}
            </span>
        </div>

        <div class="score-card__ranking-progress">
            <div
                class="score-card__ranking-track"
                role="progressbar"
                aria-label="월간 순위"
                aria-valuemin="0"
                aria-valuemax="100"
                :aria-valuenow="displayedRankingProgress"
            >
                <div
                    class="score-card__ranking-fill"
                    :style="{ width: `${displayedRankingProgress}%` }"
                ></div>
            </div>

            <button type="button" class="score-card__report-link" @click="$emit('report-click')">
                성적표 보기 ›
            </button>
        </div>
    </div>
</template>

<style scoped>
.score-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    padding: 14px var(--tt-space-4);
    box-shadow: var(--tt-elevation-2);
}

/* ── 이번 주 판정 ──────────────────────── */
.score-card__weekly {
    padding-bottom: 14px;
}

.score-card__weekly-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.score-card__weekly-title {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
    letter-spacing: 0.08em;
}

.score-card__streak-badge {
    padding: 4px 10px;
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-deep);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-full);
}

/* ── 주간 그리드 ───────────────────────── */
.score-card__week-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
    margin-top: 12px;
}

.score-card__day {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
}

.score-card__dow {
    font-size: 11px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
}

.score-card__circle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    border: 1.5px solid var(--tt-border);
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
}

.score-card__circle--success {
    background: var(--tt-success-subtle);
    border-color: var(--tt-success);
    color: var(--tt-success-deep);
}

.score-card__circle--failed {
    background: var(--tt-danger-subtle);
    border-color: var(--tt-danger);
    color: var(--tt-danger-deep);
}

.score-card__circle--today {
    background: var(--tt-surface-inverse);
    border-color: var(--tt-surface-inverse);
    color: var(--tt-accent);
}

.score-card__circle--pending {
    background: var(--tt-bg-fill);
    border-color: var(--tt-border);
    color: var(--tt-text-hint);
}

.score-card__tangi {
    width: 26px;
    height: 26px;
    object-fit: contain;
}

/* ── 구분선 ────────────────────────────── */
.score-card__divider {
    border-top: 1px dashed var(--tt-border-strong);
    margin: 0 4px;
}

/* ── 이번 달 누적 (기존) ───────────────── */
.score-card__top {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    padding-top: 14px;
}

.score-card__overline {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
    letter-spacing: 0.08em;
}

.score-card__score-row {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-top: 3px;
}

.score-card__score {
    font-size: var(--tt-fs-stat);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    letter-spacing: -0.02em;
    font-family: var(--tt-font-mono);
}

.score-card__unit {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-deep);
}

.score-card__percentile {
    background: var(--tt-accent-subtle);
    color: var(--tt-accent-deep);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    padding: 5px 11px;
    border-radius: var(--tt-radius-full);
    white-space: nowrap;
}

.score-card__ranking-progress {
    margin-top: var(--tt-space-4);
}

.score-card__ranking-track {
    position: relative;
    height: 8px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
}

.score-card__ranking-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    background: var(--tt-accent);
    transition: width 0.7s cubic-bezier(0.22, 1, 0.36, 1);
}

@media (prefers-reduced-motion: reduce) {
    .score-card__ranking-fill {
        transition: none;
    }
}

.score-card__report-link {
    display: block;
    margin-top: var(--tt-space-3);
    margin-left: auto;
    background: none;
    border: none;
    padding: 0;
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-info);
    white-space: nowrap;
    cursor: pointer;
    font-family: var(--tt-font-sans);
}

@media (max-width: 359px) {
    .score-card__circle {
        width: 30px;
        height: 30px;
        font-size: 9px;
    }
}
</style>
