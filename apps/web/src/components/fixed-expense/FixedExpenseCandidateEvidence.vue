<script setup>
defineProps({
    count: { type: Number, required: true },
    months: { type: Array, required: true },
});
</script>

<template>
    <section class="candidate-evidence" aria-labelledby="candidate-evidence-title">
        <header>
            <h2 id="candidate-evidence-title">반복 결제 감지</h2>
            <strong>{{ count }} / {{ count }}</strong>
        </header>
        <ol>
            <li v-for="item in months" :key="item.month">
                <span :class="{ 'candidate-evidence__mark--detected': item.detected }">
                    {{ item.detected ? '✓' : '?' }}
                </span>
                <small>{{ item.month }}월</small>
            </li>
        </ol>
    </section>
</template>

<style scoped>
.candidate-evidence {
    padding: var(--tt-space-4) var(--tt-space-5);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
}

.candidate-evidence header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.candidate-evidence h2 {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
}

.candidate-evidence header strong {
    font-family: var(--tt-font-mono);
    color: var(--tt-text-subtle);
}

.candidate-evidence ol {
    display: flex;
    justify-content: space-between;
    margin-top: var(--tt-space-3);
}

.candidate-evidence li {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--tt-space-2);
}

.candidate-evidence li span {
    display: grid;
    width: 36px;
    height: 36px;
    place-items: center;
    font-weight: var(--tt-fw-black);
    color: var(--tt-border-strong);
    background: var(--tt-surface-row);
    border-radius: var(--tt-radius-full);
}

.candidate-evidence li .candidate-evidence__mark--detected {
    color: var(--tt-accent);
    background: var(--tt-surface-strong);
}

.candidate-evidence small {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-subtle);
}
</style>
