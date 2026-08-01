<!--
  용도: 카드의 "셸"(테두리 · 라운드 · 그림자 · 여백)만 담당한다. 내용은 전부 슬롯으로 받는다.
  언제 쓰는지: 목록 항목 · 요약 블록 등 네모난 덩어리를 그릴 때. 카드 라이브러리(사건기록부 · 판결문 …)도 이걸 감싸서 만든다.
  쓰면 안 되는 경우: 내용을 이 파일 안에 하드코딩하고 싶을 때. 그런 카드는 도메인 폴더에 따로 만든다.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    variant: {
        type: String,
        default: 'plain',
        validator: (v) => ['plain', 'record', 'judgment', 'receipt'].includes(v),
    },
    padding: {
        type: String,
        default: 'md',
        validator: (v) => ['none', 'sm', 'md', 'lg'].includes(v),
    },
    clickable: { type: Boolean, default: false },
});

const emit = defineEmits(['click']);

const tag = computed(() => (props.clickable ? 'button' : 'div'));

function onClick(event) {
    if (props.clickable) {
        emit('click', event);
    }
}
</script>

<template>
    <component
        :is="tag"
        class="tt-card"
        :class="[
            `tt-card--${variant}`,
            `tt-card--pad-${padding}`,
            { 'tt-card--clickable': clickable },
        ]"
        :type="clickable ? 'button' : undefined"
        @click="onClick"
    >
        <header v-if="$slots.header" class="tt-card__header"><slot name="header" /></header>
        <div class="tt-card__body"><slot /></div>
        <footer v-if="$slots.footer" class="tt-card__footer"><slot name="footer" /></footer>
    </component>
</template>

<style scoped>
.tt-card {
    display: block;
    width: 100%;
    text-align: left;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    box-shadow: var(--tt-elevation-1);
}

/* ── padding ──────────────────────────────────────── */
.tt-card--pad-none {
    padding: 0;
}

.tt-card--pad-sm {
    padding: var(--tt-space-3);
}

.tt-card--pad-md {
    padding: var(--tt-space-4);
}

.tt-card--pad-lg {
    padding: var(--tt-space-6);
}

/* ── variant ──────────────────────────────────────── */
/* record — 사건기록부. 좌측 강조선 */
.tt-card--record {
    border-left: 3px solid var(--tt-primary);
}

/* judgment — 판결문. 두꺼운 테두리 + 종이 톤 */
.tt-card--judgment {
    border: 2px solid var(--tt-gray-900);
    border-radius: var(--tt-radius-sm);
    box-shadow: var(--tt-elevation-2);
}

/* receipt — 영수증. 종이 질감 + 아래 절취선 */
.tt-card--receipt {
    background: var(--tt-bg-subtle);
    border-style: dashed;
    border-radius: var(--tt-radius-sm);
    box-shadow: var(--tt-elevation-0);
}

/* ── clickable ────────────────────────────────────── */
.tt-card--clickable {
    cursor: pointer;
    transition:
        box-shadow 0.15s ease,
        border-color 0.15s ease;
}

.tt-card--clickable:hover {
    border-color: var(--tt-border-strong);
    box-shadow: var(--tt-elevation-2);
}

.tt-card--clickable:active {
    box-shadow: var(--tt-elevation-1);
}

/* ── slot 영역 ────────────────────────────────────── */
.tt-card__header {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    line-height: var(--tt-lh-snug);
}

.tt-card__footer {
    margin-top: var(--tt-space-4);
    padding-top: var(--tt-space-3);
    border-top: 1px solid var(--tt-border);
}
</style>
