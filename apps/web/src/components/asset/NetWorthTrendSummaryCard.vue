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

const current = computed(() => props.trend[props.trend.length - 1]);
const changeAmount = computed(() => current.value - props.trend[0]);
const changePercent = computed(() => getSignedPercent(props.trend[0], current.value));
const changeMonths = computed(() => props.trend.length);
</script>

<template>
    <section class="trend-summary">
        <p class="trend-summary__label">현재 순자산 평결액</p>
        <p class="trend-summary__amount">{{ formatWon(current) }}</p>
        <p class="trend-summary__change" :class="{ 'trend-summary__change--down': changeAmount < 0 }">
            {{ changeMonths }}개월간 {{ formatSignedWon(changeAmount) }} ({{ changePercent }})
        </p>
    </section>
</template>

<style scoped>
.trend-summary {
    padding: var(--tt-space-6);
    background: var(--tt-gray-900);
    border-radius: var(--tt-radius-xl);
}

.trend-summary__label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-gray-400);
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
    color: var(--tt-innocent-300);
}

.trend-summary__change--down {
    color: var(--tt-guilty-300);
}
</style>
