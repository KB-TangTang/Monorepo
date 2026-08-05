<!--
  용도: 자산 상세 화면(입출금·예적금·투자증권·대출) 공통 상단 바. 뒤로가기 + 화면 제목만 담당한다.
  언제 쓰는지: AssetHomeView 의 자산 목록 항목을 눌러 진입하는 4개 상세 화면 전용.
-->
<script setup>
import { useRouter } from 'vue-router';

defineProps({
    title: { type: String, required: true },
});

const router = useRouter();

function goBack() {
    router.back();
}
</script>

<template>
    <header class="asset-detail-header">
        <button
            type="button"
            class="asset-detail-header__back"
            aria-label="자산 홈으로 돌아가기"
            @click="goBack"
        >
            <svg viewBox="0 0 24 24" aria-hidden="true">
                <path d="m15 4-8 8 8 8" />
            </svg>
        </button>
        <h1 class="asset-detail-header__title">{{ title }}</h1>
    </header>
</template>

<style scoped>
.asset-detail-header {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    margin-bottom: var(--tt-space-4);
}

.asset-detail-header__back {
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

.asset-detail-header__back svg {
    width: 28px;
    height: 28px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.2;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.asset-detail-header__title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
</style>
