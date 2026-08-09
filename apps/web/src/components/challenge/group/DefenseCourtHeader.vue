<!--
  Court Bar 헤더 — 변론 플로우 전체에서 공유하는 Ink 상단 바.
  뒤로가기 + 슬롯(nav-title, nav-right, default)으로 화면별 내용을 채운다.
  01(위반 상세)은 제목+사건번호, 02a~02d(입력형)은 타이머+마스코트 조합.
-->
<script setup>
import { useRouter } from 'vue-router';

defineProps({
    /** 탭바 옆 제목 (01 화면 등) */
    title: { type: String, default: '' },
    /** 사건번호 (모노스페이스) */
    caseNumber: { type: String, default: '' },
});

const emit = defineEmits(['back']);
const router = useRouter();

function goBack() {
    emit('back');
    router.back();
}
</script>

<template>
    <header class="court-header">
        <div class="court-header__deco"></div>

        <!-- 네비게이션 행 -->
        <div class="court-header__nav">
            <button
                type="button"
                class="court-header__back"
                aria-label="뒤로가기"
                @click="goBack"
            >
                <svg viewBox="0 0 9 16" fill="none" aria-hidden="true">
                    <path
                        d="M8 1L1 8l7 7"
                        stroke="currentColor"
                        stroke-width="2.2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                    />
                </svg>
            </button>

            <span v-if="title" class="court-header__title">{{ title }}</span>
            <slot name="nav-title" />

            <span v-if="caseNumber" class="court-header__case">{{ caseNumber }}</span>
            <slot name="nav-right" />
        </div>

        <!-- 본문 영역 (태그, 제목, 마스코트 등) -->
        <div v-if="$slots.default" class="court-header__body">
            <slot />
        </div>
    </header>
</template>

<style scoped>
.court-header {
    flex: none;
    background: var(--tt-surface-inverse);
    padding-bottom: var(--tt-space-5);
    border-radius: 0 0 26px 26px;
    position: relative;
    overflow: hidden;
}

.court-header__deco {
    position: absolute;
    right: -30px;
    top: -30px;
    width: 120px;
    height: 120px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.04);
}

/* ── 네비게이션 ── */
.court-header__nav {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--tt-space-1) var(--tt-screen-padding) 0;
    position: relative;
    z-index: 2;
}

.court-header__back {
    display: grid;
    place-items: center;
    width: 32px;
    height: 40px;
    padding: 0;
    color: var(--tt-white);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.court-header__back svg {
    width: 9px;
    height: 16px;
}

.court-header__title {
    flex: 1;
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
    margin-left: var(--tt-space-2);
}

.court-header__case {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
    font-family: var(--tt-font-mono);
}

/* ── 본문 ── */
.court-header__body {
    padding: var(--tt-space-4) 22px 0;
    position: relative;
    z-index: 2;
}
</style>
