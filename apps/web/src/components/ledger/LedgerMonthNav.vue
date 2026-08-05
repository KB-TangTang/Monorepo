<!--
  용도: 조회 월 이동(‹ N년 M월 ›) + 결제수단 필터 진입 버튼.
  언제 쓰는지: LedgerView 한 곳에서만 렌더한다.
-->
<script setup>
import { formatMonthLabel } from '@/utils/ledger';

defineProps({
    period: { type: String, required: true },
    disablePrev: { type: Boolean, default: false },
    disableNext: { type: Boolean, default: false },
    paymentMethodLabel: { type: String, default: '전체 수단' },
});

defineEmits(['prev', 'next', 'open-payment-filter']);
</script>

<template>
    <div class="ledger-month-nav">
        <div class="ledger-month-nav__stepper">
            <button
                type="button"
                aria-label="이전 달"
                :disabled="disablePrev"
                @click="$emit('prev')"
            >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m15 5-7 7 7 7" />
                </svg>
            </button>
            <span>{{ formatMonthLabel(period) }}</span>
            <button
                type="button"
                aria-label="다음 달"
                :disabled="disableNext"
                @click="$emit('next')"
            >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                    <path d="m9 5 7 7-7 7" />
                </svg>
            </button>
        </div>

        <button
            type="button"
            class="ledger-month-nav__filter"
            @click="$emit('open-payment-filter')"
        >
            {{ paymentMethodLabel }}
        </button>
    </div>
</template>

<style scoped>
.ledger-month-nav {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
}

.ledger-month-nav__stepper {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.ledger-month-nav__stepper button {
    display: grid;
    width: 28px;
    height: 28px;
    color: var(--tt-text-muted);
    background: transparent;
    border: 0;
    cursor: pointer;
    place-items: center;
}

.ledger-month-nav__stepper button:disabled {
    color: var(--tt-border-strong);
    cursor: not-allowed;
}

.ledger-month-nav__stepper svg {
    width: 18px;
    height: 18px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.2;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.ledger-month-nav__filter {
    padding: var(--tt-space-2) var(--tt-space-4);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    cursor: pointer;
}
</style>
