<script setup>
import { ref, computed, nextTick } from 'vue';

const props = defineProps({
    modelValue: { type: String, default: '' },
    length: { type: Number, default: 5 },
    disabled: { type: Boolean, default: false },
});

const emit = defineEmits(['update:model-value', 'complete']);

const hiddenInput = ref(null);

const cells = computed(() => {
    const chars = props.modelValue.split('');
    return Array.from({ length: props.length }, (_, i) => chars[i] || '');
});

const focusIndex = computed(() => {
    return Math.min(props.modelValue.length, props.length - 1);
});

function focusInput() {
    if (!props.disabled) {
        hiddenInput.value?.focus();
    }
}

function handleInput(e) {
    const raw = e.target.value
        .toUpperCase()
        .replace(/[^A-Z0-9]/g, '')
        .slice(0, props.length);

    emit('update:model-value', raw);
    e.target.value = raw;

    if (raw.length >= props.length) {
        nextTick(() => emit('complete'));
    }
}

function handleKeydown(e) {
    if (e.key === 'Backspace' && props.modelValue.length === 0) {
        e.preventDefault();
    }
}
</script>

<template>
    <div class="gci-wrap" @click="focusInput">
        <input
            ref="hiddenInput"
            type="text"
            class="gci-hidden"
            :value="modelValue"
            :disabled="disabled"
            :maxlength="length"
            inputmode="text"
            autocomplete="off"
            autocapitalize="characters"
            @input="handleInput"
            @keydown="handleKeydown"
        />
        <div class="gci-cells">
            <div
                v-for="(char, i) in cells"
                :key="i"
                class="gci-cell"
                :class="{
                    'gci-cell--focus': i === focusIndex && !disabled && modelValue.length < length,
                    'gci-cell--filled': char !== '',
                }"
            >
                <span v-if="char" class="gci-char">{{ char }}</span>
                <span
                    v-else-if="i === focusIndex && !disabled && modelValue.length < length"
                    class="gci-caret"
                />
            </div>
        </div>
    </div>
</template>

<style scoped>
.gci-wrap {
    cursor: text;
}

.gci-hidden {
    position: absolute;
    width: 1px;
    height: 1px;
    opacity: 0;
    pointer-events: none;
}

.gci-cells {
    display: flex;
    gap: 8px;
    justify-content: center;
}

.gci-cell {
    width: 46px;
    height: 56px;
    border-radius: 12px;
    border: 1.5px solid var(--tt-border);
    background: var(--tt-bg-subtle);
    display: flex;
    align-items: center;
    justify-content: center;
    transition: border-color 0.15s ease, background 0.15s ease;
}

.gci-cell--focus {
    border: 2px solid var(--tt-info);
    background: var(--tt-bg);
}

.gci-cell--filled {
    background: var(--tt-bg-subtle);
}

.gci-char {
    font-size: 24px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gci-caret {
    width: 2px;
    height: 26px;
    background: var(--tt-info);
    animation: tt-caret 1s step-end infinite;
}

@keyframes tt-caret {
    0%, 49% { opacity: 1; }
    50%, 100% { opacity: 0; }
}
</style>
