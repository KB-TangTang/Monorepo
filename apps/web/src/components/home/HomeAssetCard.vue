<!--
  용도: 홈 「자산 요약」 카드. 순자산과 자산 구성 비율을 도넛 + 범례로 보여주고 자산탭으로 보낸다.
  구성 비율(%)은 백엔드가 합계를 안 내려줘서 프론트에서 계산한다 — 자산탭과 같은
  utils/asset.js 의 getCompositionRatios 를 그대로 써서 두 화면의 숫자가 갈라지지 않게 한다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { formatAssetHomeWon, formatWon, getCompositionRatios, toneColor } from '@/utils/asset';

const props = defineProps({
    /* fetchAssetSummary() 의 결과. 계좌 연동 전이거나 조회 실패면 null. */
    summary: { type: Object, default: null },
});

defineEmits(['open']);

const composition = computed(() => props.summary?.composition ?? []);
const segments = computed(() =>
    getCompositionRatios(composition.value).filter((item) => item.ratio > 0),
);

const donutStyle = computed(() => {
    if (segments.value.length === 0) {
        return { background: 'var(--tt-border-track)' };
    }

    const stops = segments.value.map((item) => {
        const from = (item.offset * 100).toFixed(2);
        const to = ((item.offset + item.ratio) * 100).toFixed(2);
        return `${toneColor(item.tone)} ${from}% ${to}%`;
    });

    return { background: `conic-gradient(${stops.join(', ')})` };
});
</script>

<template>
    <BaseCard class="asset" clickable padding="none" @click="$emit('open')">
        <p class="asset__label">자산 요약</p>

        <p class="asset__net-worth">
            <template v-if="summary">순자산 {{ formatAssetHomeWon(summary.netWorth) }}</template>
            <template v-else>자산 연결이 필요해요</template>
        </p>

        <!-- 카드 전체가 자산탭으로 가는 버튼인데 그걸 알려주는 표시가 없었다. 저울 장식 대신 화살표를 둔다. -->
        <span class="asset__mark" aria-hidden="true">›</span>

        <div v-if="summary" class="asset__body">
            <div class="asset__donut" :style="donutStyle">
                <span class="asset__donut-hole"></span>
                <b class="asset__donut-label">총자산</b>
            </div>

            <ul class="asset__legend">
                <li v-for="item in segments" :key="item.code" class="asset__legend-item">
                    <span
                        class="asset__dot"
                        :style="{ background: toneColor(item.tone) }"
                        aria-hidden="true"
                    ></span>
                    <span class="asset__legend-label">
                        {{ item.label }} {{ Math.round(item.ratio * 100) }}%
                    </span>
                    <span class="asset__legend-amount">{{ formatWon(item.amount) }}</span>
                </li>
            </ul>
        </div>

        <p v-else class="asset__empty">계좌를 연결하면 자산 구성을 그려드릴게요</p>
    </BaseCard>
</template>

<style scoped>
.asset {
    position: relative;
    padding: var(--tt-space-4);
    border-radius: var(--tt-radius-xl);
}

.asset__label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.asset__net-worth {
    margin-top: var(--tt-space-1);
    padding-right: var(--tt-space-5);
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
}

.asset__mark {
    position: absolute;
    top: var(--tt-space-4);
    right: var(--tt-space-4);
    color: var(--tt-text-hint);
    font-size: var(--tt-fs-label);
    line-height: 1;
}

.asset__body {
    display: flex;
    align-items: center;
    gap: var(--tt-space-5);
    margin-top: var(--tt-space-4);
}

.asset__donut {
    position: relative;
    flex: none;
    width: 96px;
    height: 96px;
    border-radius: var(--tt-radius-full);
}

/*
 * inset 이 곧 고리 굵기다(96px 도넛에 구멍 56px).
 * 더 키우면 구멍이 닫혀 파이차트로 읽히니 20px 언저리가 끝이다.
 */
.asset__donut-hole {
    position: absolute;
    inset: 20px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
}

/*
 * 가운데에는 라벨만 두고 금액은 넣지 않는다. 예전에는 금액까지 적었는데
 * 바로 위 「순자산」 줄과 읽는 사람 눈에는 같은 말이라, 한 카드가 같은 숫자를
 * 두 번 말하는 꼴이었다. 금액은 위 한 줄에서만 말한다.
 * 자산탭 AssetCompositionCard 의 도넛과 같은 표기·같은 톤을 쓴다 — 두 화면이 갈라지면
 * 같은 그림이 화면마다 다른 말을 하는 것처럼 보인다.
 */
.asset__donut-label {
    position: absolute;
    inset: 0;
    display: grid;
    place-items: center;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.asset__legend {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
    flex: 1;
    min-width: 0;
    list-style: none;
}

.asset__legend-item {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.asset__dot {
    flex: none;
    width: 8px;
    height: 8px;
    border-radius: var(--tt-radius-xs);
}

.asset__legend-label {
    overflow: hidden;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    white-space: nowrap;
    text-overflow: ellipsis;
}

.asset__legend-amount {
    flex: none;
    margin-left: auto;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
}

.asset__empty {
    margin-top: var(--tt-space-3);
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}
</style>
