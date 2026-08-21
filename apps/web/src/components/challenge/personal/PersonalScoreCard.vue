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

function dayAriaLabel(status) {
    if (status === 'success') return '인정';
    if (status === 'failed') return '기각';
    if (status === 'today') return '오늘 · 판정 전';
    return '판정 없음';
}
</script>

<template>
    <div class="score-card">
        <!-- ── 이번 주 판정 ────────────────────
             제목은 카드 밖 섹션 헤더에 있다. 여기서 또 쓰면 같은 말이 두 번 나온다. -->
        <div v-if="weekDays.length" class="score-card__weekly">
            <div class="score-card__week-grid">
                <div v-for="day in weekDays" :key="day.dow" class="score-card__day">
                    <small class="score-card__dow">{{ day.dow }}</small>
                    <div
                        class="score-card__circle"
                        :class="`score-card__circle--${day.status}`"
                        :aria-label="`${day.dow}요일 ${dayAriaLabel(day.status)}`"
                    >
                        <img
                            v-if="day.status === 'today' && prosecutorImage"
                            :src="prosecutorImage"
                            alt=""
                            class="score-card__tangi"
                        />
                        <!-- 인정: 채운 파랑 + 흰 체크 / 기각: 연한 빨강 + X.
                             기각을 성공과 같은 채도로 채우면 실패가 이어진 주가 붉은 벽이 된다. -->
                        <svg
                            v-else-if="day.status === 'success'"
                            width="16"
                            height="16"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="3"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            aria-hidden="true"
                        >
                            <path d="M5 12.5l4.5 4.5L19 7" />
                        </svg>
                        <svg
                            v-else-if="day.status === 'failed'"
                            width="14"
                            height="14"
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            stroke-width="3"
                            stroke-linecap="round"
                            aria-hidden="true"
                        >
                            <path d="M6 6l12 12M18 6L6 18" />
                        </svg>
                    </div>
                </div>
            </div>

            <p class="score-card__streak">🔥 연속 성공 {{ streakDays }}일째</p>
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

.score-card__streak {
    margin: 11px 0 0;
    text-align: center;
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

/* ── 주간 그리드 ───────────────────────── */
.score-card__week-grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
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
    border: 1.5px solid var(--tt-border);
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
}

/* 인정만 꽉 채운다. 한 주에서 눈에 먼저 들어와야 하는 건 성공한 날이다 */
.score-card__circle--success {
    background: var(--tt-primary);
    border-color: var(--tt-primary);
    color: var(--tt-text-inverse);
}

/* 기각은 같은 채도로 채우지 않는다 — 실패가 이어진 주가 붉은 벽이 된다 */
.score-card__circle--failed {
    background: var(--tt-danger-subtle);
    border-color: var(--tt-danger-subtle);
    color: var(--tt-danger);
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

@media (max-width: 359px) {
    .score-card__circle {
        width: 30px;
        height: 30px;
    }
}
</style>
