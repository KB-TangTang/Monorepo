<!--
  용도: 순자산 추이 화면의 월별 스택 막대 차트(순자산 + 부채). 막대를 클릭하면 그 달이 선택된다.
  언제 쓰는지: NetWorthTrendView 중단 한 곳. SVG 대신 CSS height 로 그린다 — viewBox 계산 실수로
  테두리가 잘리는 문제(AssetCompositionCard 도넛에서 겪음)를 피하기 위함이다.
-->
<script setup>
import { computed } from 'vue';
import { getBarHeights } from '@/utils/asset';

const props = defineProps({
    months: { type: Array, required: true },
    netWorth: { type: Array, required: true },
    totalDebt: { type: Array, required: true },
    selectedMonthIndex: { type: Number, required: true },
});

defineEmits(['update:selectedMonthIndex']);

const MAX_HEIGHT = 160;

const bars = computed(() => getBarHeights(props.netWorth, props.totalDebt, MAX_HEIGHT));
</script>

<template>
    <div class="trend-chart">
        <div class="trend-chart__legend">
            <span class="trend-chart__legend-item">
                <i class="trend-chart__dot trend-chart__dot--net"></i>순자산
            </span>
            <span class="trend-chart__legend-item">
                <i class="trend-chart__dot trend-chart__dot--debt"></i>부채
            </span>
        </div>

        <div class="trend-chart__bars">
            <button
                v-for="(month, i) in months"
                :key="month"
                type="button"
                class="trend-chart__bar"
                :class="{ 'trend-chart__bar--selected': i === selectedMonthIndex }"
                @click="$emit('update:selectedMonthIndex', i)"
            >
                <span class="trend-chart__stack-wrap" :style="{ height: `${MAX_HEIGHT}px` }">
                    <span class="trend-chart__stack">
                        <span
                            class="trend-chart__segment trend-chart__segment--debt"
                            :style="{ height: `${bars[i].debtHeight}px` }"
                        ></span>
                        <span
                            class="trend-chart__segment trend-chart__segment--net"
                            :style="{ height: `${bars[i].netWorthHeight}px` }"
                        ></span>
                    </span>
                </span>
                <span class="trend-chart__month">{{ month }}</span>
            </button>
        </div>
    </div>
</template>

<style scoped>
.trend-chart__legend {
    display: flex;
    gap: var(--tt-space-4);
    margin-bottom: var(--tt-space-4);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.trend-chart__legend-item {
    display: inline-flex;
    align-items: center;
    gap: var(--tt-space-1);
}

.trend-chart__dot {
    display: inline-block;
    width: 8px;
    height: 8px;
    border-radius: var(--tt-radius-full);
}

.trend-chart__dot--net {
    background: var(--tt-gray-900);
}

.trend-chart__dot--debt {
    background: var(--tt-guilty-300);
}

.trend-chart__bars {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: var(--tt-space-2);
}

.trend-chart__bar {
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    gap: var(--tt-space-2);
    padding: 0;
    background: transparent;
    border: 0;
    cursor: pointer;
}

.trend-chart__stack-wrap {
    display: flex;
    align-items: flex-end;
    width: 100%;
}

.trend-chart__stack {
    display: flex;
    flex-direction: column;
    width: 28px;
    margin: 0 auto;
    overflow: hidden;
    border-radius: var(--tt-radius-xs) var(--tt-radius-xs) 0 0;
}

.trend-chart__segment--net {
    background: var(--tt-gray-900);
}

.trend-chart__segment--debt {
    background: var(--tt-guilty-300);
}

.trend-chart__bar--selected .trend-chart__segment--debt {
    background: var(--tt-danger);
}

.trend-chart__month {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.trend-chart__bar--selected .trend-chart__month {
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
</style>
