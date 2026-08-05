<!--
  용도: 구글 로그인 시작 버튼. 로그인 화면 전용이다.
  언제 쓰는지: /login 화면 하나. 세 번째 화면에서 또 필요해지면 common/ 으로 올린다(3의 법칙).
  쓰면 안 되는 경우: 일반 액션 버튼 — BaseButton 을 쓴다.

  BaseButton 을 쓰지 않는 이유: Figma 의 남색 배경이 BaseButton 의 4개 variant 에 없고,
  구글 로고 슬롯까지 넣으면 범용 버튼이 오염된다.
-->
<script setup>
defineProps({
    label: { type: String, default: 'Google로 판결 시작하기' },
});

const emit = defineEmits(['click-login']);
</script>

<template>
    <button class="google-signin" type="button" @click="emit('click-login')">
        <span class="google-signin__logo">
            <img src="@/assets/images/google-logo.svg" alt="" width="20" height="20" />
        </span>
        <span class="google-signin__label">{{ label }}</span>
    </button>
</template>

<style scoped>
.google-signin {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-3);
    width: 100%;
    height: 60px;
    padding: 0 var(--tt-space-5);
    border: none;
    border-radius: var(--tt-radius-lg);
    background: var(--tt-text);
    color: var(--tt-text-inverse);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    line-height: 1;
    cursor: pointer;
    transition: filter 0.15s ease;
}

.google-signin:hover {
    filter: brightness(1.15);
}

/* 구글 브랜드 가이드라인 — 로고는 흰 배경 위에 둔다 */
.google-signin__logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg);
}
</style>
