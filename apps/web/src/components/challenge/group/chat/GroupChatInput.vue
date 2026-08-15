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
        <button
            v-if="showStickerButton"
            class="chat-input__sticker-btn"
            @click="emit('toggle-sticker')"
            :disabled="disabled"
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
            placeholder="메시지를 입력하세요"
            :disabled="disabled"
            @keydown="handleKeydown"
        />

        <button class="chat-input__send" :disabled="disabled || !text.trim()" @click="handleSend">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path d="M3 10l14-7-7 14v-7H3z" fill="currentColor" />
            </svg>
        </button>
    </div>
</template>

<style scoped>
.chat-input {
    flex: none;
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    padding: var(--tt-space-2) var(--tt-screen-padding) var(--tt-space-3);
    background: var(--tt-bg);
    border-top: 1px solid var(--tt-border);
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
    padding: 10px 16px;
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
    outline: none;
}

.chat-input__field::placeholder {
    color: var(--tt-text-hint);
}

.chat-input__send {
    border: none;
    background: var(--tt-primary);
    color: #fff;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    flex: none;
}

.chat-input__send:disabled {
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
    cursor: default;
}
</style>
