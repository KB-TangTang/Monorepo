<script setup>
import { ChevronLeftIcon } from '@heroicons/vue/24/solid';

defineProps({
    step: { type: Number, required: true },
    title: { type: String, required: true },
    subtitle: { type: String, default: '' },
});

const emit = defineEmits(['back']);
</script>

<template>
    <header class="gcr-header">
        <div class="gcr-deco-circle gcr-deco-circle--white" />
        <div class="gcr-deco-circle gcr-deco-circle--gold" />

        <div class="gcr-nav">
            <button type="button" class="gcr-nav__back" aria-label="뒤로가기" @click="emit('back')">
                <ChevronLeftIcon class="gcr-nav__back-icon" />
            </button>
            <span class="gcr-nav__label">그룹 만들기</span>
            <span class="gcr-nav__sub">
                <slot name="subtitle">
                    <template v-if="subtitle">{{ subtitle }}</template>
                </slot>
            </span>
        </div>

        <div class="gcr-meta">
            <span class="gcr-step-badge">{{ step }} / 4</span>
            <h2 class="gcr-title" v-html="title" />
        </div>
    </header>
</template>

<style scoped>
.gcr-header {
    background: var(--tt-surface-inverse);
    border-radius: 0 0 var(--tt-radius-2xl) var(--tt-radius-2xl);
    padding: var(--tt-space-10) var(--tt-screen-padding) 34px;
    position: relative;
    overflow: hidden;
    flex: none;
}

/* ── decorative circles ──────────────────── */
.gcr-deco-circle {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
}

.gcr-deco-circle--white {
    top: -26px;
    right: -18px;
    width: 124px;
    height: 124px;
    background: rgba(255, 255, 255, 0.05);
}

.gcr-deco-circle--gold {
    bottom: 6px;
    right: 56px;
    width: 58px;
    height: 58px;
    background: rgba(245, 185, 33, 0.07);
}

/* ── navigation row ──────────────────────── */
.gcr-nav {
    display: flex;
    align-items: center;
    gap: 9px;
    position: relative;
    z-index: 2;
}

.gcr-nav__back {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    margin: -12px;
    padding: 0;
    background: none;
    border: none;
    cursor: pointer;
}

.gcr-nav__back-icon {
    width: 22px;
    height: 22px;
    color: var(--tt-text-inverse);
}

.gcr-nav__label {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
}

.gcr-nav__sub {
    margin-left: auto;
    font-size: 11.5px;
    font-weight: var(--tt-fw-bold);
    color: rgba(174, 178, 204, 1);
}

/* ── step badge + title ──────────────────── */
.gcr-meta {
    padding-top: 14px;
    position: relative;
    z-index: 2;
}

.gcr-step-badge {
    display: inline-block;
    background: var(--tt-accent);
    color: var(--tt-primary);
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    padding: 4px 11px;
    border-radius: var(--tt-radius-full);
}

.gcr-title {
    font-size: 22px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
    line-height: 1.3;
    margin-top: 11px;
    letter-spacing: -0.01em;
}
</style>
