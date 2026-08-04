<script setup>
defineProps({
    title: { type: String, required: true },
    backLabel: { type: String, default: '' },
    actionLabel: { type: String, default: '' },
    actionDisabled: { type: Boolean, default: false },
});

defineEmits(['back', 'action']);
</script>

<template>
    <header class="fixed-expense-header">
        <button
            v-if="backLabel"
            class="fixed-expense-header__back"
            type="button"
            :aria-label="backLabel"
            @click="$emit('back')"
        >
            ‹
        </button>
        <h1>{{ title }}</h1>
        <button
            v-if="actionLabel"
            class="fixed-expense-header__action"
            type="button"
            :disabled="actionDisabled"
            @click="$emit('action')"
        >
            {{ actionLabel }}
        </button>
    </header>
</template>

<style scoped>
.fixed-expense-header {
    position: relative;
    display: flex;
    align-items: center;
    min-height: 48px;
}

.fixed-expense-header h1 {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-surface-strong);
}

.fixed-expense-header__back {
    width: 28px;
    margin-right: var(--tt-space-1);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    line-height: 1;
    color: var(--tt-surface-strong);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.fixed-expense-header__action {
    margin-left: auto;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-subtle);
    background: transparent;
    border: 0;
}

.fixed-expense-header__action:disabled {
    cursor: default;
    opacity: 1;
}

@media (max-width: 360px) {
    .fixed-expense-header h1 {
        font-size: var(--tt-fs-section);
    }
}
</style>
