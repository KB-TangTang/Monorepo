<script setup>
import { computed } from 'vue';

const props = defineProps({
    ranking: { type: Object, default: null },
    challengeDays: { type: Number, default: 0 },
    bestStreakDays: { type: Number, default: 0 },
    bestWeekday: { type: String, default: '' },
});

const hasRanking = computed(
    () =>
        Number.isFinite(props.ranking?.rank) &&
        Number.isFinite(props.ranking?.topPercent) &&
        Number.isFinite(props.ranking?.totalScore),
);

function formatNumber(value) {
    return value.toLocaleString('ko-KR');
}
</script>

<template>
    <section class="ranking-summary-card" aria-labelledby="ranking-summary-title">
        <header class="ranking-summary-card__header">
            <span id="ranking-summary-title">이번 달 명예 전적</span>
            <small>MONTHLY SCOREBOARD</small>
        </header>

        <div v-if="hasRanking" class="ranking-summary-card__overview">
            <div class="ranking-summary-card__emblem" aria-label="내 순위">
                <span>RANK</span>
                <strong>{{ formatNumber(ranking.rank) }}</strong>
                <small>위</small>
            </div>
            <div class="ranking-summary-card__percentile">
                <span>전체 참가자 중</span>
                <strong>상위 {{ formatNumber(ranking.topPercent) }}%</strong>
                <small v-if="bestWeekday">가장 잘 지킨 요일 · {{ bestWeekday }}</small>
            </div>
        </div>
        <p v-else class="ranking-summary-card__empty">이 달의 랭킹을 집계하고 있어요.</p>

        <dl
            class="ranking-summary-card__stats"
            :class="{ 'ranking-summary-card__stats--without-ranking': !hasRanking }"
        >
            <div v-if="hasRanking">
                <dt>누적 점수</dt>
                <dd>{{ formatNumber(ranking.totalScore) }}<small>점</small></dd>
            </div>
            <div>
                <dt>도전 일수</dt>
                <dd>{{ formatNumber(challengeDays) }}<small>일</small></dd>
            </div>
            <div>
                <dt>최고 연속 성공</dt>
                <dd>{{ formatNumber(bestStreakDays) }}<small>일</small></dd>
            </div>
        </dl>
    </section>
</template>

<style scoped>
.ranking-summary-card {
    position: relative;
    overflow: hidden;
    padding: var(--tt-space-4);
    color: var(--tt-text);
    background:
        linear-gradient(color-mix(in srgb, var(--tt-info) 6%, transparent) 1px, transparent 1px),
        linear-gradient(
            90deg,
            color-mix(in srgb, var(--tt-info) 6%, transparent) 1px,
            transparent 1px
        ),
        var(--tt-bg);
    background-size: var(--tt-space-4) var(--tt-space-4);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
}

.ranking-summary-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    padding-bottom: var(--tt-space-2);
    border-bottom: 1px solid var(--tt-border);
}

.ranking-summary-card__header > span,
.ranking-summary-card__header > small {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.08em;
    color: var(--tt-text);
}

.ranking-summary-card__header > small {
    flex-shrink: 0;
    letter-spacing: 0.12em;
    opacity: 0.72;
}

.ranking-summary-card__overview {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-3);
}

.ranking-summary-card__emblem {
    display: grid;
    flex: 0 0 72px;
    grid-template-columns: 1fr auto;
    grid-template-rows: auto 1fr;
    align-items: end;
    min-height: 72px;
    padding: var(--tt-space-2);
    color: var(--tt-text);
    background: linear-gradient(150deg, var(--tt-accent) 0 37%, var(--tt-accent-subtle) 37% 100%);
    border: 1px solid var(--tt-accent-subtle-border);
    border-radius: var(--tt-radius-xs);
    box-shadow: inset 0 0 0 3px color-mix(in srgb, var(--tt-bg) 68%, transparent);
}

.ranking-summary-card__emblem > span {
    grid-column: 1 / -1;
    align-self: start;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.1em;
}

.ranking-summary-card__emblem > strong {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    letter-spacing: -0.05em;
}

.ranking-summary-card__emblem > small {
    margin-bottom: 1px;
    font-size: var(--tt-fs-body);
}

.ranking-summary-card__percentile {
    display: flex;
    flex: 1;
    flex-direction: column;
    min-width: 0;
    gap: var(--tt-space-1);
}

.ranking-summary-card__percentile > span,
.ranking-summary-card__percentile > small,
.ranking-summary-card__stats dt,
.ranking-summary-card__stats span {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.ranking-summary-card__percentile > strong {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.ranking-summary-card__stats {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    margin-top: var(--tt-space-3);
    border-top: 1px dashed var(--tt-border-divider);
    border-bottom: 1px dashed var(--tt-border-divider);
}

.ranking-summary-card__stats--without-ranking {
    grid-template-columns: repeat(2, minmax(0, 1fr));
}

.ranking-summary-card__stats > div {
    min-width: 0;
    padding: var(--tt-space-2) var(--tt-space-1);
}

.ranking-summary-card__stats > div + div {
    border-left: 1px solid var(--tt-border-light);
}

.ranking-summary-card__stats dt,
.ranking-summary-card__stats dd,
.ranking-summary-card__stats span {
    display: block;
}

.ranking-summary-card__stats dd {
    margin-top: 2px;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.ranking-summary-card__stats dd small {
    margin-left: 2px;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-caption);
}

.ranking-summary-card__stats span {
    overflow: hidden;
    margin-top: 2px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.ranking-summary-card__empty {
    margin-top: var(--tt-space-4);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

@media (max-width: 360px) {
    .ranking-summary-card__header > small {
        display: none;
    }

    .ranking-summary-card__emblem {
        flex-basis: 68px;
        min-height: 68px;
    }

    .ranking-summary-card__stats dt {
        font-size: var(--tt-fs-overline);
    }
}
</style>
