<script setup>
import { computed, ref, watch } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    months: { type: Array, default: () => [] },
    selectedPeriod: { type: String, required: true },
});

const emit = defineEmits(['update:modelValue', 'select']);
const draftPeriod = ref(props.selectedPeriod);
const selectedYear = ref(Number(props.selectedPeriod.slice(0, 4)));
const availableYears = computed(() =>
    [...new Set(props.months.map((month) => month.year))].sort((a, b) => a - b),
);
const visibleMonths = computed(() =>
    Array.from({ length: 12 }, (_, index) => {
        const monthNumber = index + 1;
        const source = props.months.find(
            (month) => month.year === selectedYear.value && month.month === monthNumber,
        );
        return (
            source ?? {
                value: `${selectedYear.value}-${String(monthNumber).padStart(2, '0')}`,
                year: selectedYear.value,
                month: monthNumber,
                available: false,
                reason: 'unavailable',
            }
        );
    }),
);
const previousYear = computed(() =>
    [...availableYears.value].reverse().find((year) => year < selectedYear.value),
);
const nextYear = computed(() => availableYears.value.find((year) => year > selectedYear.value));
const draftMonth = computed(() => props.months.find((month) => month.value === draftPeriod.value));

watch(
    () => props.modelValue,
    (open) => {
        if (open) {
            draftPeriod.value = props.selectedPeriod;
            selectedYear.value = Number(props.selectedPeriod.slice(0, 4));
        }
    },
);

function selectMonth(month) {
    if (month.available) {
        draftPeriod.value = month.value;
    }
}

function moveYear(year) {
    if (year) {
        selectedYear.value = year;
    }
}

function applyMonth() {
    if (!draftMonth.value?.available) {
        return;
    }
    emit('select', draftPeriod.value);
    emit('update:modelValue', false);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title="조회할 달 선택"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <div class="month-picker__year-navigation">
            <button
                type="button"
                aria-label="이전 연도"
                :disabled="!previousYear"
                @click="moveYear(previousYear)"
            >
                ‹
            </button>
            <h3>{{ selectedYear }}년</h3>
            <button
                type="button"
                aria-label="다음 연도"
                :disabled="!nextYear"
                @click="moveYear(nextYear)"
            >
                ›
            </button>
        </div>
        <div class="month-picker__grid">
            <button
                v-for="month in visibleMonths"
                :key="month.value"
                type="button"
                :class="{ 'month-picker__active': month.value === draftPeriod }"
                :disabled="!month.available"
                :aria-pressed="month.value === draftPeriod"
                @click="selectMonth(month)"
            >
                {{ month.month }}월
            </button>
        </div>

        <template #footer>
            <div class="month-picker__action">
                <BaseButton block size="lg" :disabled="!draftMonth?.available" @click="applyMonth">
                    {{ draftMonth?.month ?? '-' }}월 리포트 보기
                </BaseButton>
            </div>
        </template>
    </BaseBottomSheet>
</template>

<style scoped>
.month-picker__year-navigation {
    display: grid;
    grid-template-columns: 40px 1fr 40px;
    align-items: center;
    margin-bottom: var(--tt-space-4);
    text-align: center;
}

.month-picker__year-navigation h3 {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}

.month-picker__year-navigation button {
    height: 40px;
    font-size: var(--tt-fs-title);
    color: var(--tt-primary);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.month-picker__year-navigation button:disabled {
    color: var(--tt-border-strong);
    cursor: default;
}

.month-picker__grid {
    display: grid;
    grid-template-columns: repeat(4, minmax(0, 1fr));
    gap: var(--tt-space-3) var(--tt-space-2);
}

.month-picker__grid button {
    min-height: 52px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    cursor: pointer;
}

.month-picker__grid .month-picker__active {
    color: var(--tt-text-inverse);
    background: var(--tt-text);
    border-color: var(--tt-text);
    box-shadow: var(--tt-elevation-2);
}

.month-picker__grid button:disabled {
    color: var(--tt-border-strong);
    background: var(--tt-bg-subtle);
    border-color: transparent;
    cursor: not-allowed;
}

.month-picker__action {
    width: 100%;
}

.month-picker__action :deep(.tt-btn) {
    color: var(--tt-text-inverse);
    background: var(--tt-text);
}

@media (max-width: 360px) {
    .month-picker__grid {
        gap: var(--tt-space-2) var(--tt-space-1);
    }
}
</style>
