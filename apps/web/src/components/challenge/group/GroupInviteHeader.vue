<script setup>
import { ChevronLeftIcon, XMarkIcon } from '@heroicons/vue/24/solid';

/*
 * `navMode` 는 「어디로 가느냐」가 아니라 **「돌아갈 앞 화면이 있느냐」**를 나타낸다.
 * 목록·상세에서 온 경우에는 되돌아갈 자리가 있으니 ← 를, 생성 완료 직후처럼 되돌아갈 자리가
 * 없는(있어도 돌아가면 안 되는) 경우에는 「닫기」인 × 를 쓴다. 아이콘만 바꾸는 게 아니라
 * 사용자에게 다른 약속을 하는 것이라 호출부가 명시적으로 정한다.
 */
defineProps({
    title: { type: String, required: true },
    subtitle: { type: String, default: '' },
    badges: { type: Array, default: () => [] },
    navLabel: { type: String, default: '' },
    navMode: {
        type: String,
        default: 'back',
        validator: (v) => ['back', 'close'].includes(v),
    },
});

const emit = defineEmits(['back']);
</script>

<template>
    <header class="gih-header">
        <div class="gih-deco-circle gih-deco-circle--white" />

        <div class="gih-nav">
            <button
                type="button"
                class="gih-nav__back"
                :aria-label="navMode === 'close' ? '닫기' : '뒤로가기'"
                @click="emit('back')"
            >
                <component
                    :is="navMode === 'close' ? XMarkIcon : ChevronLeftIcon"
                    class="gih-nav__back-icon"
                />
            </button>
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
}

.gih-nav__back {
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

.gih-nav__back-icon {
    width: 22px;
    height: 22px;
    color: var(--tt-text-inverse);
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
