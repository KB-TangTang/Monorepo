<!--
  용도: 도장·인장 그래픽. 유죄 GUILTY / 무죄 NOT GUILTY / 확정 CONFIRMED 를 기울여 찍은 모양으로 그린다.
  언제 쓰는지: 판결이 확정된 카드 위에 결과를 시각적으로 못박을 때. JudgmentCard 의 #seal 슬롯에 넣는다.
  쓰면 안 되는 경우: 목록에서 상태만 알리면 되는 자리(BaseBadge). 도장은 한 화면에 여러 개 찍으면 값이 떨어진다.

  ※ 7종 중 이것만 BaseCard 를 감싸지 않는다 — 카드가 아니라 카드 위에 얹는 장식이라 테두리·그림자가 있으면 안 된다.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    verdict: {
        type: String,
        default: 'guilty',
        validator: (v) => ['guilty', 'innocent', 'confirmed'].includes(v),
    },
    size: {
        type: String,
        default: 'md',
        validator: (v) => ['sm', 'md', 'lg'].includes(v),
    },
    /* 찍힌 느낌을 주는 기울기(도). 디자인시스템 기준 -8 ~ -12 */
    angle: { type: Number, default: -10 },
});

const VERDICTS = {
    guilty: { ko: '유죄', en: 'GUILTY' },
    innocent: { ko: '무죄', en: 'NOT GUILTY' },
    confirmed: { ko: '확정', en: 'CONFIRMED' },
};

const label = computed(() => VERDICTS[props.verdict] ?? VERDICTS.guilty);
</script>

<template>
    <div
        class="seal"
        :class="[`seal--${verdict}`, `seal--${size}`]"
        :style="{ transform: `rotate(${angle}deg)` }"
        role="img"
        :aria-label="`${label.ko} 인장`"
    >
        <span class="seal__ko">{{ label.ko }}</span>
        <span class="seal__en">{{ label.en }}</span>
    </div>
</template>

<style scoped>
.seal {
    display: inline-flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    aspect-ratio: 1;
    font-family: var(--tt-font-sans);
    text-align: center;
    border: 3px solid currentColor;
    border-radius: var(--tt-radius-full);
    /* 잉크가 고르게 묻지 않은 느낌 — 이미지 없이 그라데이션으로만 */
    opacity: 0.88;
    box-shadow: inset 0 0 0 2px var(--tt-bg);
}

.seal--sm {
    width: 64px;
    font-size: var(--tt-fs-mono-chip);
}

.seal--md {
    width: 92px;
    font-size: var(--tt-fs-caption);
}

.seal--lg {
    width: 124px;
    font-size: var(--tt-fs-body);
}

.seal__ko {
    font-size: 1.6em;
    font-weight: var(--tt-fw-black);
    line-height: 1.1;
    letter-spacing: 0.04em;
}

.seal__en {
    margin-top: 2px;
    font-size: 0.72em;
    font-weight: var(--tt-fw-bold);
    line-height: 1.2;
    letter-spacing: 0.12em;
}

/* ── 판정별 색 ────────────────────────────────────── */
.seal--guilty {
    color: var(--tt-danger);
    background: var(--tt-danger-subtle);
}

.seal--innocent {
    color: var(--tt-innocent-800);
    background: var(--tt-success-subtle);
}

/* 확정 도장은 인장이므로 목재 계열을 쓴다 (인장·판사봉·종이 질감 한정 규칙) */
.seal--confirmed {
    color: var(--tt-wood);
    background: var(--tt-kraft);
}
</style>
