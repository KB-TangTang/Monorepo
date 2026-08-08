<!--
  챌린지 약속 아코디언 — 그룹 생성 시 작성한 메모를 접었다 펼칠 수 있다.
-->
<script setup>
import { ref, computed } from 'vue';

const props = defineProps({
    memo: { type: String, default: null },
    memoAuthor: { type: String, default: '' },
    memoDate: { type: String, default: '' },
    /** true 면 부제까지 표시 (시작 전 상세용) */
    showSubtitle: { type: Boolean, default: false },
});

const isOpen = ref(false);
const hasMemo = computed(() => !!props.memo);

function toggle() {
    if (hasMemo.value) isOpen.value = !isOpen.value;
}
</script>

<template>
    <div v-if="hasMemo" class="promise">
        <div class="promise__trigger" @click="toggle">
            <div class="promise__icon-wrap">
                <svg class="promise__icon" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M11.5 8.5c.83 0 1.5-.67 1.5-1.5S12.33 5.5 11.5 5.5 10 6.17 10 7s.67 1.5 1.5 1.5zm5 0c.83 0 1.5-.67 1.5-1.5S17.33 5.5 16.5 5.5 15 6.17 15 7s.67 1.5 1.5 1.5zM22 3v7.5c0 1.1-.9 2-2 2h-2l-2.6 3.25c-.2.25-.6.25-.8 0L12 12.5H6c-1.1 0-2-.9-2-2V3c0-1.1.9-2 2-2h14c1.1 0 2 .9 2 2zm-2 0H6v7.5h6.6l1.4 1.75 1.4-1.75H20V3zM2 15.5c0 1.1.9 2 2 2h2v-2H4v-7h2V6.5H4c-1.1 0-2 .9-2 2v7zM14 14l-1.75 2.19L14 18.5l3-3.75L14 14zm-2 8h4v-1.5h-4V22z" />
                </svg>
            </div>
            <div class="promise__label">
                <div class="promise__title">챌린지 약속 보기</div>
                <div v-if="showSubtitle" class="promise__subtitle">
                    방 만들 때 적은 그룹원끼리의 내기 · 메모
                </div>
            </div>
            <svg
                class="promise__chevron"
                :class="{ 'promise__chevron--open': isOpen }"
                viewBox="0 0 24 24"
                fill="currentColor"
            >
                <path d="M7.41 8.59L12 13.17l4.59-4.58L18 10l-6 6-6-6z" />
            </svg>
        </div>

        <Transition name="promise-slide">
            <div v-if="isOpen" class="promise__body">
                <div class="promise__divider"></div>
                <p class="promise__text">{{ memo }}</p>
                <div class="promise__meta">{{ memoDate }} · {{ memoAuthor }} 작성</div>
            </div>
        </Transition>
    </div>
</template>

<style scoped>
.promise {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 16px;
    box-shadow: var(--tt-elevation-2);
    overflow: hidden;
}

.promise__trigger {
    padding: 9px 13px;
    display: flex;
    align-items: center;
    gap: 11px;
    cursor: pointer;
}

.promise__icon-wrap {
    width: 34px;
    height: 34px;
    border-radius: 11px;
    background: var(--tt-bg-fill);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.promise__icon {
    width: 19px;
    height: 19px;
    color: var(--tt-text);
}

.promise__label {
    flex: 1;
    min-width: 0;
}

.promise__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.promise__subtitle {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    margin-top: 2px;
}

.promise__chevron {
    width: 20px;
    height: 20px;
    color: var(--tt-text-hint);
    flex: none;
    transition: transform 0.25s ease;
}

.promise__chevron--open {
    transform: rotate(180deg);
}

/* ── 펼쳐지는 본문 ── */
.promise__body {
    padding: 0 13px 11px;
}

.promise__divider {
    border-top: 1px solid var(--tt-bg-fill);
    padding-top: 9px;
}

.promise__text {
    font-size: var(--tt-fs-caption);
    line-height: 1.65;
    color: var(--tt-text-body);
}

.promise__meta {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
    margin-top: 8px;
}

/* ── 슬라이드 트랜지션 ── */
.promise-slide-enter-active,
.promise-slide-leave-active {
    transition: all 0.25s ease;
    max-height: 200px;
    overflow: hidden;
}

.promise-slide-enter-from,
.promise-slide-leave-to {
    max-height: 0;
    opacity: 0;
    padding-top: 0;
    padding-bottom: 0;
}
</style>
