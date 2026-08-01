<!--
  용도: 화면 위에 겹쳐 뜨는 모달. 스크롤 잠금 · ESC · 오버레이 클릭 · 포커스 트랩 · 뒤로가기 닫기를 전부 처리한다.
  언제 쓰는지: 확인 · 경고 · 상세보기 등 모든 모달. ★ 각자 오버레이를 만들지 말고 반드시 이걸 쓴다.
  쓰면 안 되는 경우: 아래에서 올라오는 시트(BaseBottomSheet), 화면 전환이 나은 복잡한 폼(별도 라우트).
-->
<script setup>
import { computed, ref } from 'vue';
import { useOverlay } from './useOverlay';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    title: { type: String, default: '' },
    closeOnOverlay: { type: Boolean, default: true },
    closeOnEsc: { type: Boolean, default: true },
    showClose: { type: Boolean, default: true },
});

const emit = defineEmits(['update:modelValue', 'close']);

const panel = ref(null);
const isOpen = computed(() => props.modelValue);

function close() {
    emit('update:modelValue', false);
    emit('close');
}

function onOverlayClick() {
    if (props.closeOnOverlay) {
        close();
    }
}

useOverlay({
    isOpen,
    panelRef: panel,
    canCloseOnEsc: () => props.closeOnEsc,
    requestClose: close,
});
</script>

<template>
    <Teleport to="body">
        <Transition name="tt-modal">
            <div v-if="modelValue" class="tt-modal" @mousedown.self="onOverlayClick">
                <div
                    ref="panel"
                    class="tt-modal__panel"
                    role="dialog"
                    aria-modal="true"
                    :aria-label="title || undefined"
                    tabindex="-1"
                >
                    <header v-if="title || $slots.header || showClose" class="tt-modal__header">
                        <slot name="header">
                            <h2 class="tt-modal__title">{{ title }}</h2>
                        </slot>
                        <button
                            v-if="showClose"
                            class="tt-modal__close"
                            type="button"
                            aria-label="닫기"
                            @click="close"
                        >
                            ✕
                        </button>
                    </header>

                    <div class="tt-modal__body"><slot /></div>

                    <footer v-if="$slots.footer" class="tt-modal__footer">
                        <slot name="footer" />
                    </footer>
                </div>
            </div>
        </Transition>
    </Teleport>
</template>

<style scoped>
.tt-modal {
    position: fixed;
    inset: 0;
    z-index: var(--tt-z-overlay);
    display: flex;
    align-items: center;
    justify-content: center;
    padding: var(--tt-space-5);
    background: rgba(27, 33, 56, 0.48);
}

.tt-modal__panel {
    position: relative;
    z-index: var(--tt-z-modal);
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: var(--tt-content-max);
    max-height: 84vh;
    padding: var(--tt-space-5);
    font-family: var(--tt-font-sans);
    color: var(--tt-text);
    background: var(--tt-bg);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-3);
    outline: none;
}

.tt-modal__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--tt-space-3);
    margin-bottom: var(--tt-space-4);
}

.tt-modal__title {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    line-height: var(--tt-lh-snug);
}

.tt-modal__close {
    flex: none;
    width: 28px;
    height: 28px;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    background: transparent;
    border: none;
    border-radius: var(--tt-radius-full);
    cursor: pointer;
}

.tt-modal__close:hover {
    background: var(--tt-bg-subtle);
    color: var(--tt-text);
}

.tt-modal__body {
    overflow-y: auto;
    font-size: var(--tt-fs-body);
    line-height: var(--tt-lh-normal);
}

.tt-modal__footer {
    display: flex;
    gap: var(--tt-space-2);
    margin-top: var(--tt-space-5);
}

/* ── 전환 ─────────────────────────────────────────── */
.tt-modal-enter-active,
.tt-modal-leave-active {
    transition: opacity 0.18s ease;
}

.tt-modal-enter-active .tt-modal__panel,
.tt-modal-leave-active .tt-modal__panel {
    transition:
        transform 0.18s ease,
        opacity 0.18s ease;
}

.tt-modal-enter-from,
.tt-modal-leave-to {
    opacity: 0;
}

.tt-modal-enter-from .tt-modal__panel,
.tt-modal-leave-to .tt-modal__panel {
    transform: scale(0.96);
    opacity: 0;
}

@media (prefers-reduced-motion: reduce) {
    .tt-modal-enter-active,
    .tt-modal-leave-active,
    .tt-modal-enter-active .tt-modal__panel,
    .tt-modal-leave-active .tt-modal__panel {
        transition: none;
    }
}
</style>
