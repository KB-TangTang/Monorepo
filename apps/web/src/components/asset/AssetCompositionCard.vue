<!--
  용도: 자산 홈 화면의 자산 구성 도넛 차트 + 범례. composition 배열의 amount 비율을 시각화한다.
  언제 쓰는지: AssetHomeView 중단 한 곳. 대출 등 부채는 여기 포함하지 않는다(도넛 = 총 자산 구성비).
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import {
    formatWon,
    formatCompactWon,
    getCompositionTotal,
    getCompositionRatios,
    toneColor,
} from '@/utils/asset';

const props = defineProps({
    composition: { type: Array, required: true },
});

const RADIUS = 54;
const CIRCUMFERENCE = 2 * Math.PI * RADIUS;

const total = computed(() => getCompositionTotal(props.composition));
const segments = computed(() => getCompositionRatios(props.composition));
</script>

<template>
    <BaseCard class="asset-composition">
        <template #header>자산 구성</template>

        <div class="asset-composition__body">
            <div class="asset-composition__chart">
                <svg viewBox="0 0 124 124" class="asset-composition__donut" aria-hidden="true">
                    <circle
                        v-for="segment in segments"
                        :key="segment.code"
                        cx="62"
                        cy="62"
                        :r="RADIUS"
                        fill="none"
                        :stroke="toneColor(segment.tone)"
                        stroke-width="16"
                        :stroke-dasharray="`${segment.ratio * CIRCUMFERENCE} ${CIRCUMFERENCE}`"
                        :stroke-dashoffset="-segment.offset * CIRCUMFERENCE"
                        transform="rotate(-90 62 62)"
                    />
                </svg>
                <div class="asset-composition__center">
                    <p class="asset-composition__center-label">총 자산</p>
                    <p class="asset-composition__center-value">{{ formatCompactWon(total) }}</p>
                </div>
            </div>

            <ul class="asset-composition__legend">
                <li
                    v-for="item in composition"
                    :key="item.code"
                    class="asset-composition__legend-item"
                >
                    <span
                        class="asset-composition__dot"
                        :style="{ background: toneColor(item.tone) }"
                        aria-hidden="true"
                    ></span>
                    <span class="asset-composition__legend-label">{{ item.label }}</span>
                    <span class="asset-composition__legend-amount">{{
                        formatWon(item.amount)
                    }}</span>
                </li>
            </ul>
        </div>
    </BaseCard>
</template>

<style scoped>
.asset-composition__body {
    display: flex;
    align-items: center;
    gap: var(--tt-space-5);
}

.asset-composition__chart {
    position: relative;
    flex-shrink: 0;
    width: 120px;
    height: 120px;
}

.asset-composition__donut {
    width: 100%;
    height: 100%;
}

.asset-composition__center {
    position: absolute;
    inset: 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
}

.asset-composition__center-label {
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.asset-composition__center-value {
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.asset-composition__legend {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.asset-composition__legend-item {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
}

.asset-composition__dot {
    width: 8px;
    height: 8px;
    border-radius: var(--tt-radius-full);
}

.asset-composition__legend-label {
    flex: 1;
    color: var(--tt-text-muted);
}

.asset-composition__legend-amount {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}
</style>
