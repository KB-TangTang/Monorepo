<!--
  용도: 자산 홈 화면의 순자산 요약 카드. 금액 · 전월 대비 증감 · 추이 스파크라인을 한 장으로 보여준다.
  언제 쓰는지: AssetHomeView 상단 한 곳. 다른 화면에서 아직 필요 없다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import BaseBadge from '@/components/common/BaseBadge.vue';
import { formatSignedWon, formatWon, getSparklinePoints } from '@/utils/asset';

const props = defineProps({
    netWorth: { type: Number, required: true },
    monthOverMonthChange: { type: Number, required: true },
    trend: { type: Array, required: true },
});

defineEmits(['view-trend']);

const SPARK_WIDTH = 96;
const SPARK_HEIGHT = 40;
// 끝점 마커(circle r) 반지름. getSparklinePoints 의 padding 으로도 넘겨 마커가
// viewBox 가장자리에서 잘리지 않게 한다(이슈 #444) — 두 값이 어긋나면 다시 잘린다.
const SPARK_MARKER_RADIUS = 3;

/*
 * 순자산이 1000만원대로 올라가면 「9,824,700원」이 카드 폭을 넘겨 두 줄로 잘렸다.
 * 단위를 「~만원」으로 줄이면 끝자리가 사라지므로, 전체 금액을 유지한 채 자릿수만큼
 * 글자 크기를 낮춘다. 옆의 스파크라인(96px)이 폭을 고정으로 먹기 때문에 금액에 남는
 * 자리는 375px 화면에서 약 190px 뿐이다 — 아래 값은 그 폭에 「-1,234,567,890원」까지
 * 한 줄로 들어가도록 잡았다. 스파크라인 폭을 바꾸면 이 표도 같이 손봐야 한다.
 */
const AMOUNT_FONT_STEPS = [
    { maxDigits: 6, fontSize: 'var(--tt-fs-stat)' },
    { maxDigits: 7, fontSize: '26px' },
    { maxDigits: 8, fontSize: '24px' },
    { maxDigits: 9, fontSize: '22px' },
    { maxDigits: 10, fontSize: '20px' },
];
const AMOUNT_FONT_SIZE_MIN = '18px';

const changeVariant = computed(() => (props.monthOverMonthChange < 0 ? 'guilty' : 'innocent'));
const amountStyle = computed(() => {
    const digits = String(Math.abs(Math.trunc(props.netWorth))).length;
    const step = AMOUNT_FONT_STEPS.find((item) => digits <= item.maxDigits);
    return { fontSize: step ? step.fontSize : AMOUNT_FONT_SIZE_MIN };
});
const sparkline = computed(() =>
    getSparklinePoints(props.trend, SPARK_WIDTH, SPARK_HEIGHT, SPARK_MARKER_RADIUS),
);
</script>

<template>
    <BaseCard class="net-worth">
        <template #header>
            <div class="net-worth__head">
                <span>순자산</span>
                <button type="button" class="net-worth__trend-link" @click="$emit('view-trend')">
                    추이 보기 ›
                </button>
            </div>
        </template>

        <div class="net-worth__body">
            <div class="net-worth__main">
                <p class="net-worth__amount" :style="amountStyle">{{ formatWon(netWorth) }}</p>
                <div class="net-worth__change-row">
                    <BaseBadge :variant="changeVariant">
                        {{ formatSignedWon(monthOverMonthChange) }}
                    </BaseBadge>
                    <span class="net-worth__caption">지난달 대비</span>
                </div>
            </div>

            <svg
                class="net-worth__sparkline"
                :viewBox="`0 0 ${SPARK_WIDTH} ${SPARK_HEIGHT}`"
                preserveAspectRatio="none"
                aria-hidden="true"
            >
                <polyline
                    :points="sparkline.pointsAttr"
                    fill="none"
                    stroke="var(--tt-primary)"
                    stroke-width="2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                />
                <circle
                    v-if="sparkline.lastPoint"
                    :cx="sparkline.lastPoint.x"
                    :cy="sparkline.lastPoint.y"
                    :r="SPARK_MARKER_RADIUS"
                    fill="var(--tt-primary)"
                />
            </svg>
        </div>
    </BaseCard>
</template>

<style scoped>
.net-worth__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.net-worth__trend-link {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.net-worth__body {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: var(--tt-space-4);
}

/* font-size 는 자릿수에 따라 amountStyle 이 인라인으로 덮는다 — 여기 값은 기본값이다. */
.net-worth__amount {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
    white-space: nowrap;
}

.net-worth__change-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    margin-top: var(--tt-space-2);
}

.net-worth__caption {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.net-worth__sparkline {
    flex-shrink: 0;
    width: 96px;
    height: 40px;
}
</style>
