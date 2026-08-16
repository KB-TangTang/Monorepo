<script setup>
import { ref } from 'vue';

defineProps({
    disabled: { type: Boolean, default: false },
    /**
     * 스티커 버튼 노출 여부. 스티커 기능은 이슈 #174 설계상 이번 범위 밖이라 기본 숨김이다.
     * 버튼 마크업·이벤트는 나중에 스티커를 붙일 때 바로 되살릴 수 있게 지우지 않고 남겨둔다.
     */
    showStickerButton: { type: Boolean, default: false },
});

const emit = defineEmits(['send-text', 'toggle-sticker']);

const text = ref('');

function handleSend() {
    const val = text.value.trim();
    if (!val) return;
    emit('send-text', val);
    text.value = '';
}

function handleKeydown(e) {
    if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) {
        e.preventDefault();
        handleSend();
    }
}
</script>

<template>
    <div class="chat-input" :class="{ 'chat-input--disabled': disabled }">
        <div class="chat-input__row">
            <button
                v-if="showStickerButton"
                class="chat-input__sticker-btn"
                :disabled="disabled"
                @click="emit('toggle-sticker')"
            >
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.8" />
                    <circle cx="9" cy="10" r="1.2" fill="currentColor" />
                    <circle cx="15" cy="10" r="1.2" fill="currentColor" />
                    <path
                        d="M8.5 14.5c1 1.5 5.5 1.5 7 0"
                        stroke="currentColor"
                        stroke-width="1.5"
                        stroke-linecap="round"
                    />
                </svg>
            </button>

            <input
                v-model="text"
                type="text"
                class="chat-input__field"
                placeholder="메시지 입력"
                :disabled="disabled"
                @keydown="handleKeydown"
            />

            <button
                class="chat-input__send"
                :disabled="disabled || !text.trim()"
                aria-label="보내기"
                @click="handleSend"
            >
                <svg width="18" height="18" viewBox="0 0 20 20" fill="none" aria-hidden="true">
                    <path
                        d="M17.5 2.5 9 11M17.5 2.5 12 17.5l-3-6.5-6.5-3 15-5.5Z"
                        stroke="currentColor"
                        stroke-width="1.6"
                        stroke-linejoin="round"
                    />
                </svg>
            </button>
        </div>

        <p class="chat-input__hint">텍스트만 보낼 수 있어요 · 사진 전송 미지원</p>
    </div>
</template>

<style scoped>
.chat-input {
    flex: none;
    background: var(--tt-bg);
    border-top: 1px solid var(--tt-border);
    padding: var(--tt-space-3) var(--tt-screen-padding);
    padding-bottom: max(var(--tt-space-3), env(safe-area-inset-bottom));
}

.chat-input__row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.chat-input--disabled {
    opacity: 0.5;
    pointer-events: none;
}

.chat-input__sticker-btn {
    border: none;
    background: none;
    color: var(--tt-text-hint);
    cursor: pointer;
    padding: 6px;
    display: flex;
    flex: none;
}

.chat-input__field {
    flex: 1;
    min-width: 0;
    border: none;
    background: var(--tt-bg-fill);
    border-radius: var(--tt-radius-full);
    padding: 12px 18px;
    font-family: inherit;
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
    outline: none;
}

.chat-input__field::placeholder {
    color: var(--tt-text-hint);
}

.chat-input__field:focus-visible {
    outline: 2px solid var(--tt-info);
    outline-offset: 1px;
}

.chat-input__send {
    border: none;
    background: var(--tt-primary);
    color: var(--tt-text-inverse);
    width: 44px;
    height: 44px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    flex: none;
    box-shadow: var(--tt-elevation-btn);
    transition: transform 0.12s ease-out;
}

.chat-input__send:active {
    transform: scale(0.92);
}

.chat-input__send:disabled {
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
    cursor: default;
    box-shadow: none;
}

.chat-input__hint {
    margin: var(--tt-space-2) 0 0;
    text-align: center;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
}

@media (prefers-reduced-motion: reduce) {
    .chat-input__send {
        transition: none;
    }

    .chat-input__send:active {
        transform: none;
    }
}
</style>
