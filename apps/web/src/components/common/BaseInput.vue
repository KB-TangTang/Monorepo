<!--
  용도: 라벨 · 힌트 · 에러 · 글자수 카운터를 포함한 기본 입력. multiline 으로 textarea 모드가 된다.
  언제 쓰는지: 모든 단일행 입력과 변론 · 코멘트 같은 여러 줄 입력.
  쓰면 안 되는 경우: 금액 키패드 · 날짜 피커 등 전용 입력 UI. 그건 도메인 컴포넌트로 따로 만든다.
-->
<script setup>
import { computed, useId } from 'vue';

const props = defineProps({
    modelValue: { type: [String, Number], default: '' },
    label: { type: String, default: '' },
    placeholder: { type: String, default: '' },
    hint: { type: String, default: '' },
    error: { type: String, default: '' },
    type: { type: String, default: 'text' },
    maxlength: { type: [String, Number], default: null },
    rows: { type: [String, Number], default: 4 },
    multiline: { type: Boolean, default: false },
    disabled: { type: Boolean, default: false },
    required: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue']);

const inputId = useId();
const length = computed(() => String(props.modelValue ?? '').length);
const hasError = computed(() => props.error !== '');
const describedBy = computed(() => (props.hint || props.error ? `${inputId}-desc` : undefined));

function onInput(event) {
    emit('update:modelValue', event.target.value);
}
</script>

<template>
    <div class="tt-field" :class="{ 'tt-field--error': hasError, 'tt-field--disabled': disabled }">
        <div v-if="label" class="tt-field__labelrow">
            <label class="tt-field__label" :for="inputId">
                {{ label }}
                <span v-if="required" class="tt-field__required" aria-hidden="true">*</span>
            </label>
            <span v-if="maxlength" class="tt-field__counter">{{ length }}/{{ maxlength }}</span>
        </div>

        <textarea
            v-if="multiline"
            :id="inputId"
            class="tt-field__control tt-field__control--multiline"
            :value="modelValue"
            :placeholder="placeholder"
            :maxlength="maxlength"
            :rows="rows"
            :disabled="disabled"
            :aria-invalid="hasError"
            :aria-describedby="describedBy"
            @input="onInput"
        ></textarea>
        <input
            v-else
            :id="inputId"
            class="tt-field__control"
            :type="type"
            :value="modelValue"
            :placeholder="placeholder"
            :maxlength="maxlength"
            :disabled="disabled"
            :aria-invalid="hasError"
            :aria-describedby="describedBy"
            @input="onInput"
        />

        <p v-if="error" :id="`${inputId}-desc`" class="tt-field__message tt-field__message--error">
            {{ error }}
        </p>
        <p v-else-if="hint" :id="`${inputId}-desc`" class="tt-field__message">{{ hint }}</p>
    </div>
</template>

<style scoped>
.tt-field {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
    font-family: var(--tt-font-sans);
}

.tt-field__labelrow {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-2);
}

.tt-field__label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text);
}

.tt-field__required {
    color: var(--tt-danger);
}

.tt-field__counter {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.tt-field__control {
    width: 100%;
    padding: var(--tt-space-3) var(--tt-space-4);
    font-family: inherit;
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    outline: none;
    transition:
        border-color 0.15s ease,
        box-shadow 0.15s ease;
}

.tt-field__control::placeholder {
    color: var(--tt-gray-500);
}

.tt-field__control:focus {
    border-color: var(--tt-primary);
    box-shadow: 0 0 0 3px var(--tt-primary-subtle);
}

.tt-field__control--multiline {
    resize: vertical;
    line-height: var(--tt-lh-normal);
}

.tt-field--error .tt-field__control {
    border-color: var(--tt-danger);
}

.tt-field--error .tt-field__control:focus {
    box-shadow: 0 0 0 3px var(--tt-danger-subtle);
}

.tt-field--disabled .tt-field__control {
    background: var(--tt-bg-subtle);
    color: var(--tt-text-muted);
    cursor: not-allowed;
}

.tt-field__message {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.tt-field__message--error {
    color: var(--tt-danger);
}
</style>
