<!--
  용도: 투자증권 상세 화면의 보유 종목 한 행. 종목명 · 수량·평단가 · 평가금액 · 수익률을 보여준다.
  언제 쓰는지: AssetInvestmentView 하나뿐. returnRate 는 소수(0.124 = +12.4%)로 받는다.
-->
<script setup>
import { formatWon, formatSignedPercent, toneColor } from '@/utils/asset';

defineProps({
    badge: { type: String, required: true },
    name: { type: String, required: true },
    quantity: { type: Number, required: true },
    unitPrice: { type: Number, required: true },
    amount: { type: Number, required: true },
    returnRate: { type: Number, required: true },
    tone: { type: String, default: 'gray' },
});
</script>

<template>
    <li class="holding-row">
        <span
            class="holding-row__avatar"
            :style="{ background: toneColor(tone) }"
            aria-hidden="true"
        >
            {{ badge }}
        </span>
        <div class="holding-row__info">
            <p class="holding-row__name">{{ name }}</p>
            <p class="holding-row__meta">
                {{ quantity }}주 · {{ unitPrice.toLocaleString('ko-KR') }}
            </p>
        </div>
        <div class="holding-row__figures">
            <p class="holding-row__amount">{{ formatWon(amount) }}</p>
            <p
                class="holding-row__rate"
                :class="returnRate < 0 ? 'holding-row__rate--down' : 'holding-row__rate--up'"
            >
                {{ formatSignedPercent(returnRate) }}
            </p>
        </div>
    </li>
</template>

<style scoped>
.holding-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
}

.holding-row__avatar {
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

.holding-row__info {
    flex: 1;
    min-width: 0;
}

.holding-row__name {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.holding-row__meta {
    margin-top: 2px;
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.holding-row__figures {
    flex-shrink: 0;
    text-align: right;
}

.holding-row__amount {
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.holding-row__rate {
    margin-top: 2px;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
}

.holding-row__rate--up {
    color: var(--tt-success);
}

.holding-row__rate--down {
    color: var(--tt-danger);
}
</style>
