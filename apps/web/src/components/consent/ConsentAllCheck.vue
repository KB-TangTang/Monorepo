<!--
  용도: 동의 화면 상단의 "약관 전체 동의" 마스터 체크.
  언제 쓰는지: 항목이 3개 이상인 동의 화면.
  쓰면 안 되는 경우: 항목이 하나뿐인 화면(금융데이터 동의 화면은 쓰지 않는다).
-->
<script setup>
defineProps({
    modelValue: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue']);

function onToggle(event) {
    emit('update:modelValue', event.target.checked);
}
</script>

<template>
    <label class="consent-all">
        <input
            class="consent-all__check"
            type="checkbox"
            :checked="modelValue"
            aria-label="약관 전체 동의"
            @change="onToggle"
        />
        <span class="consent-all__label">약관 전체 동의</span>
    </label>
</template>

<style scoped>
.consent-all {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4) var(--tt-space-5);
    background: var(--tt-text);
    border-radius: var(--tt-radius-lg);
    cursor: pointer;
}

.consent-all__check {
    width: 22px;
    height: 22px;
    accent-color: var(--tt-accent);
    flex-shrink: 0;
}

.consent-all__label {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-inverse);
}
</style>
