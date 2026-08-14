<!--
  용도: 순자산 추이 화면 상단의 다크 요약 카드. 현재 순자산 + 6개월간 변화(금액·퍼센트)를 보여준다.
  언제 쓰는지: NetWorthTrendView 상단 한 곳. 막대 선택 상태와 무관하게 항상 고정 표시한다.
-->
<script setup>
import { computed } from 'vue';
import { formatWon, formatSignedWon, getSignedPercent } from '@/utils/asset';

const props = defineProps({
    trend: { type: Array, required: true },
});

// 스냅샷이 없는 이전 달은 null 로 들어온다 — "N개월간 변화"는 실제 데이터가 있는
// 구간만으로 계산한다. 전부 null 이 없는 값(0)으로 계산하면 예: 6개월 전이 미집계인데
// 그 값을 0원으로 취급해 변화량이 실제보다 과장되게 표시된다.
const availableTrend = computed(() =>
    props.trend.filter((value) => value !== null && value !== undefined),
);
const current = computed(() => availableTrend.value[availableTrend.value.length - 1] ?? 0);
const earliestAvailable = computed(() => availableTrend.value[0] ?? current.value);
const changeAmount = computed(() => current.value - earliestAvailable.value);
const changePercent = computed(() => getSignedPercent(earliestAvailable.value, current.value));
const changeMonths = computed(() => availableTrend.value.length);
</script>

<template>
    <section class="trend-summary">
        <p class="trend-summary__label">현재 순자산 평결액</p>
        <p class="trend-summary__amount">{{ formatWon(current) }}</p>
        <p
            class="trend-summary__change"
            :class="{ 'trend-summary__change--down': changeAmount < 0 }"
        >
            {{ changeMonths }}개월간 {{ formatSignedWon(changeAmount) }} ({{ changePercent }})
        </p>
    </section>
</template>

<style scoped>
.trend-summary {
    padding: var(--tt-space-6);
    /* Ink 스탯 카드 배경 — 의미 토큰 이름 그대로 "역전(다크) 배경"용이다. */
    background: var(--tt-surface-inverse);
    border-radius: var(--tt-radius-xl);
}

.trend-summary__label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    /* v1 의 --tt-gray-400 은 v2 에 없다 — 어두운 배경 위 텍스트는 --tt-text-inverse 뿐이라
       불투명도로 톤을 낮춘다. */
    color: var(--tt-text-inverse);
    opacity: 0.7;
}

.trend-summary__amount {
    margin-top: var(--tt-space-2);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text-inverse);
}

.trend-summary__change {
    margin-top: var(--tt-space-3);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-success);
}

.trend-summary__change--down {
    color: var(--tt-danger);
}
</style>
