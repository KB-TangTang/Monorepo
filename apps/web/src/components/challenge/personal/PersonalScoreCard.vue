<script setup>
defineProps({
    score: { type: Number, required: true },
    percentile: { type: Number, required: true },
    nextTierGap: { type: Number, required: true },
    tierProgress: { type: Number, default: 0 },
});

defineEmits(['report-click']);
</script>

<template>
    <div class="score-card">
        <div class="score-card__top">
            <div class="score-card__numbers">
                <div class="score-card__overline">이번 달 누적</div>
                <div class="score-card__score-row">
                    <span class="score-card__score">{{ score }}</span>
                    <span class="score-card__unit">점</span>
                </div>
            </div>
            <span class="score-card__percentile">월간 상위 {{ percentile }}%</span>
        </div>

        <div class="score-card__gauge">
            <div class="score-card__gauge-track">
                <div
                    class="score-card__gauge-fill"
                    :style="{ width: tierProgress + '%' }"
                ></div>
                <div class="score-card__gauge-marker"></div>
            </div>
            <div class="score-card__gauge-labels">
                <span class="score-card__gap-text">
                    상위 10%까지 <b>{{ nextTierGap }}점</b>
                </span>
                <button
                    type="button"
                    class="score-card__report-link"
                    @click="$emit('report-click')"
                >
                    성적표 보기 ›
                </button>
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

.score-card__top {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
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

.score-card__gauge {
    margin-top: var(--tt-space-3);
}

.score-card__gauge-track {
    position: relative;
    height: 8px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
}

.score-card__gauge-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    background: var(--tt-accent);
    transition: width 0.4s ease;
}

.score-card__gauge-marker {
    position: absolute;
    right: 0;
    top: -5px;
    width: 2px;
    height: 18px;
    border-radius: 1px;
    background: var(--tt-border-divider);
}

.score-card__gauge-labels {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: var(--tt-space-2);
}

.score-card__gap-text {
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
}

.score-card__gap-text b {
    color: var(--tt-accent-deep);
}

.score-card__report-link {
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
</style>
