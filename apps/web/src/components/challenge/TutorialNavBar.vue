<script setup>
defineProps({
    step: {
        type: Number,
        required: true,
    },
    totalSteps: {
        type: Number,
        default: 4,
    },
    lastLabel: {
        type: String,
        default: '시작하기',
    },
});

defineEmits(['prev', 'next', 'skip']);
</script>

<template>
    <div class="tut-nav">
        <!-- step 0: 시작 화면 — 단일 버튼 -->
        <template v-if="step === 0">
            <button
                type="button"
                class="tut-nav__btn tut-nav__btn--ink tut-nav__btn--full"
                @click="$emit('next')"
            >
                시작 · {{ totalSteps }}단계 보기
            </button>
        </template>

        <!-- 마지막 step: 골드 버튼 -->
        <template v-else-if="step === totalSteps - 1">
            <button
                type="button"
                class="tut-nav__btn tut-nav__btn--back"
                @click="$emit('prev')"
            >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M19 12H5M12 19l-7-7 7-7" />
                </svg>
            </button>
            <button
                type="button"
                class="tut-nav__btn tut-nav__btn--gold tut-nav__btn--full"
                @click="$emit('skip')"
            >
                {{ lastLabel }}
            </button>
        </template>

        <!-- 중간 단계: 이전 + 다음 -->
        <template v-else>
            <button
                type="button"
                class="tut-nav__btn tut-nav__btn--back"
                @click="$emit('prev')"
            >
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
                    <path d="M19 12H5M12 19l-7-7 7-7" />
                </svg>
            </button>
            <button
                type="button"
                class="tut-nav__btn tut-nav__btn--ink tut-nav__btn--full"
                @click="$emit('next')"
            >
                다음
            </button>
        </template>
    </div>
</template>

<style scoped>
.tut-nav {
    flex: none;
    padding: 12px 22px 26px;
    display: flex;
    gap: 10px;
}

.tut-nav__btn {
    border: none;
    cursor: pointer;
    font-family: inherit;
    font-weight: var(--tt-fw-black);
    font-size: 15px;
    border-radius: 15px;
    transition: opacity 0.15s ease;
}

.tut-nav__btn:active {
    opacity: 0.8;
}

.tut-nav__btn--full {
    flex: 1;
    padding: 15px;
    text-align: center;
}

.tut-nav__btn--ink {
    background: var(--tt-surface-inverse);
    color: var(--tt-text-inverse);
}

.tut-nav__btn--gold {
    background: var(--tt-accent);
    color: var(--tt-surface-inverse);
    box-shadow: 0 12px 26px -12px rgba(245, 185, 33, 0.9);
}

.tut-nav__btn--back {
    width: 52px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    display: flex;
    align-items: center;
    justify-content: center;
    color: var(--tt-text-hint);
}
</style>
