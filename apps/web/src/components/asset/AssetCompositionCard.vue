<!--
  용도: 자산 홈 화면의 자산 구성 도넛 차트 + 범례. composition 배열의 amount 비율을 시각화한다.
  언제 쓰는지: AssetHomeView 중단 한 곳. 도넛(파이 조각)은 자산 항목(양수)만으로 100%를 채운다.
  순자산 금액은 위쪽 AssetNetWorthCard 에서 이미 보여주므로 도넛 안에는 "총자산" 라벨만 두고 금액은 넣지 않는다.
  월간보고서 카테고리별 지출 그래프(MonthlyCategoryReport)와 같은 conic-gradient 링 방식을 쓴다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { formatWon, getCompositionRatios, toneColor } from '@/utils/asset';

const props = defineProps({
    composition: { type: Array, required: true },
});

const segments = computed(() =>
    getCompositionRatios(props.composition.filter((item) => item.amount > 0)),
);

const chartStyle = computed(() => {
    if (!segments.value.length) {
        return { background: 'var(--tt-border)' };
    }
    let offset = 0;
    const stops = segments.value.map((segment) => {
        const start = offset;
        const end = offset + segment.ratio * 100;
        offset = end;
        return `${toneColor(segment.tone)} ${start}% ${end}%`;
    });
    return { background: `conic-gradient(${stops.join(', ')})` };
});
</script>

<template>
    <BaseCard class="asset-composition">
        <template #header>자산 구성</template>

        <div class="asset-composition__body">
            <div class="asset-composition__chart">
                <div
                    class="asset-composition__donut"
                    :style="chartStyle"
                    aria-label="자산 구성 비율"
                >
                    <span></span>
                    <b>총자산</b>
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
    flex-shrink: 0;
    width: 132px;
    height: 132px;
}

.asset-composition__donut {
    position: relative;
    width: 100%;
    height: 100%;
    border-radius: 50%;
}

.asset-composition__donut span {
    position: absolute;
    inset: 31px;
    background: var(--tt-bg);
    border-radius: 50%;
}

.asset-composition__donut > b {
    position: absolute;
    inset: 0;
    display: grid;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
    place-items: center;
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

/* 금액을 전체 자릿수로 적으면서 좁아졌다 — 넘치면 라벨(「예금·적금」 등)을 줄여 금액을 지킨다. */
.asset-composition__legend-label {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    color: var(--tt-text-muted);
    white-space: nowrap;
    text-overflow: ellipsis;
}

.asset-composition__legend-amount {
    flex: none;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    white-space: nowrap;
}
</style>
