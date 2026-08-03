<!--
  용도: 벤토 그리드. 통계 타일(누적 도전일수 · 스트릭 · 명예 점수 …)을 2열 격자로 늘어놓는다.
  언제 쓰는지: 마이·홈의 요약 수치 영역. 타일 하나가 수치 하나를 맡는다.
  쓰면 안 되는 경우: 항목별 명세(ReceiptCard), 설명이 긴 내용. 타일은 숫자 하나 + 라벨 한 줄이 전부다.
-->
<script setup>
import BaseCard from '@/components/common/BaseCard.vue';

defineProps({
    /* [{ label: '누적 도전일수', value: 42, unit: '일', tone: 'primary', span: 2, caption: '…' }, …]
     * tone: default | primary | accent | danger | success   span: 1 | 2 */
    items: { type: Array, default: () => [] },
});

const display = (v) => (typeof v === 'number' ? v.toLocaleString('ko-KR') : v);
</script>

<template>
    <div class="bento">
        <BaseCard
            v-for="(it, i) in items"
            :key="`${it.label}-${i}`"
            class="bento__tile"
            :class="[
                `bento__tile--${it.tone ?? 'default'}`,
                { 'bento__tile--wide': it.span === 2 },
            ]"
            padding="md"
        >
            <p class="bento__label">{{ it.label }}</p>
            <p class="bento__value">
                {{ display(it.value) }}<span v-if="it.unit" class="bento__unit">{{ it.unit }}</span>
            </p>
            <p v-if="it.caption" class="bento__caption">{{ it.caption }}</p>
        </BaseCard>
    </div>
</template>

<style scoped>
.bento {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: var(--tt-space-3);
}

.bento__tile--wide {
    grid-column: span 2;
}

.bento__label {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

/* 반칸 타일에는 title(26) 을 쓴다. numeric(34) 로 두면 6자리 금액에서 단위가 줄바꿈된다.
 * 가로 전체를 쓰는 wide 타일에서만 numeric 을 쓴다. */
.bento__value {
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.bento__tile--wide .bento__value {
    font-size: var(--tt-fs-numeric);
}

.bento__unit {
    margin-left: 2px;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.bento__caption {
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

/* ── tone ─────────────────────────────────────────── */
.bento__tile--primary {
    background: var(--tt-gray-900);
    border-color: var(--tt-gray-900);
}

.bento__tile--primary .bento__label,
.bento__tile--primary .bento__caption,
.bento__tile--primary .bento__unit {
    color: var(--tt-gray-400);
}

.bento__tile--primary .bento__value {
    color: var(--tt-text-inverse);
}

.bento__tile--accent {
    background: var(--tt-accent-subtle);
    border-color: var(--tt-accent);
}

.bento__tile--accent .bento__value {
    color: var(--tt-accent-700);
}

.bento__tile--danger {
    background: var(--tt-danger-subtle);
    border-color: var(--tt-guilty-300);
}

.bento__tile--danger .bento__value {
    color: var(--tt-danger);
}

.bento__tile--success {
    background: var(--tt-success-subtle);
    border-color: var(--tt-innocent-300);
}

.bento__tile--success .bento__value {
    color: var(--tt-innocent-800);
}
</style>
