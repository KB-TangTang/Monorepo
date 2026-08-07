<!--
  용도: 카테고리로 거래내역을 좁히는 바텀시트. LedgerCategorySheet(단일 거래내역 카테고리 선택)와
  동일한 대분류 아이콘 그리드를 그대로 쓴다. "전체 카테고리"는 그리드와 분리해 맨 위 한 줄로 둔다.
  언제 쓰는지: LedgerMonthTransactionsView 한 곳에서만 렌더한다.
-->
<script setup>
import { computed } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import CategoryIcon from '@/components/common/CategoryIcon.vue';
import { EXPENSE_CATEGORIES, INCOME_CATEGORIES } from '@/fixtures/category';
import { TONES, chunkCategories } from '@/utils/category';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    isIncome: { type: Boolean, default: false },
    selected: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue', 'select']);

const categories = computed(() => (props.isIncome ? INCOME_CATEGORIES : EXPENSE_CATEGORIES));
const rows = computed(() => chunkCategories(categories.value, 4));

function toneAt(rowIndex, itemIndex) {
    return TONES[(rowIndex * 4 + itemIndex) % TONES.length];
}

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
        <button
            type="button"
            class="category-filter-sheet__all"
            :class="{ 'category-filter-sheet__all--active': selected === '' }"
            @click="choose('')"
        >
            전체 카테고리
        </button>

        <div class="category-filter-sheet__grid">
            <div v-for="(row, rowIndex) in rows" :key="rowIndex" class="category-filter-sheet__row">
                <button
                    v-for="(item, itemIndex) in row"
                    :key="item.id"
                    type="button"
                    class="category-filter-sheet__tile"
                    :class="[
                        `category-filter-sheet__tile--${toneAt(rowIndex, itemIndex)}`,
                        { 'category-filter-sheet__tile--active': selected === item.name },
                    ]"
                    :aria-pressed="selected === item.name"
                    @click="choose(item.name)"
                >
                    <CategoryIcon :icon="item.icon" />
                    <span>{{ item.name }}</span>
                </button>
            </div>
        </div>
    </BaseBottomSheet>
</template>

<style scoped>
.category-filter-sheet__all {
    width: 100%;
    padding: var(--tt-space-3) var(--tt-space-4);
    margin-bottom: var(--tt-space-3);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    text-align: center;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    cursor: pointer;
}

.category-filter-sheet__all--active {
    color: var(--tt-primary);
    border-color: var(--tt-primary);
    border-width: 2px;
}

.category-filter-sheet__grid {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.category-filter-sheet__row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--tt-space-2);
}

.category-filter-sheet__tile {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--tt-space-1);
    padding: var(--tt-space-3) var(--tt-space-1);
    text-align: center;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    cursor: pointer;
}

.category-filter-sheet__tile span {
    max-width: 100%;
    overflow: hidden;
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    white-space: nowrap;
    text-overflow: ellipsis;
}

.category-filter-sheet__tile--active {
    border-color: var(--tt-primary);
    border-width: 2px;
}

.category-filter-sheet__tile--primary {
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
}

.category-filter-sheet__tile--accent {
    color: var(--tt-accent-strong);
    background: var(--tt-accent-subtle);
}

.category-filter-sheet__tile--success {
    color: var(--tt-success);
    background: var(--tt-success-subtle);
}

.category-filter-sheet__tile--muted {
    color: var(--tt-text-muted);
    background: var(--tt-gray-100);
}
</style>
