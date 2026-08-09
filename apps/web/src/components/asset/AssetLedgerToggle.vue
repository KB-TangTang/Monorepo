<!--
  용도: 자산 현황(AssetHomeView) ↔ 거래내역 홈(LedgerView) 전환 토글.
  언제 쓰는지: 두 화면 모두 하단 탭바 바로 위에 띄운다.
-->
<script setup>
defineProps({
    active: {
        type: String,
        default: 'asset',
        validator: (v) => ['asset', 'ledger'].includes(v),
    },
});

defineEmits(['open-asset', 'open-ledger']);
</script>

<template>
    <nav class="asset-ledger-toggle" aria-label="자산 현황·거래내역 화면 전환">
        <button
            type="button"
            :class="{ 'asset-ledger-toggle__active': active === 'asset' }"
            :aria-current="active === 'asset' ? 'page' : undefined"
            @click="$emit('open-asset')"
        >
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M3.5 8.5A2.5 2.5 0 0 1 6 6h11.5A1.5 1.5 0 0 1 19 7.5V9" />
                <path
                    d="M3.5 8.5v8A2.5 2.5 0 0 0 6 19h12a1.5 1.5 0 0 0 1.5-1.5v-7A1.5 1.5 0 0 0 18 9H6"
                />
                <path d="M20 12.5h-3a1.5 1.5 0 0 0 0 3h3" />
            </svg>
            <span>자산 현황</span>
            <i v-if="active === 'asset'" aria-hidden="true"></i>
        </button>
        <button
            type="button"
            :class="{ 'asset-ledger-toggle__active': active === 'ledger' }"
            :aria-current="active === 'ledger' ? 'page' : undefined"
            @click="$emit('open-ledger')"
        >
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 3.5h12v17l-3-2-3 2-3-2-3 2z" />
                <path d="M9 8h6M9 12h6" />
            </svg>
            <span>거래내역</span>
            <i v-if="active === 'ledger'" aria-hidden="true"></i>
        </button>
    </nav>
</template>

<style scoped>
.asset-ledger-toggle {
    position: fixed;
    bottom: calc(var(--tt-tabbar-height) + env(safe-area-inset-bottom) + var(--tt-space-3));
    left: 50%;
    z-index: var(--tt-z-sticky);
    display: grid;
    grid-template-columns: 1fr 1fr;
    width: min(236px, calc(100% - var(--tt-space-12)));
    padding: var(--tt-space-1);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    box-shadow: var(--tt-elevation-3);
    transform: translateX(-50%);
}

.asset-ledger-toggle button {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-1);
    min-height: 40px;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    background: transparent;
    border: 0;
    border-radius: var(--tt-radius-full);
    cursor: pointer;
}

.asset-ledger-toggle svg {
    width: 18px;
    height: 18px;
    fill: none;
    stroke: currentColor;
    stroke-width: 1.9;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.asset-ledger-toggle .asset-ledger-toggle__active {
    color: var(--tt-text);
    background: var(--tt-accent);
}

.asset-ledger-toggle__active i {
    position: absolute;
    top: calc(var(--tt-space-1) * -1);
    right: var(--tt-space-1);
    width: 16px;
    height: 16px;
    background: color-mix(in srgb, var(--tt-text) 55%, var(--tt-accent));
    border: 2px solid var(--tt-bg);
    border-radius: var(--tt-radius-full);
}

.asset-ledger-toggle__active i::after {
    position: absolute;
    top: 3px;
    left: 3px;
    width: 4px;
    height: 4px;
    content: '';
    background: var(--tt-bg);
    border-radius: var(--tt-radius-full);
}
</style>
