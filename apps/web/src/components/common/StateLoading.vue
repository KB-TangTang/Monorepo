<!--
  용도: 데이터를 불러오는 동안 보여주는 로딩 상태. 스피너 모양과 크기를 여기서만 정한다.
  언제 쓰는지: 화면 · 목록 · 카드 영역의 로딩. inline 모드는 버튼 옆 같은 좁은 자리에 쓴다.
  쓰면 안 되는 경우: 버튼 자체의 로딩(BaseButton 의 loading prop), 1초 안에 끝나는 짧은 전환.
-->
<script setup>
defineProps({
    message: { type: String, default: '' },
    size: {
        type: String,
        default: 'md',
        validator: (v) => ['sm', 'md', 'lg'].includes(v),
    },
    inline: { type: Boolean, default: false },
});
</script>

<template>
    <div
        class="tt-loading"
        :class="{ 'tt-loading--inline': inline }"
        role="status"
        aria-live="polite"
    >
        <span class="tt-loading__spinner" :class="`tt-loading__spinner--${size}`"></span>
        <p v-if="message" class="tt-loading__message">{{ message }}</p>
    </div>
</template>

<style scoped>
.tt-loading {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-10) var(--tt-space-5);
    font-family: var(--tt-font-sans);
    text-align: center;
}

.tt-loading--inline {
    flex-direction: row;
    gap: var(--tt-space-2);
    padding: 0;
}

.tt-loading__spinner {
    display: block;
    border: 2px solid var(--tt-border);
    border-top-color: var(--tt-primary);
    border-radius: var(--tt-radius-full);
    animation: tt-loading-spin 0.7s linear infinite;
}

.tt-loading__spinner--sm {
    width: 16px;
    height: 16px;
}

.tt-loading__spinner--md {
    width: 26px;
    height: 26px;
}

.tt-loading__spinner--lg {
    width: 36px;
    height: 36px;
    border-width: 3px;
}

.tt-loading__message {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

@keyframes tt-loading-spin {
    to {
        transform: rotate(360deg);
    }
}

@media (prefers-reduced-motion: reduce) {
    .tt-loading__spinner {
        animation-duration: 1.6s;
    }
}
</style>
