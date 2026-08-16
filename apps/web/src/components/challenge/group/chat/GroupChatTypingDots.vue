<script setup>
defineProps({
    name: { type: String, default: '' },
});
</script>

<template>
    <div class="typing">
        <span v-if="name" class="typing__name">{{ name }}</span>
        <span class="typing__dots">
            <span class="typing__dot" />
            <span class="typing__dot" />
            <span class="typing__dot" />
        </span>
    </div>
</template>

<style scoped>
.typing {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 0 4px;
}

.typing__name {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
}

.typing__dots {
    display: flex;
    gap: 3px;
}

.typing__dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: var(--tt-text-hint);
    animation: dotHop 1.2s infinite ease-in-out;
}

.typing__dot:nth-child(2) {
    animation-delay: 0.2s;
}
.typing__dot:nth-child(3) {
    animation-delay: 0.4s;
}

@keyframes dotHop {
    0%,
    60%,
    100% {
        transform: translateY(0);
    }
    30% {
        transform: translateY(-4px);
    }
}
</style>
