<script setup>
import { ref, computed, watch } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import { MOCK_PROSECUTORS } from '@/fixtures/personalChallenge';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    currentProsecutorId: { type: String, default: 'NORMAL' },
});

const emit = defineEmits(['update:modelValue', 'select', 'confirm']);

const selectedId = ref(props.currentProsecutorId);

watch(
    () => [props.modelValue, props.currentProsecutorId],
    ([isOpen, currentProsecutorId]) => {
        if (isOpen) {
            selectedId.value = currentProsecutorId;
        }
    },
);

const selectedProsecutor = computed(() => MOCK_PROSECUTORS.find((p) => p.id === selectedId.value));

function select(id) {
    selectedId.value = id;
    emit('select', id);
}

function confirm() {
    emit('confirm', selectedId.value);
    emit('update:modelValue', false);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title=""
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <div class="tangi-sheet">
            <div class="tangi-sheet__header">
                <h2 class="tangi-sheet__title">누구에게 수사를 맡길까요?</h2>
                <p class="tangi-sheet__subtitle">모든 사건에 적용돼요 · 깐깐할수록 점수가 커요</p>
            </div>

            <div class="tangi-sheet__list">
                <button
                    v-for="p in MOCK_PROSECUTORS"
                    :key="p.id"
                    type="button"
                    class="tangi-sheet__card"
                    :class="{
                        'tangi-sheet__card--selected': selectedId === p.id,
                    }"
                    @click="select(p.id)"
                >
                    <span v-if="p.recommended" class="tangi-sheet__recommend"> 탕이 추천 </span>
                    <div class="tangi-sheet__card-inner">
                        <div class="tangi-sheet__avatar" :style="{ background: p.badgeBg }">
                            <img :src="p.image" :alt="p.name" />
                        </div>
                        <div class="tangi-sheet__info">
                            <div class="tangi-sheet__name-row">
                                <span class="tangi-sheet__name">{{ p.name }}</span>
                                <span
                                    class="tangi-sheet__points"
                                    :style="{ background: p.badgeBg, color: p.badgeColor }"
                                >
                                    +{{ p.bonusPoints }}점
                                </span>
                            </div>
                            <div class="tangi-sheet__desc">
                                {{ p.quote }}
                                <template v-if="p.description"> · {{ p.description }}</template>
                            </div>
                            <div class="tangi-sheet__reduction" :style="{ color: p.badgeColor }">
                                목표 절감률 {{ p.targetReductionRange }}
                            </div>
                        </div>
                        <div
                            class="tangi-sheet__radio"
                            :class="{ 'tangi-sheet__radio--checked': selectedId === p.id }"
                        >
                            <svg
                                v-if="selectedId === p.id"
                                width="12"
                                height="12"
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="#fff"
                                stroke-width="3.4"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                            >
                                <path d="M5 12.5l4.5 4.5L19 7" />
                            </svg>
                        </div>
                    </div>
                </button>
            </div>

            <div class="tangi-sheet__info-box">
                <span class="tangi-sheet__info-icon">i</span>
                <span class="tangi-sheet__info-text">
                    담당 탕이는 카테고리와 상관없이 <b>모든 사건에 적용</b>돼요. 내일 배정분부터
                    반영됩니다.
                </span>
            </div>
        </div>

        <template #footer>
            <button type="button" class="tangi-sheet__confirm" @click="confirm">
                {{ selectedProsecutor?.name }}에게 맡기기
            </button>
        </template>
    </BaseBottomSheet>
</template>

<style scoped>
.tangi-sheet__header {
    text-align: center;
    margin-bottom: 14px;
}

.tangi-sheet__title {
    font-size: 20px;
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
}

.tangi-sheet__subtitle {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    margin-top: var(--tt-space-2);
    font-weight: var(--tt-fw-medium);
}

.tangi-sheet__list {
    display: flex;
    flex-direction: column;
    gap: 9px;
}

.tangi-sheet__card {
    position: relative;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    padding: 13px 14px;
    box-shadow: var(--tt-elevation-1);
    cursor: pointer;
    text-align: left;
    font-family: var(--tt-font-sans);
    width: 100%;
    outline: none;
    transition:
        transform 100ms ease,
        background-color 100ms ease,
        box-shadow 100ms ease;
}

.tangi-sheet__card:focus,
.tangi-sheet__card:focus-visible {
    outline: none;
}

.tangi-sheet__card:active {
    background: var(--tt-bg-subtle);
    box-shadow: none;
    transform: translateY(1px) scale(0.99);
}

.tangi-sheet__card--selected {
    border-color: var(--tt-border);
}

.tangi-sheet__recommend {
    position: absolute;
    top: -9px;
    left: 14px;
    background: var(--tt-surface-inverse);
    color: var(--tt-accent);
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.tangi-sheet__card-inner {
    display: flex;
    align-items: center;
    gap: 11px;
}

.tangi-sheet__avatar {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.tangi-sheet__avatar img {
    width: 44px;
    height: 44px;
    object-fit: contain;
}

.tangi-sheet__info {
    flex: 1;
    min-width: 0;
}

.tangi-sheet__name-row {
    display: flex;
    align-items: center;
    gap: 7px;
}

.tangi-sheet__name {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
}

.tangi-sheet__points {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.tangi-sheet__desc {
    font-size: 11.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
    margin-top: 4px;
}

.tangi-sheet__reduction {
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    margin-top: 4px;
    font-family: var(--tt-font-mono);
}

.tangi-sheet__radio {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    border: 2px solid var(--tt-border-divider);
    flex: none;
    display: flex;
    align-items: center;
    justify-content: center;
}

.tangi-sheet__radio--checked {
    background: var(--tt-surface-inverse);
    border-color: var(--tt-surface-inverse);
}

.tangi-sheet__info-box {
    margin-top: 11px;
    background: var(--tt-info-subtle);
    border: 1px solid var(--tt-info-subtle-border);
    border-radius: var(--tt-radius-md);
    padding: 11px 13px;
    display: flex;
    gap: 9px;
    align-items: flex-start;
}

.tangi-sheet__info-icon {
    width: 17px;
    height: 17px;
    border-radius: 50%;
    border: 1.5px solid var(--tt-info);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    margin-top: 1px;
    font-size: 10px;
    font-weight: 900;
    color: var(--tt-info);
}

.tangi-sheet__info-text {
    font-size: 11.5px;
    color: var(--tt-info-deep);
    line-height: 1.55;
    font-weight: var(--tt-fw-semibold);
}

.tangi-sheet__confirm {
    flex: 1;
    background: var(--tt-surface-inverse);
    color: var(--tt-text-inverse);
    font-weight: var(--tt-fw-black);
    font-size: var(--tt-fs-label);
    padding: 15px;
    border-radius: var(--tt-radius-md);
    border: none;
    cursor: pointer;
    box-shadow: var(--tt-elevation-4);
    font-family: var(--tt-font-sans);
}
</style>
