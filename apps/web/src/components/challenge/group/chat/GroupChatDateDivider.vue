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
    margin: var(--tt-space-3) 0 var(--tt-space-1);
}

.date-divider__pill {
    background: var(--tt-bg-fill);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    padding: 5px 14px;
    border-radius: var(--tt-radius-full);
}
</style>
