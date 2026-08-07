<script setup>
defineProps({
    title: { type: String, required: true },
    subtitle: { type: String, default: '' },
    badges: { type: Array, default: () => [] },
    navLabel: { type: String, default: '' },
});

const emit = defineEmits(['back']);
</script>

<template>
    <header class="gih-header">
        <div class="gih-deco-circle gih-deco-circle--white" />

        <div class="gih-nav" @click="emit('back')">
            <svg width="9" height="16" viewBox="0 0 9 16" fill="none" aria-hidden="true">
                <path d="M8 1L1 8l7 7" stroke="#fff" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
            </svg>
            <span class="gih-nav__label">{{ navLabel }}</span>
            <span v-if="badges.length" class="gih-nav__badges">
                <span
                    v-for="(badge, i) in badges"
                    :key="i"
                    class="gih-badge"
                    :class="`gih-badge--${badge.variant || 'default'}`"
                >
                    {{ badge.text }}
                </span>
            </span>
        </div>

        <div class="gih-meta">
            <h2 class="gih-title" v-html="title" />
        </div>
    </header>
</template>

<style scoped>
.gih-header {
    background: var(--tt-surface-inverse);
    border-radius: 0 0 var(--tt-radius-2xl) var(--tt-radius-2xl);
    padding: var(--tt-space-10) var(--tt-screen-padding) 34px;
    position: relative;
    overflow: hidden;
    flex: none;
}

/* ── decorative circle ─────────────────── */
.gih-deco-circle {
    position: absolute;
    border-radius: 50%;
    pointer-events: none;
}

.gih-deco-circle--white {
    top: -26px;
    right: -18px;
    width: 124px;
    height: 124px;
    background: rgba(255, 255, 255, 0.05);
}

/* ── navigation row ────────────────────── */
.gih-nav {
    display: flex;
    align-items: center;
    gap: 9px;
    position: relative;
    z-index: 2;
    cursor: pointer;
}

.gih-nav__label {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
}

.gih-nav__badges {
    margin-left: auto;
    display: flex;
    gap: 7px;
}

/* ── badges ─────────────────────────────── */
.gih-badge {
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.gih-badge--success {
    background: var(--tt-success-subtle);
    color: var(--tt-success);
}

.gih-badge--danger {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
}

.gih-badge--default {
    background: rgba(245, 185, 33, 0.16);
    color: var(--tt-accent);
}

/* ── title ──────────────────────────────── */
.gih-meta {
    padding-top: 14px;
    position: relative;
    z-index: 2;
}

.gih-title {
    font-size: 22px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
    line-height: 1.3;
    letter-spacing: -0.01em;
}
</style>
