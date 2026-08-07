<!--
  기간결산 순위 리스트 — 명예 법정(S4)의 누적 혐의액 순위.
  순위 · 아바타 · 닉네임 · 누적 소비액/비율 · 프로그레스 바를 보여준다.
  위험 멤버(>=80%)는 배경이 빨갛게 하이라이트된다.
-->
<script setup>
defineProps({
    rankings: { type: Array, required: true },
    limitAmount: { type: Number, required: true },
});

function barColor(percent) {
    if (percent >= 80) return '#E0664B';
    return '#2E9E6B';
}

function textColor(percent) {
    if (percent >= 80) return '#C24B31';
    return '#2E9E6B';
}

function trackBg(percent) {
    if (percent >= 80) return '#F3DFDA';
    return 'var(--tt-bg-fill)';
}
</script>

<template>
    <div class="period-rank">
        <div class="period-rank__header">
            <span class="period-rank__title">전체 피고인 현황</span>
            <span class="period-rank__sub">
                적게 쓴 순 · 한도 {{ limitAmount.toLocaleString() }}원
            </span>
        </div>

        <div class="period-rank__list">
            <div
                v-for="(r, idx) in rankings"
                :key="r.userId"
                class="period-rank__row"
                :class="{
                    'period-rank__row--me': r.isMe,
                    'period-rank__row--danger': r.usagePercent >= 80,
                    'period-rank__row--bordered': idx > 0,
                }"
            >
                <span class="period-rank__num">{{ r.rank }}</span>

                <div
                    class="period-rank__avatar"
                    :style="{ background: r.avatarColor }"
                >
                    {{ r.initial }}
                </div>

                <div class="period-rank__info">
                    <div class="period-rank__name-row">
                        <span class="period-rank__name">{{ r.nickname }}</span>
                        <span v-if="r.isMe" class="period-rank__me-badge">나</span>
                        <span
                            v-if="r.usagePercent >= 80 && !r.isMe"
                            class="period-rank__danger-badge"
                        >위험</span>
                        <span class="period-rank__amount">
                            {{ r.currentAmount.toLocaleString() }}원 ·
                            <b :style="{ color: textColor(r.usagePercent) }">{{ r.usagePercent }}%</b>
                        </span>
                    </div>
                    <div
                        class="period-rank__bar-track"
                        :style="{ background: trackBg(r.usagePercent) }"
                    >
                        <div
                            class="period-rank__bar-fill"
                            :style="{
                                width: Math.min(r.usagePercent, 100) + '%',
                                background: barColor(r.usagePercent),
                            }"
                        ></div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.period-rank__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.period-rank__title {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.period-rank__sub {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

/* ── 리스트 ── */
.period-rank__list {
    margin-top: 10px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 16px;
    box-shadow: var(--tt-elevation-2);
    overflow: hidden;
}

.period-rank__row {
    padding: 9px 13px;
    display: flex;
    align-items: center;
    gap: 10px;
}

.period-rank__row--me {
    background: var(--tt-bg-subtle);
}

.period-rank__row--danger {
    background: #FDF4F1;
}

.period-rank__row--bordered {
    border-top: 1px solid var(--tt-bg-fill);
}

.period-rank__num {
    width: 14px;
    text-align: center;
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
}

.period-rank__avatar {
    width: 28px;
    height: 28px;
    border-radius: 50%;
    color: var(--tt-white);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.period-rank__info {
    flex: 1;
    min-width: 0;
}

.period-rank__name-row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: 5px;
}

.period-rank__name {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.period-rank__me-badge {
    background: var(--tt-gold-soft);
    color: var(--tt-gold-deep);
    font-size: 9.5px;
    font-weight: var(--tt-fw-black);
    padding: 2px 6px;
    border-radius: var(--tt-radius-full);
}

.period-rank__danger-badge {
    background: var(--tt-red-soft);
    color: var(--tt-red-deep);
    font-size: 9.5px;
    font-weight: var(--tt-fw-black);
    padding: 2px 6px;
    border-radius: var(--tt-radius-full);
}

.period-rank__amount {
    margin-left: auto;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
    white-space: nowrap;
}

.period-rank__bar-track {
    margin-top: 6px;
    height: 5px;
    border-radius: var(--tt-radius-full);
    overflow: hidden;
}

.period-rank__bar-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    transition: width 0.6s ease;
}
</style>
