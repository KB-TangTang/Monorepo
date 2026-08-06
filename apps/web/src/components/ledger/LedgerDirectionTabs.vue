<!--
  용도: 거래 상세내역 화면의 전체/지출/입금/카테고리/수단 필터 pill 5개.
  언제 쓰는지: LedgerMonthTransactionsView 한 곳에서만 렌더한다.
-->
<script setup>
const TABS = [
    { value: 'ALL', label: '전체' },
    { value: 'CONSUMPTION', label: '지출' },
    { value: 'INCOME', label: '입금' },
];

defineProps({
    active: { type: String, required: true },
    categoryLabel: { type: String, default: '카테고리' },
    paymentLabel: { type: String, default: '수단' },
});

defineEmits(['select', 'open-category-filter', 'open-payment-filter']);
</script>

<template>
    <div class="direction-tabs" role="tablist" aria-label="거래내역 필터">
        <button
            v-for="tab in TABS"
            :key="tab.value"
            type="button"
            role="tab"
            class="direction-tabs__pill"
            :class="{ 'direction-tabs__pill--active': active === tab.value }"
            :aria-selected="active === tab.value"
            @click="$emit('select', tab.value)"
        >
            {{ tab.label }}
        </button>
        <button
            type="button"
            class="direction-tabs__pill"
            @click="$emit('open-category-filter')"
        >
            {{ categoryLabel }}
        </button>
        <button
            type="button"
            class="direction-tabs__pill"
            @click="$emit('open-payment-filter')"
        >
            {{ paymentLabel }}
        </button>
    </div>
</template>

<style scoped>
.direction-tabs {
    display: flex;
    gap: var(--tt-space-2);
    overflow-x: auto;
}

.direction-tabs__pill {
    flex-shrink: 0;
    padding: var(--tt-space-2) var(--tt-space-4);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    white-space: nowrap;
    cursor: pointer;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.direction-tabs__pill--active {
    color: var(--tt-text-inverse);
    background: var(--tt-primary);
    border-color: var(--tt-primary);
}
</style>
