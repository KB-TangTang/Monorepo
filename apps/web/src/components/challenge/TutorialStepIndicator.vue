<script setup>
defineProps({
    currentStep: {
        type: Number,
        required: true,
    },
    totalSteps: {
        type: Number,
        default: 4,
    },
});
</script>

<template>
    <div class="tut-indicator">
        <span
            v-for="i in totalSteps"
            :key="i"
            class="tut-indicator__dot"
            :class="{ 'tut-indicator__dot--active': currentStep === i - 1 }"
        />
    </div>
</template>

<style scoped>
.tut-indicator {
    display: flex;
    align-items: center;
    gap: 6px;
}

.tut-indicator__dot {
    width: 6px;
    height: 6px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-divider, #D4D9E2);
    transition: width 0.3s ease, background 0.3s ease;
}

.tut-indicator__dot--active {
    width: 22px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-accent);
}
</style>
