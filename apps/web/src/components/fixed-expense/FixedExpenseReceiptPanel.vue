<script setup>
import BaseCard from '@/components/common/BaseCard.vue';

defineProps({
    heading: { type: String, required: true },
});
</script>

<template>
    <BaseCard class="fixed-receipt" padding="none">
        <div class="fixed-receipt__heading">{{ heading }}</div>
        <div class="fixed-receipt__body"><slot /></div>
        <div v-if="$slots.footer" class="fixed-receipt__footer"><slot name="footer" /></div>
    </BaseCard>
</template>

<style scoped>
.fixed-receipt {
    position: relative;
    border-radius: var(--tt-radius-md) var(--tt-radius-md) 0 0;
    box-shadow: var(--tt-elevation-0);
}

.fixed-receipt::after {
    position: absolute;
    right: 0;
    bottom: -8px;
    left: 0;
    height: 16px;
    content: '';
    background: radial-gradient(circle at 8px 16px, transparent 8px, var(--tt-bg) 8.5px);
    background-size: 16px 16px;
}

.fixed-receipt__heading {
    padding: var(--tt-space-4) var(--tt-space-5);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.18em;
    text-align: center;
    color: var(--tt-text-soft);
    border-bottom: 1px dashed var(--tt-border);
}

.fixed-receipt__body {
    padding: var(--tt-space-4) var(--tt-space-5);
}

.fixed-receipt__footer {
    margin: 0 var(--tt-space-5);
    padding: var(--tt-space-4) 0 var(--tt-space-5);
    border-top: 1px dashed var(--tt-border);
}
</style>
