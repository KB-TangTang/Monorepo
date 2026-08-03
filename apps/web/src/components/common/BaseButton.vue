<!--
  용도: 서비스 전역에서 쓰는 기본 버튼. variant · size 조합으로만 형태를 바꾼다.
  언제 쓰는지: 모든 액션 버튼(제출 · 이동 · 확인 · 삭제). 로딩 상태가 필요한 버튼 포함.
  쓰면 안 되는 경우: 링크처럼 보여야 하는 텍스트(그냥 RouterLink 를 쓴다), 탭바 · 아이콘 전용 토글.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    variant: {
        type: String,
        default: 'primary',
        validator: (v) => ['primary', 'secondary', 'ghost', 'danger'].includes(v),
    },
    size: {
        type: String,
        default: 'md',
        validator: (v) => ['sm', 'md', 'lg'].includes(v),
    },
    type: { type: String, default: 'button' },
    block: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false },
    loading: { type: Boolean, default: false },
});

const emit = defineEmits(['click']);

const isLocked = computed(() => props.disabled || props.loading);

function onClick(event) {
    if (isLocked.value) {
        return;
    }
    emit('click', event);
}
</script>

<template>
    <button
        class="tt-btn"
        :class="[`tt-btn--${variant}`, `tt-btn--${size}`, { 'tt-btn--block': block }]"
        :type="type"
        :disabled="isLocked"
        :aria-busy="loading"
        @click="onClick"
    >
        <span v-if="loading" class="tt-btn__spinner" aria-hidden="true"></span>
        <span class="tt-btn__label"><slot /></span>
    </button>
</template>

<style scoped>
.tt-btn {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-2);
    border: 1px solid transparent;
    border-radius: var(--tt-radius-md);
    font-family: var(--tt-font-sans);
    font-weight: var(--tt-fw-bold);
    line-height: 1;
    cursor: pointer;
    transition:
        background-color 0.15s ease,
        border-color 0.15s ease,
        color 0.15s ease;
}

.tt-btn:disabled {
    cursor: not-allowed;
    opacity: 0.45;
}

.tt-btn--block {
    display: flex;
    width: 100%;
}

/* ── size ─────────────────────────────────────────── */
.tt-btn--sm {
    height: 34px;
    padding: 0 var(--tt-space-3);
    font-size: var(--tt-fs-caption);
}

.tt-btn--md {
    height: 44px;
    padding: 0 var(--tt-space-5);
    font-size: var(--tt-fs-body);
}

.tt-btn--lg {
    height: 54px;
    padding: 0 var(--tt-space-6);
    font-size: var(--tt-fs-section);
    border-radius: var(--tt-radius-lg);
}

/* ── variant ──────────────────────────────────────── */
.tt-btn--primary {
    background: var(--tt-primary);
    color: var(--tt-text-inverse);
}

.tt-btn--primary:not(:disabled):hover {
    background: var(--tt-primary-hover);
}

.tt-btn--secondary {
    background: var(--tt-primary-subtle);
    color: var(--tt-primary);
}

.tt-btn--secondary:not(:disabled):hover {
    background: var(--tt-brand-200);
}

.tt-btn--ghost {
    background: transparent;
    border-color: var(--tt-border);
    color: var(--tt-text);
}

.tt-btn--ghost:not(:disabled):hover {
    background: var(--tt-bg-subtle);
    border-color: var(--tt-border-strong);
}

.tt-btn--danger {
    background: var(--tt-danger);
    color: var(--tt-text-inverse);
}

.tt-btn--danger:not(:disabled):hover {
    filter: brightness(0.92);
}

/* ── loading ──────────────────────────────────────── */
.tt-btn__spinner {
    width: 1em;
    height: 1em;
    border: 2px solid currentColor;
    border-right-color: transparent;
    border-radius: var(--tt-radius-full);
    animation: tt-btn-spin 0.7s linear infinite;
}

@keyframes tt-btn-spin {
    to {
        transform: rotate(360deg);
    }
}
</style>
