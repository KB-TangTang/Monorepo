<!--
  용도: 조회 · 요청이 실패했을 때 보여주는 에러 상태. 기본 재시도 버튼을 포함한다.
  언제 쓰는지: API 호출이 reject 됐을 때. ApiError 의 message 를 그대로 넘기면 된다.
  쓰면 안 되는 경우: 입력값 검증 실패(BaseInput 의 error prop), 데이터가 0건인 정상 상태(StateEmpty).
-->
<script setup>
import BaseButton from './BaseButton.vue';

defineProps({
    title: { type: String, default: '문제가 발생했어요' },
    message: { type: String, default: '잠시 후 다시 시도해 주세요.' },
    retryable: { type: Boolean, default: true },
    retryLabel: { type: String, default: '다시 시도' },
});

defineEmits(['retry']);
</script>

<template>
    <div class="tt-error" role="alert">
        <div class="tt-error__icon">
            <slot name="icon">
                <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.75"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                >
                    <path d="M12 4a8 8 0 1 0 0 16 8 8 0 0 0 0-16z" />
                    <path d="M12 8.5v4.5" />
                    <path d="M12 16h.01" />
                </svg>
            </slot>
        </div>
        <p class="tt-error__title">{{ title }}</p>
        <p v-if="message" class="tt-error__message">{{ message }}</p>
        <div class="tt-error__action">
            <slot name="action">
                <BaseButton v-if="retryable" variant="ghost" size="sm" @click="$emit('retry')">
                    {{ retryLabel }}
                </BaseButton>
            </slot>
        </div>
    </div>
</template>

<style scoped>
.tt-error {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-2);
    padding: var(--tt-space-10) var(--tt-space-5);
    font-family: var(--tt-font-sans);
    text-align: center;
}

.tt-error__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    margin-bottom: var(--tt-space-1);
    color: var(--tt-danger);
    background: var(--tt-danger-subtle);
    border-radius: var(--tt-radius-full);
}

.tt-error__icon :deep(svg) {
    width: 28px;
    height: 28px;
}

.tt-error__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.tt-error__message {
    max-width: 26em;
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.tt-error__action {
    margin-top: var(--tt-space-3);
}
</style>
