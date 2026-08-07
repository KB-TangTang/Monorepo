<!--
  용도: 뒤로가기 + 화면 제목만 담당하는 공용 상세화면 상단 바.
  언제 쓰는지: 자산 상세 4개 화면, 장부 상세내역 화면 등 "뒤로가기 헤더"가 필요한 모든 화면.
-->
<script setup>
import { useRouter } from 'vue-router';

defineProps({
    title: { type: String, required: true },
    backLabel: { type: String, default: '뒤로가기' },
});

const router = useRouter();

function goBack() {
    router.back();
}
</script>

<template>
    <header class="base-back-header">
        <button
            type="button"
            class="base-back-header__back"
            :aria-label="backLabel"
            @click="goBack"
        >
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m15 4-8 8 8 8" />
            </svg>
        </button>
        <h1 class="base-back-header__title">{{ title }}</h1>
    </header>
</template>

<style scoped>
.base-back-header {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    margin-bottom: var(--tt-space-4);
}

.base-back-header__back {
    display: grid;
    flex: 0 0 32px;
    width: 32px;
    height: 40px;
    padding: 0;
    color: var(--tt-text);
    background: transparent;
    border: 0;
    cursor: pointer;
    place-items: center;
}

.base-back-header__back svg {
    width: 28px;
    height: 28px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.2;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.base-back-header__title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
</style>
