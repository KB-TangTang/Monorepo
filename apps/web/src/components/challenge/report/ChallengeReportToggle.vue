<script setup>
defineProps({
    active: {
        type: String,
        default: 'report',
        validator: (v) => ['transactions', 'report'].includes(v),
    },
});

defineEmits(['open-transactions', 'open-monthly-report']);
</script>

<template>
    <nav class="report-toggle" aria-label="자료실 화면 전환">
        <button
            type="button"
            :class="{ 'report-toggle__active': active === 'transactions' }"
            :aria-current="active === 'transactions' ? 'page' : undefined"
            @click="$emit('open-transactions')"
        >
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M6 3.5h12v17l-3-2-3 2-3-2-3 2z" />
                <path d="M9 8h6M9 12h6" />
            </svg>
            <span>거래내역</span>
            <i v-if="active === 'transactions'" aria-hidden="true"></i>
        </button>
        <button
            type="button"
            :class="{ 'report-toggle__active': active === 'report' }"
            :aria-current="active === 'report' ? 'page' : undefined"
            @click="$emit('open-monthly-report')"
        >
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="M5 3.5h10l4 4v13H5z" />
                <path d="M15 3.5v4h4M9 16v-3M12 16V9M15 16v-5" />
            </svg>
            <span>리포트</span>
            <i v-if="active === 'report'" aria-hidden="true"></i>
        </button>
    </nav>
</template>

<style scoped>
.report-toggle {
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

.report-toggle button {
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

.report-toggle svg {
    width: 18px;
    height: 18px;
    fill: none;
    stroke: currentColor;
    stroke-width: 1.9;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.report-toggle .report-toggle__active {
    color: var(--tt-text);
    background: var(--tt-accent);
}

.report-toggle__active i {
    position: absolute;
    top: calc(var(--tt-space-1) * -1);
    right: var(--tt-space-1);
    width: 16px;
    height: 16px;
    background: color-mix(in srgb, var(--tt-text) 55%, var(--tt-accent));
    border: 2px solid var(--tt-bg);
    border-radius: var(--tt-radius-full);
}

.report-toggle__active i::after {
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
