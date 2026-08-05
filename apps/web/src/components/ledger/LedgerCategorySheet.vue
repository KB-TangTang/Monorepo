<!--
  용도: 거래내역 카테고리를 재분류하는 바텀시트. 지출은 대분류 아이콘 그리드 → 소분류 아코디언,
  수입은 소분류 없는 단일 그리드로 보여준다. 거래 방향(수입/지출)은 amount 부호로 정해지며
  사용자가 바꿀 수 없다(은행 데이터와 모순 방지).
  언제 쓰는지: LedgerView 한 곳에서만 렌더한다.
-->
<script setup>
import { computed, ref, watch } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import CategoryIcon from '@/components/common/CategoryIcon.vue';
import { EXPENSE_CATEGORIES, INCOME_CATEGORIES } from '@/fixtures/category';
import {
    TONES,
    chunkCategories,
    findExpenseParentByChildName,
    resolveCategoryDirection,
} from '@/utils/category';
import { formatDayLabel, formatWon } from '@/utils/ledger';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    transaction: { type: Object, default: null },
});

const emit = defineEmits(['update:modelValue', 'select']);

const expandedParentId = ref('');

const direction = computed(() =>
    props.transaction ? resolveCategoryDirection(props.transaction.amount) : 'expense',
);
const isExpense = computed(() => direction.value === 'expense');
const categories = computed(() => (isExpense.value ? EXPENSE_CATEGORIES : INCOME_CATEGORIES));
const rows = computed(() => chunkCategories(categories.value, 4));

const currentExpenseParent = computed(() =>
    props.transaction && isExpense.value
        ? findExpenseParentByChildName(props.transaction.category)
        : null,
);

const expandedParent = computed(
    () => EXPENSE_CATEGORIES.find((parent) => parent.id === expandedParentId.value) ?? null,
);

watch(
    () => props.modelValue,
    (open) => {
        if (open) {
            expandedParentId.value = currentExpenseParent.value?.id ?? '';
        }
    },
);

function toneAt(rowIndex, itemIndex) {
    return TONES[(rowIndex * 4 + itemIndex) % TONES.length];
}

function isActiveTile(item) {
    if (!props.transaction) {
        return false;
    }
    return isExpense.value
        ? currentExpenseParent.value?.id === item.id
        : item.name === props.transaction.category;
}

function toggleParent(parent) {
    expandedParentId.value = expandedParentId.value === parent.id ? '' : parent.id;
}

function handleTileClick(item) {
    if (isExpense.value) {
        toggleParent(item);
    } else {
        chooseCategory(item.name);
    }
}

function chooseExpenseChild(child) {
    chooseCategory(child.name);
}

function chooseCategory(categoryName) {
    emit('select', { transactionId: props.transaction.id, categoryName });
    emit('update:modelValue', false);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title="카테고리 선택"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <div v-if="transaction" class="category-sheet__summary">
            <p class="category-sheet__merchant">{{ transaction.merchant }}</p>
            <p class="category-sheet__meta">
                {{ formatDayLabel(transaction.date) }} · {{ formatWon(transaction.amount) }}
            </p>
            <p class="category-sheet__direction">
                {{ isExpense ? '지출 카테고리' : '수입 카테고리' }}
            </p>
        </div>

        <div class="category-sheet__grid">
            <template v-for="(row, rowIndex) in rows" :key="rowIndex">
                <div class="category-sheet__row">
                    <button
                        v-for="(item, itemIndex) in row"
                        :key="item.id"
                        type="button"
                        class="category-sheet__tile"
                        :class="[
                            `category-sheet__tile--${toneAt(rowIndex, itemIndex)}`,
                            { 'category-sheet__tile--active': isActiveTile(item) },
                        ]"
                        @click="handleTileClick(item)"
                    >
                        <CategoryIcon :icon="item.icon" />
                        <span>{{ item.name }}</span>
                    </button>
                </div>

                <div
                    v-if="isExpense && row.some((item) => item.id === expandedParentId)"
                    class="category-sheet__children"
                >
                    <button
                        v-for="child in expandedParent?.children ?? []"
                        :key="child.id"
                        type="button"
                        class="category-sheet__chip"
                        :class="{
                            'category-sheet__chip--active': child.name === transaction?.category,
                        }"
                        @click="chooseExpenseChild(child)"
                    >
                        {{ child.name }}
                    </button>
                </div>
            </template>
        </div>
    </BaseBottomSheet>
</template>

<style scoped>
.category-sheet__summary {
    padding-bottom: var(--tt-space-4);
    margin-bottom: var(--tt-space-4);
    border-bottom: 1px solid var(--tt-border);
}

.category-sheet__merchant {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.category-sheet__meta {
    margin-top: 2px;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.category-sheet__direction {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.category-sheet__grid {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.category-sheet__row {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: var(--tt-space-2);
}

.category-sheet__tile {
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

.category-sheet__tile span {
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.category-sheet__tile--active {
    border-color: var(--tt-primary);
    border-width: 2px;
}

.category-sheet__tile--primary {
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
}

.category-sheet__tile--accent {
    color: var(--tt-accent-strong);
    background: var(--tt-accent-subtle);
}

.category-sheet__tile--success {
    color: var(--tt-success);
    background: var(--tt-success-subtle);
}

.category-sheet__tile--muted {
    color: var(--tt-text-muted);
    background: var(--tt-gray-100);
}

.category-sheet__children {
    display: flex;
    flex-wrap: wrap;
    gap: var(--tt-space-2);
    padding: var(--tt-space-1) 0 var(--tt-space-3);
}

.category-sheet__chip {
    padding: var(--tt-space-2) var(--tt-space-3);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    cursor: pointer;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.category-sheet__chip--active {
    color: var(--tt-text-inverse);
    background: var(--tt-text);
    border-color: var(--tt-text);
}
</style>
