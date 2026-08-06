<!--
  용도: 카테고리로 거래내역을 좁히는 바텀시트. "전체 카테고리" + 이 화면에 실제로 등장한
  카테고리만 보여준다 (LedgerPaymentMethodSheet 와 동일한 패턴).
  언제 쓰는지: LedgerMonthTransactionsView 한 곳에서만 렌더한다.
-->
<script setup>
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';

defineProps({
    modelValue: { type: Boolean, required: true },
    categories: { type: Array, default: () => [] },
    selected: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue', 'select']);

function choose(category) {
    emit('select', category);
    emit('update:modelValue', false);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title="카테고리 선택"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <ul class="category-filter-sheet__list">
            <li>
                <button
                    type="button"
                    :class="{ 'category-filter-sheet__active': selected === '' }"
                    @click="choose('')"
                >
                    전체 카테고리
                </button>
            </li>
            <li v-for="category in categories" :key="category">
                <button
                    type="button"
                    :class="{ 'category-filter-sheet__active': selected === category }"
                    @click="choose(category)"
                >
                    {{ category }}
                </button>
            </li>
        </ul>
    </BaseBottomSheet>
</template>

<style scoped>
.category-filter-sheet__list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.category-filter-sheet__list button {
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

.category-filter-sheet__active {
    color: var(--tt-text-inverse);
    background: var(--tt-text);
    border-color: var(--tt-text);
}
</style>
