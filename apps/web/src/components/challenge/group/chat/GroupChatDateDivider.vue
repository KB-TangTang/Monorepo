<script setup>
defineProps({
    label: { type: String, required: true },
});
</script>

<template>
    <div class="date-divider">
        <span class="date-divider__pill">{{ label }}</span>
    </div>
</template>

<style scoped>
.date-divider {
    display: flex;
    justify-content: center;
    padding: var(--tt-space-2) 0;
}

.date-divider__pill {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    color: var(--tt-text-muted);
    background: var(--tt-bg-fill);
    padding: 4px 14px;
    border-radius: var(--tt-radius-full);
}
</style>
