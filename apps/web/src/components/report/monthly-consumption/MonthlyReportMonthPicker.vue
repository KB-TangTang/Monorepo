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
const visibleMonths = computed(() =>
    Array.from({ length: 12 }, (_, index) => {
        const month = index + 1;
        return (
            props.months.find(
                (item) => item.year === selectedYear.value && item.month === month,
            ) ?? {
                value: `${selectedYear.value}-${String(month).padStart(2, '0')}`,
                month,
                available: false,
            }
        );
    }),
);
const draftMonth = computed(() => props.months.find((month) => month.value === draftPeriod.value));

watch(
    () => props.modelValue,
    (isOpen) => {
        if (isOpen) {
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

function applyMonth() {
    if (draftMonth.value?.available) {
        emit('select', draftPeriod.value);
        emit('update:modelValue', false);
    }
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title="조회할 달 선택"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <h3 class="month-picker__year">{{ selectedYear }}년</h3>
        <div class="month-picker__grid">
            <button
                v-for="month in visibleMonths"
                :key="month.value"
                type="button"
                :class="{ 'month-picker__active': month.value === draftPeriod }"
                :disabled="!month.available"
                @click="selectMonth(month)"
            >
                {{ month.month }}월
            </button>
        </div>
        <template #footer>
            <BaseButton block size="lg" :disabled="!draftMonth?.available" @click="applyMonth">
                {{ draftMonth?.month ?? '-' }}월 리포트 보기
            </BaseButton>
        </template>
    </BaseBottomSheet>
</template>

<style scoped>
.month-picker__year {
    margin-bottom: var(--tt-space-4);
    text-align: center;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}
.month-picker__grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
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
</style>
