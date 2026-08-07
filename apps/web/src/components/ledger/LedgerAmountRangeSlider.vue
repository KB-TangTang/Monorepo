<!--
  용도: 최소~최대 금액을 두 개의 손잡이로 고르는 듀얼 범위 슬라이더.
  언제 쓰는지: LedgerSearchView 의 "금액 조건" 한 곳에서만 렌더한다.
-->
<script setup>
import { computed } from 'vue';
import { formatWon } from '@/utils/ledger';

const props = defineProps({
    min: { type: Number, required: true },
    max: { type: Number, required: true },
    modelMin: { type: Number, required: true },
    modelMax: { type: Number, required: true },
    step: { type: Number, default: 1000 },
});

const emit = defineEmits(['update:modelMin', 'update:modelMax']);

const range = computed(() => Math.max(props.max - props.min, 1));
const minPercent = computed(() => ((props.modelMin - props.min) / range.value) * 100);
const maxPercent = computed(() => ((props.modelMax - props.min) / range.value) * 100);

function onMinInput(event) {
    const value = Math.min(Number(event.target.value), props.modelMax);
    emit('update:modelMin', value);
}

function onMaxInput(event) {
    const value = Math.max(Number(event.target.value), props.modelMin);
    emit('update:modelMax', value);
}
</script>

<template>
    <div class="amount-range">
        <p class="amount-range__value">{{ formatWon(modelMin) }} ~ {{ formatWon(modelMax) }}</p>

        <div class="amount-range__track-wrap">
            <div class="amount-range__track" aria-hidden="true"></div>
            <div
                class="amount-range__fill"
                aria-hidden="true"
                :style="{ left: `${minPercent}%`, right: `${100 - maxPercent}%` }"
            ></div>
            <input
                class="amount-range__input"
                type="range"
                :min="min"
                :max="max"
                :step="step"
                :value="modelMin"
                aria-label="최소 금액"
                @input="onMinInput"
            />
            <input
                class="amount-range__input"
                type="range"
                :min="min"
                :max="max"
                :step="step"
                :value="modelMax"
                aria-label="최대 금액"
                @input="onMaxInput"
            />
        </div>

        <div class="amount-range__bounds">
            <span>{{ formatWon(min) }}</span>
            <span>{{ formatWon(max) }}</span>
        </div>
    </div>
</template>

<style scoped>
.amount-range {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.amount-range__value {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
}

.amount-range__track-wrap {
    position: relative;
    height: 20px;
}

.amount-range__track {
    position: absolute;
    top: 50%;
    right: 0;
    left: 0;
    height: 4px;
    background: var(--tt-border);
    border-radius: var(--tt-radius-full);
    transform: translateY(-50%);
}

.amount-range__fill {
    position: absolute;
    top: 50%;
    height: 4px;
    background: var(--tt-primary);
    border-radius: var(--tt-radius-full);
    transform: translateY(-50%);
}

.amount-range__input {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 20px;
    margin: 0;
    -webkit-appearance: none;
    appearance: none;
    pointer-events: none;
    background: transparent;
}

.amount-range__input::-webkit-slider-runnable-track {
    height: 4px;
    background: transparent;
}

.amount-range__input::-moz-range-track {
    height: 4px;
    background: transparent;
}

.amount-range__input::-webkit-slider-thumb {
    width: 20px;
    height: 20px;
    margin-top: -8px;
    pointer-events: auto;
    cursor: pointer;
    background: var(--tt-bg);
    border: 2px solid var(--tt-primary);
    border-radius: var(--tt-radius-full);
    box-shadow: var(--tt-elevation-1);
    -webkit-appearance: none;
    appearance: none;
}

.amount-range__input::-moz-range-thumb {
    box-sizing: border-box;
    width: 20px;
    height: 20px;
    pointer-events: auto;
    cursor: pointer;
    background: var(--tt-bg);
    border: 2px solid var(--tt-primary);
    border-radius: var(--tt-radius-full);
    box-shadow: var(--tt-elevation-1);
}

.amount-range__bounds {
    display: flex;
    justify-content: space-between;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}
</style>
