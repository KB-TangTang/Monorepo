<script setup>
defineProps({
    modelValue: { type: String, required: true },
    counts: {
        type: Object,
        default: () => ({ 'pre-start': 0, active: 0, ended: 0 }),
    },
});

defineEmits(['update:modelValue']);

const TABS = [
    { key: 'pre-start', label: '시작 전' },
    { key: 'active', label: '진행 중' },
    { key: 'ended', label: '종료됨' },
];
</script>

<template>
    <div class="gls-track">
        <button
            v-for="tab in TABS"
            :key="tab.key"
            type="button"
            :class="['gls-tab', { 'gls-tab--active': modelValue === tab.key }]"
            @click="$emit('update:modelValue', tab.key)"
        >
            {{ tab.label }}
            <span v-if="counts[tab.key]" class="gls-tab__count">{{ counts[tab.key] }}</span>
        </button>
    </div>
</template>

<style scoped>
.gls-track {
    display: flex;
    gap: 3px;
    padding: var(--tt-space-1);
    background: var(--tt-bg-fill);
    border-radius: var(--tt-radius-full);
}

.gls-tab {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 4px;
    padding: 8px 0;
    border: none;
    border-radius: var(--tt-radius-full);
    background: transparent;
    font-family: inherit;
    font-size: 12.5px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    cursor: pointer;
    transition: background 0.2s ease, color 0.2s ease;
}

.gls-tab--active {
    background: var(--tt-gold);
    color: var(--tt-text);
    font-weight: var(--tt-fw-black);
}

.gls-tab__count {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
}
</style>
