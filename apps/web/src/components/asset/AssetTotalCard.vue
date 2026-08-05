<!--
  용도: 자산 상세 화면 상단의 어두운 합계 카드. 라벨 · 총액 · (선택) 우측 메타 텍스트 · (선택) 하단 보조 영역을 보여준다.
  언제 쓰는지: 입출금·예적금·투자증권·대출 4개 상세 화면 전용. default 슬롯은 증감 배지·원금 같은 보조 정보에 쓴다.
-->
<script setup>
import { formatWon } from '@/utils/asset';

defineProps({
    label: { type: String, required: true },
    amount: { type: Number, required: true },
});
</script>

<template>
    <div class="asset-total">
        <div class="asset-total__row">
            <span class="asset-total__label">{{ label }}</span>
            <span v-if="$slots.meta" class="asset-total__meta"><slot name="meta" /></span>
        </div>
        <p class="asset-total__amount">{{ formatWon(amount) }}</p>
        <div v-if="$slots.default" class="asset-total__extra"><slot /></div>
    </div>
</template>

<style scoped>
.asset-total {
    padding: var(--tt-space-6) var(--tt-space-5);
    color: var(--tt-text-inverse);
    background: var(--tt-gray-900);
    border-radius: var(--tt-radius-xl);
}

.asset-total__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
}

.asset-total__label {
    font-size: var(--tt-fs-caption);
    color: var(--tt-border-strong);
}

.asset-total__meta {
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-border-strong);
}

.asset-total__amount {
    margin-top: var(--tt-space-2);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
}

.asset-total__extra {
    margin-top: var(--tt-space-3);
}
</style>
