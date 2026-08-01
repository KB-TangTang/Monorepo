<!--
  용도: 영수증 카드. 항목별 금액을 괘선 위에 늘어놓고 절취선 아래에 소계 · 한도 · 초과액을 정렬한다.
  언제 쓰는지: 하루치 지출 명세, 챌린지 한도 대비 사용액 정산.
  쓰면 안 되는 경우: 항목이 없는 요약 수치(BentoStats), 확정된 판결(JudgmentCard).
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';

const props = defineProps({
    title: { type: String, default: '' },
    /* [{ label: '편의점', amount: 4800 }, …] */
    items: { type: Array, default: () => [] },
    /* 한도. null 이면 초과액 줄을 그리지 않는다 */
    limit: { type: Number, default: null },
    issuedAt: { type: String, default: '' },
});

const won = (n) => `${Number(n).toLocaleString('ko-KR')}원`;

const subtotal = computed(() => props.items.reduce((sum, it) => sum + Number(it.amount || 0), 0));
const excess = computed(() =>
    props.limit === null ? 0 : Math.max(0, subtotal.value - props.limit),
);
const isOver = computed(() => excess.value > 0);
</script>

<template>
    <BaseCard class="receipt" variant="receipt" padding="md">
        <template v-if="title || issuedAt" #header>
            <div class="receipt__head">
                <p class="receipt__title">{{ title }}</p>
                <p v-if="issuedAt" class="receipt__issued">{{ issuedAt }}</p>
            </div>
        </template>

        <ul v-if="items.length" class="receipt__items">
            <li v-for="(it, i) in items" :key="`${it.label}-${i}`" class="receipt__row">
                <span class="receipt__label">{{ it.label }}</span>
                <span class="receipt__amount">{{ won(it.amount) }}</span>
            </li>
        </ul>
        <slot />

        <div class="receipt__tear" aria-hidden="true"></div>

        <dl class="receipt__summary">
            <div class="receipt__srow">
                <dt>소계</dt>
                <dd>{{ won(subtotal) }}</dd>
            </div>
            <div v-if="limit !== null" class="receipt__srow">
                <dt>한도</dt>
                <dd>{{ won(limit) }}</dd>
            </div>
            <div
                v-if="limit !== null"
                class="receipt__srow receipt__srow--total"
                :class="{ 'receipt__srow--over': isOver }"
            >
                <dt>{{ isOver ? '초과액' : '잔여' }}</dt>
                <dd>{{ won(isOver ? excess : limit - subtotal) }}</dd>
            </div>
        </dl>
    </BaseCard>
</template>

<style scoped>
.receipt {
    background: var(--tt-kraft); /* 종이 질감 — 영수증은 종이 계열 허용 대상 */
}

.receipt__head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-2);
}

.receipt__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
}

.receipt__issued {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-regular);
    color: var(--tt-text-muted);
}

/* 괘선 — 각 항목 줄 아래 옅은 선 */
.receipt__row {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-3);
    padding: var(--tt-space-2) 0;
    font-size: var(--tt-fs-caption);
    border-bottom: 1px solid var(--tt-border);
}

.receipt__label {
    color: var(--tt-text);
}

.receipt__amount {
    font-family: var(--tt-font-mono);
    font-variant-numeric: tabular-nums;
    color: var(--tt-text);
}

/* 절취선 — 카드 좌우 끝까지 뻗는 점선 + 양끝 노치.
 * 노치는 카드가 페이지 배경(--tt-bg-subtle) 위에 놓인다고 가정한다. */
.receipt__tear {
    position: relative;
    height: 0;
    margin: var(--tt-space-4) calc(var(--tt-space-4) * -1);
    border-top: 2px dashed var(--tt-border-strong);
}

.receipt__tear::before,
.receipt__tear::after {
    position: absolute;
    top: -9px;
    width: 16px;
    height: 16px;
    content: '';
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-full);
}

.receipt__tear::before {
    left: -9px;
}

.receipt__tear::after {
    right: -9px;
}

.receipt__summary {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.receipt__srow {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.receipt__srow dd {
    font-family: var(--tt-font-mono);
    font-variant-numeric: tabular-nums;
}

.receipt__srow--total {
    padding-top: var(--tt-space-2);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-success);
    border-top: 1px solid var(--tt-border-strong);
}

.receipt__srow--over {
    color: var(--tt-danger);
}
</style>
