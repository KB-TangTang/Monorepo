<!--
  용도: 아래에서 올라오는 바텀시트. BaseModal 과 같은 규칙(스크롤 잠금 · ESC · 포커스 트랩 · 뒤로가기)을 공유한다.
  언제 쓰는지: 모바일에서 선택지 · 필터 · 간단한 폼을 띄울 때. 손이 닿는 아래쪽에서 조작해야 하는 경우.
  쓰면 안 되는 경우: 반드시 결정해야 하는 확인 다이얼로그(BaseModal). 시트는 아래로 끌어 닫을 수 있어 강제성이 약하다.
-->
<script setup>
import { computed, ref } from 'vue';
import { useOverlay } from './useOverlay';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    title: { type: String, default: '' },
    closeOnOverlay: { type: Boolean, default: true },
    closeOnEsc: { type: Boolean, default: true },
    /* 패널 높이를 고정한다. null 이면 콘텐츠에 맞춰 늘어난다 (기본) */
    height: { type: String, default: null },
    /* 드래그 핸들을 아래로 이만큼(px) 끌면 닫는다 */
    dragCloseThreshold: { type: Number, default: 80 },
});

const emit = defineEmits(['update:modelValue', 'close']);

const panel = ref(null);
const isOpen = computed(() => props.modelValue);
const dragOffset = ref(0);
const dragging = ref(false);
let startY = 0;

function close() {
    dragOffset.value = 0;
    dragging.value = false;
    emit('update:modelValue', false);
    emit('close');
}

function onOverlayClick() {
    if (props.closeOnOverlay) {
        close();
    }
}

function onDragStart(event) {
    dragging.value = true;
    startY = event.clientY;
    event.currentTarget.setPointerCapture?.(event.pointerId);
}

function onDragMove(event) {
    if (!dragging.value) {
        return;
    }
    dragOffset.value = Math.max(0, event.clientY - startY);
}

function onDragEnd() {
    if (!dragging.value) {
        return;
    }
    dragging.value = false;
    if (dragOffset.value >= props.dragCloseThreshold) {
        close();
    } else {
        dragOffset.value = 0;
    }
}

const { releaseHistory } = useOverlay({
    isOpen,
    panelRef: panel,
    canCloseOnEsc: () => props.closeOnEsc,
    requestClose: close,
});

/* 시트 안에서 다른 화면으로 이동하는 경우에만 쓴다. useOverlay 의 releaseHistory 주석 참고. */
defineExpose({ releaseHistory });
</script>

<template>
    <Teleport to="body">
        <Transition name="tt-sheet">
            <div v-if="modelValue" class="tt-sheet" @mousedown.self="onOverlayClick">
                <div
                    ref="panel"
                    class="tt-sheet__panel"
                    :class="{ 'tt-sheet__panel--dragging': dragging }"
                    :style="[
                        height ? { height } : null,
                        dragOffset ? { transform: `translateY(${dragOffset}px)` } : null,
                    ]"
                    role="dialog"
                    aria-modal="true"
                    :aria-label="title || undefined"
                    tabindex="-1"
                >
                    <div
                        class="tt-sheet__handle"
                        @pointerdown="onDragStart"
                        @pointermove="onDragMove"
                        @pointerup="onDragEnd"
                        @pointercancel="onDragEnd"
                    >
                        <span class="tt-sheet__grip" aria-hidden="true"></span>
                    </div>

                    <header v-if="title || $slots.header" class="tt-sheet__header">
                        <slot name="header">
                            <h2 class="tt-sheet__title">{{ title }}</h2>
                        </slot>
                    </header>

                    <div class="tt-sheet__body"><slot /></div>

                    <footer v-if="$slots.footer" class="tt-sheet__footer">
                        <slot name="footer" />
                    </footer>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<style scoped>
.tt-sheet {
    position: fixed;
    inset: 0;
    z-index: var(--tt-z-overlay);
    display: flex;
    align-items: flex-end;
    justify-content: center;
    background: var(--tt-overlay-dim);
    /*
     * 키보드가 가린 높이만큼 패널을 띄운다. iOS 는 키보드가 올라와도 fixed 요소를
     * 밀어주지 않아 이 값이 없으면 시트가 키보드 밑에 깔린다 (useOverlay 의 주석 참고).
     * 안드로이드·데스크톱에서는 값이 없거나 0 이라 아무 영향이 없다.
     */
    padding-bottom: var(--tt-keyboard-inset, 0px);
}

.tt-sheet__panel {
    position: relative;
    z-index: var(--tt-z-modal);
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: var(--tt-content-max);
    /* 키보드가 올라온 만큼 최대 높이도 줄여야 패널 위쪽이 화면 밖으로 넘어가지 않는다 */
    max-height: calc(88vh - var(--tt-keyboard-inset, 0px));
    padding: 0 var(--tt-space-5) calc(var(--tt-space-5) + env(safe-area-inset-bottom));
    font-family: var(--tt-font-sans);
    color: var(--tt-text);
    background: var(--tt-bg);
    border-radius: var(--tt-radius-xl) var(--tt-radius-xl) 0 0;
    box-shadow: var(--tt-elevation-3);
    outline: none;
    transition: transform 0.2s ease;
}

.tt-sheet__panel--dragging {
    transition: none;
}

.tt-sheet__handle {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 28px;
    margin-bottom: var(--tt-space-1);
    cursor: grab;
    touch-action: none; /* 핸들에서만 드래그를 가로챈다. 본문 스크롤은 그대로 둔다 */
}

.tt-sheet__grip {
    width: 40px;
    height: 4px;
    background: var(--tt-border-strong);
    border-radius: var(--tt-radius-full);
}

.tt-sheet__header {
    margin-bottom: var(--tt-space-4);
}

.tt-sheet__title {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    line-height: var(--tt-lh-snug);
}

.tt-sheet__body {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
    font-size: var(--tt-fs-body);
    line-height: var(--tt-lh-normal);
}

.tt-sheet__footer {
    display: flex;
    gap: var(--tt-space-2);
    margin-top: var(--tt-space-5);
}

/* ── 전환 ─────────────────────────────────────────── */
.tt-sheet-enter-active,
.tt-sheet-leave-active {
    transition: opacity 0.2s ease;
}

.tt-sheet-enter-active .tt-sheet__panel,
.tt-sheet-leave-active .tt-sheet__panel {
    transition: transform 0.2s ease;
}

.tt-sheet-enter-from,
.tt-sheet-leave-to {
    opacity: 0;
}

.tt-sheet-enter-from .tt-sheet__panel,
.tt-sheet-leave-to .tt-sheet__panel {
    transform: translateY(100%);
}

@media (prefers-reduced-motion: reduce) {
    .tt-sheet-enter-active,
    .tt-sheet-leave-active,
    .tt-sheet-enter-active .tt-sheet__panel,
    .tt-sheet-leave-active .tt-sheet__panel {
        transition: none;
    }
}
</style>
