<!--
  용도: 자산 상세 화면의 계좌·상품 한 행. 이니셜 배지 · 이름 · 보조 설명(계좌번호·만기·금리) · 금액을 보여준다.
  언제 쓰는지: 입출금·예적금·대출 상세 화면 3곳. amount 가 음수면 빨간색으로 표시된다(대출 잔액).
-->
<script setup>
import { formatWon, toneColor } from '@/utils/asset';

defineProps({
    badge: { type: String, required: true },
    label: { type: String, required: true },
    meta: { type: String, default: '' },
    amount: { type: Number, required: true },
    tone: { type: String, default: 'gray' },
});
</script>

<template>
    <li class="asset-row">
        <span class="asset-row__avatar" :style="{ background: toneColor(tone) }" aria-hidden="true">
            {{ badge }}
        </span>
        <div class="asset-row__info">
            <p class="asset-row__name">{{ label }}</p>
            <p v-if="meta" class="asset-row__meta">{{ meta }}</p>
        </div>
        <p class="asset-row__amount" :class="{ 'asset-row__amount--negative': amount < 0 }">
            {{ formatWon(amount) }}
        </p>
    </li>
</template>

<style scoped>
.asset-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
}

.asset-row__avatar {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-inverse);
    border-radius: var(--tt-radius-full);
}

.asset-row__info {
    flex: 1;
    min-width: 0;
}

.asset-row__name {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.asset-row__meta {
    margin-top: 2px;
    overflow: hidden;
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
    text-overflow: ellipsis;
    white-space: nowrap;
}

.asset-row__amount {
    flex-shrink: 0;
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.asset-row__amount--negative {
    color: var(--tt-danger);
}
</style>
