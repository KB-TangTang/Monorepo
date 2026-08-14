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
        const monthNumber = index + 1;
        return (
            props.months.find(
                (month) => month.year === selectedYear.value && month.month === monthNumber,
            ) ?? {
                value: `${selectedYear.value}-${String(monthNumber).padStart(2, '0')}`,
                year: selectedYear.value,
                month: monthNumber,
                available: false,
            }
        );
    }),
);

const previousYear = computed(() => selectedYear.value - 1);
const nextYear = computed(() => selectedYear.value + 1);
const draftMonth = computed(() => {
    const month = props.months.find((item) => item.value === draftPeriod.value);
    return month?.year === selectedYear.value ? month : null;
});

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
        @update:model-value="emit('update:modelValue', $event)"
    >
        <div class="ranking-month-picker__year">
            <button
                type="button"
                aria-label="이전 연도"
                @click="moveYear(previousYear)"
            >
                ‹
            </button>
            <h3>{{ selectedYear }}년</h3>
            <button
                type="button"
                aria-label="다음 연도"
                @click="moveYear(nextYear)"
            >
                ›
            </button>
        </div>

        <div class="ranking-month-picker__grid">
            <button
                v-for="month in visibleMonths"
                :key="month.value"
                type="button"
                :class="{ 'ranking-month-picker__active': month.value === draftPeriod }"
                :disabled="!month.available"
                :aria-pressed="month.value === draftPeriod"
                @click="selectMonth(month)"
            >
                {{ month.month }}월
            </button>
        </div>

        <template #footer>
            <div class="ranking-month-picker__action">
                <BaseButton block size="lg" :disabled="!draftMonth?.available" @click="applyMonth">
                    {{ draftMonth?.month ?? '-' }}월 랭킹 보기
                </BaseButton>
            </div>
        </template>
    </BaseBottomSheet>
</template>

<style scoped src="./PersonalRankingMonthPicker.css"></style>
