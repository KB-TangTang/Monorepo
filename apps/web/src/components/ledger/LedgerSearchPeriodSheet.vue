<!--
  용도: 검색 기간(시작 월 ~ 종료 월)을 고르는 바텀시트. 데이터가 있는 월만 고를 수 있다.
  상단의 "시작월/종료월" 탭으로 지금 무엇을 고르는 중인지 명확히 하고, 그 아래 월 목록 하나를
  같이 쓴다. 시작월을 고르면 자동으로 종료월 탭으로 넘어간다. 종료월 탭에서는 시작월보다
  이른 월을 고를 수 없다 — 목록에서 아예 비활성 처리한다.
  언제 쓰는지: LedgerSearchView 한 곳에서만 렌더한다.
-->
<script setup>
import { computed, ref, watch } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import { formatMonthLabel } from '@/utils/ledger';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    months: { type: Array, default: () => [] }, // [{ value, hasData }]
    from: { type: String, default: '' },
    to: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue', 'select']);

const draftFrom = ref(props.from);
const draftTo = ref(props.to);
const activeField = ref('from');

watch(
    () => props.modelValue,
    (open) => {
        if (open) {
            draftFrom.value = props.from;
            draftTo.value = props.to;
            activeField.value = 'from';
        }
    },
);

const availableMonths = computed(() =>
    props.months
        .filter((month) => month.hasData)
        .map((month) => month.value)
        .sort(),
);

function fieldLabel(value, placeholder) {
    return value ? formatMonthLabel(value) : placeholder;
}

function isDisabled(value) {
    return activeField.value === 'to' && draftFrom.value !== '' && value < draftFrom.value;
}

function rowState(value) {
    if (value === draftFrom.value) {
        return 'start';
    }
    if (value === draftTo.value) {
        return 'end';
    }
    if (draftFrom.value && draftTo.value && value > draftFrom.value && value < draftTo.value) {
        return 'in-range';
    }
    return '';
}

function pick(value) {
    if (isDisabled(value)) {
        return;
    }
    if (activeField.value === 'from') {
        draftFrom.value = value;
        if (draftTo.value && draftTo.value < value) {
            draftTo.value = value;
        }
        activeField.value = 'to';
    } else {
        draftTo.value = value;
    }
}

function apply() {
    emit('select', { from: draftFrom.value, to: draftTo.value || draftFrom.value });
    emit('update:modelValue', false);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title="기간 선택"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <div class="period-sheet__fields" role="tablist" aria-label="시작월 · 종료월 전환">
            <button
                type="button"
                role="tab"
                class="period-sheet__field"
                :class="{ 'period-sheet__field--active': activeField === 'from' }"
                :aria-selected="activeField === 'from'"
                @click="activeField = 'from'"
            >
                <span class="period-sheet__field-label">시작월</span>
                <span class="period-sheet__field-value">{{ fieldLabel(draftFrom, '선택') }}</span>
            </button>
            <button
                type="button"
                role="tab"
                class="period-sheet__field"
                :class="{ 'period-sheet__field--active': activeField === 'to' }"
                :aria-selected="activeField === 'to'"
                @click="activeField = 'to'"
            >
                <span class="period-sheet__field-label">종료월</span>
                <span class="period-sheet__field-value">{{ fieldLabel(draftTo, '선택') }}</span>
            </button>
        </div>

        <ul class="period-sheet__list">
            <li v-for="value in availableMonths" :key="value">
                <button
                    type="button"
                    class="period-sheet__month"
                    :class="[
                        rowState(value) && `period-sheet__month--${rowState(value)}`,
                        { 'period-sheet__month--disabled': isDisabled(value) },
                    ]"
                    :aria-pressed="rowState(value) === 'start' || rowState(value) === 'end'"
                    :disabled="isDisabled(value)"
                    @click="pick(value)"
                >
                    {{ formatMonthLabel(value) }}
                </button>
            </li>
        </ul>

        <template #footer>
            <BaseButton block :disabled="!draftFrom" @click="apply">적용</BaseButton>
        </template>
    </BaseBottomSheet>
</template>

<style scoped>
.period-sheet__fields {
    display: flex;
    gap: var(--tt-space-2);
    margin-bottom: var(--tt-space-4);
}

.period-sheet__field {
    flex: 1;
    padding: var(--tt-space-2) var(--tt-space-3);
    text-align: left;
    cursor: pointer;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
}

.period-sheet__field--active {
    background: var(--tt-primary-subtle);
    border-color: var(--tt-primary);
}

.period-sheet__field-label {
    display: block;
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.period-sheet__field--active .period-sheet__field-label {
    color: var(--tt-primary);
}

.period-sheet__field-value {
    display: block;
    margin-top: 2px;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.period-sheet__list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.period-sheet__month {
    width: 100%;
    padding: var(--tt-space-3) var(--tt-space-4);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    text-align: left;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    cursor: pointer;
}

.period-sheet__month--in-range {
    background: var(--tt-primary-subtle);
    border-color: var(--tt-primary-subtle);
}

.period-sheet__month--start,
.period-sheet__month--end {
    color: var(--tt-text-inverse);
    background: var(--tt-primary);
    border-color: var(--tt-primary);
}

.period-sheet__month--disabled {
    color: var(--tt-border-strong);
    cursor: not-allowed;
    background: var(--tt-bg-subtle);
}
</style>
