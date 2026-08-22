<script setup>
import { computed, ref, watch } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import { MOCK_PROSECUTORS } from '@/fixtures/personalChallenge';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    currentProsecutorId: { type: String, default: 'NORMAL' },
    loading: { type: Boolean, default: false },
    errorMessage: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue', 'confirm']);

const selectedId = ref(props.currentProsecutorId);

watch(
    () => [props.modelValue, props.currentProsecutorId],
    ([isOpen, currentProsecutorId]) => {
        if (isOpen) {
            selectedId.value = currentProsecutorId;
        }
    },
);

const selectedProsecutor = computed(() =>
    MOCK_PROSECUTORS.find((prosecutor) => prosecutor.id === selectedId.value),
);

function select(id) {
    if (!props.loading) {
        selectedId.value = id;
    }
}

function confirm() {
    if (!props.loading) {
        emit('confirm', selectedId.value);
    }
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title="난이도 변경"
        :close-on-overlay="!loading"
        :close-on-esc="!loading"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <template #header>
            <div class="challenge-difficulty-sheet__header">
                <h2 class="challenge-difficulty-sheet__title">누구에게 수사를 맡길까요?</h2>
                <p class="challenge-difficulty-sheet__subtitle">
                    모든 사건에 적용돼요 · 깐깐할수록 점수가 커요
                </p>
            </div>
        </template>

        <div class="challenge-difficulty-sheet">
            <div class="challenge-difficulty-sheet__list">
                <button
                    v-for="prosecutor in MOCK_PROSECUTORS"
                    :key="prosecutor.id"
                    type="button"
                    class="challenge-difficulty-sheet__card"
                    :class="{
                        'challenge-difficulty-sheet__card--selected': selectedId === prosecutor.id,
                    }"
                    :disabled="loading"
                    @click="select(prosecutor.id)"
                >
                    <span
                        v-if="prosecutor.recommended"
                        class="challenge-difficulty-sheet__recommend"
                    >
                        탕이 추천
                    </span>
                    <div class="challenge-difficulty-sheet__card-inner">
                        <div
                            class="challenge-difficulty-sheet__avatar"
                            :style="{ background: prosecutor.badgeBg }"
                        >
                            <img :src="prosecutor.image" :alt="prosecutor.name" />
                        </div>
                        <div class="challenge-difficulty-sheet__info">
                            <div class="challenge-difficulty-sheet__name-row">
                                <span class="challenge-difficulty-sheet__name">{{
                                    prosecutor.name
                                }}</span>
                                <span
                                    class="challenge-difficulty-sheet__points"
                                    :style="{
                                        background: prosecutor.badgeBg,
                                        color: prosecutor.badgeColor,
                                    }"
                                >
                                    +{{ prosecutor.bonusPoints }}점
                                </span>
                            </div>
                            <div class="challenge-difficulty-sheet__desc">
                                {{ prosecutor.quote }}
                                <template v-if="prosecutor.description">
                                    · {{ prosecutor.description }}
                                </template>
                            </div>
                            <div
                                class="challenge-difficulty-sheet__reduction"
                                :style="{ color: prosecutor.badgeColor }"
                            >
                                목표 절감률 {{ prosecutor.targetReductionRange }}
                            </div>
                        </div>
                        <div
                            class="challenge-difficulty-sheet__radio"
                            :class="{
                                'challenge-difficulty-sheet__radio--checked':
                                    selectedId === prosecutor.id,
                            }"
                        >
                            <svg
                                v-if="selectedId === prosecutor.id"
                                width="12"
                                height="12"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                stroke-width="3.4"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                                aria-hidden="true"
                            >
                                <path d="M5 12.5l4.5 4.5L19 7" />
                            </svg>
                        </div>
                    </div>
                </button>
            </div>

            <p v-if="errorMessage" class="challenge-difficulty-sheet__error" role="alert">
                {{ errorMessage }}
            </p>

            <div class="challenge-difficulty-sheet__info-box">
                <span class="challenge-difficulty-sheet__info-icon">i</span>
                <span class="challenge-difficulty-sheet__info-text">
                    담당 탕이는 카테고리와 상관없이 <b>모든 사건에 적용</b>돼요. 내일 배정분부터
                    반영됩니다.
                </span>
            </div>
        </div>

        <template #footer>
            <button
                type="button"
                class="challenge-difficulty-sheet__confirm"
                :disabled="loading"
                :aria-busy="loading"
                @click="confirm"
            >
                {{ loading ? '저장 중...' : `${selectedProsecutor?.name}에게 맡기기` }}
            </button>
        </template>
    </BaseBottomSheet>
</template>

<style scoped>
.challenge-difficulty-sheet__header {
    text-align: center;
}

.challenge-difficulty-sheet__title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
}

.challenge-difficulty-sheet__subtitle {
    margin-top: var(--tt-space-2);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
}

.challenge-difficulty-sheet__list {
    display: flex;
    flex-direction: column;
    gap: 9px;
}

.challenge-difficulty-sheet__card {
    position: relative;
    width: 100%;
    padding: 13px 14px;
    font-family: var(--tt-font-sans);
    text-align: left;
    cursor: pointer;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    outline: none;
    box-shadow: var(--tt-elevation-1);
    transition:
        transform 100ms ease,
        background-color 100ms ease,
        box-shadow 100ms ease;
}

.challenge-difficulty-sheet__card:focus,
.challenge-difficulty-sheet__card:focus-visible {
    outline: none;
}

.challenge-difficulty-sheet__card:active:not(:disabled) {
    background: var(--tt-bg-subtle);
    box-shadow: none;
    transform: translateY(1px) scale(0.99);
}

.challenge-difficulty-sheet__card:disabled,
.challenge-difficulty-sheet__confirm:disabled {
    cursor: wait;
    opacity: 0.72;
}

.challenge-difficulty-sheet__card--selected {
    border-color: var(--tt-border);
}

.challenge-difficulty-sheet__recommend {
    position: absolute;
    top: -9px;
    left: 14px;
    padding: 3px 9px;
    color: var(--tt-accent);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    background: var(--tt-surface-inverse);
    border-radius: var(--tt-radius-full);
}

.challenge-difficulty-sheet__card-inner {
    display: flex;
    align-items: center;
    gap: 11px;
}

.challenge-difficulty-sheet__avatar {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: center;
    width: 52px;
    height: 52px;
    border-radius: var(--tt-radius-full);
}

.challenge-difficulty-sheet__avatar img {
    width: 44px;
    height: 44px;
    object-fit: contain;
}

.challenge-difficulty-sheet__info {
    flex: 1;
    min-width: 0;
}

.challenge-difficulty-sheet__name-row {
    display: flex;
    align-items: center;
    gap: 7px;
}

.challenge-difficulty-sheet__name {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
}

.challenge-difficulty-sheet__points {
    padding: 3px 9px;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    border-radius: var(--tt-radius-full);
}

.challenge-difficulty-sheet__desc {
    margin-top: 4px;
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
}

.challenge-difficulty-sheet__reduction {
    margin-top: 4px;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
}

.challenge-difficulty-sheet__radio {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: center;
    width: 22px;
    height: 22px;
    color: var(--tt-text-inverse);
    border: 2px solid var(--tt-border-divider);
    border-radius: var(--tt-radius-full);
}

.challenge-difficulty-sheet__radio--checked {
    background: var(--tt-surface-inverse);
    border-color: var(--tt-surface-inverse);
}

.challenge-difficulty-sheet__error {
    margin-top: var(--tt-space-3);
    color: var(--tt-danger);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    text-align: center;
}

.challenge-difficulty-sheet__info-box {
    display: flex;
    gap: 9px;
    align-items: flex-start;
    margin-top: 11px;
    padding: 11px 13px;
    background: var(--tt-info-subtle);
    border: 1px solid var(--tt-info-subtle-border);
    border-radius: var(--tt-radius-md);
}

.challenge-difficulty-sheet__info-icon {
    display: flex;
    flex: none;
    align-items: center;
    justify-content: center;
    width: 17px;
    height: 17px;
    margin-top: 1px;
    color: var(--tt-info);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    border: 1px solid var(--tt-info);
    border-radius: var(--tt-radius-full);
}

.challenge-difficulty-sheet__info-text {
    color: var(--tt-info-deep);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-semibold);
    line-height: var(--tt-lh-normal);
}

.challenge-difficulty-sheet__confirm {
    flex: 1;
    padding: 15px;
    color: var(--tt-text-inverse);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    cursor: pointer;
    background: var(--tt-surface-inverse);
    border: 0;
    border-radius: var(--tt-radius-md);
    box-shadow: var(--tt-elevation-4);
}
</style>
