<!--
  용도: 거래내역 카테고리를 재분류하는 바텀시트. 지출은 대분류 아이콘 그리드 → 소분류 아코디언,
  수입은 소분류 없는 단일 그리드로 보여준다. 거래 방향(수입/지출)은 amount 부호로 정해지며
  사용자가 바꿀 수 없다(은행 데이터와 모순 방지).
  언제 쓰는지: LedgerView 한 곳에서만 렌더한다.
-->
<script setup>
import { computed, ref, watch } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import CategoryIcon from '@/components/common/CategoryIcon.vue';
import { EXPENSE_CATEGORIES, INCOME_CATEGORIES } from '@/fixtures/category';
import {
    TONES,
    chunkCategories,
    findExpenseParentByName,
    resolveCategoryDirection,
} from '@/utils/category';
import { formatDayLabel, formatWon } from '@/utils/ledger';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    transaction: { type: Object, default: null },
});

const emit = defineEmits(['update:modelValue', 'select']);

const expandedParentId = ref('');
const applyToMerchant = ref(false);
/* 탭으로 고른(아직 확정 전) 카테고리. '변경하기'를 눌러야 select 이벤트로 나간다. */
const pendingCategoryName = ref('');

const direction = computed(() =>
    props.transaction ? resolveCategoryDirection(props.transaction.amount) : 'expense',
);
const isExpense = computed(() => direction.value === 'expense');
const categories = computed(() => (isExpense.value ? EXPENSE_CATEGORIES : INCOME_CATEGORIES));
const rows = computed(() => chunkCategories(categories.value, 4));

const currentExpenseParent = computed(() =>
    props.transaction && isExpense.value
        ? findExpenseParentByName(props.transaction.category)
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
            pendingCategoryName.value = props.transaction?.category ?? '';
            applyToMerchant.value = false;
        }
    },
);

function toneAt(rowIndex, itemIndex) {
    return TONES[(rowIndex * 4 + itemIndex) % TONES.length];
}

/* 지출은 지금 펼쳐진 대분류를, 수입은 지금 고른 항목을 파란 테두리로 보여준다.
 * 탭할 때마다 이 값이 바뀌므로, 마지막에 확정된 값이 아니라 '지금 누르고 있는 곳'을 따라간다. */
function isActiveTile(item) {
    return isExpense.value
        ? expandedParentId.value === item.id
        : pendingCategoryName.value === item.name;
}

function toggleParent(parent) {
    expandedParentId.value = expandedParentId.value === parent.id ? '' : parent.id;
}

function handleTileClick(item) {
    pendingCategoryName.value = item.name;
    if (isExpense.value) {
        toggleParent(item);
    }
}

function chooseExpenseChild(child) {
    pendingCategoryName.value = child.name;
}

function closeSheet() {
    emit('update:modelValue', false);
}

function confirmSelection() {
    if (!props.transaction) {
        return;
    }
    emit('select', {
        transactionId: props.transaction.id,
        categoryName: pendingCategoryName.value,
        applyToMerchant: applyToMerchant.value,
    });
    emit('update:modelValue', false);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title="카테고리 선택"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <template #header>
            <div class="category-sheet__header">
                <h2 class="category-sheet__title">카테고리 선택</h2>
                <button
                    type="button"
                    class="category-sheet__close"
                    aria-label="닫기"
                    @click="closeSheet"
                >
                    ✕
                </button>
            </div>
        </template>

        <div v-if="transaction" class="category-sheet__summary">
            <p class="category-sheet__merchant">{{ transaction.merchant }}</p>
            <p class="category-sheet__meta">
                {{ formatDayLabel(transaction.date) }} · {{ formatWon(transaction.amount) }}
            </p>
            <p class="category-sheet__direction">
                {{ isExpense ? '지출 카테고리' : '수입 카테고리' }}
            </p>
        </div>

        <label v-if="transaction" class="category-sheet__merchant-toggle">
            <span class="category-sheet__merchant-toggle-text">
                <span class="category-sheet__merchant-toggle-label">이 가맹점에 항상 적용</span>
                <span class="category-sheet__merchant-toggle-desc">
                    같은 가맹점의 다른 거래에도 함께 적용돼요
                </span>
            </span>
            <input
                v-model="applyToMerchant"
                type="checkbox"
                role="switch"
                class="category-sheet__switch"
            />
        </label>

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
                        :aria-expanded="isExpense ? expandedParentId === item.id : undefined"
                        :aria-pressed="!isExpense ? isActiveTile(item) : undefined"
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
                            'category-sheet__chip--active': child.name === pendingCategoryName,
                        }"
                        :aria-pressed="child.name === pendingCategoryName"
                        @click="chooseExpenseChild(child)"
                    >
                        {{ child.name }}
                    </button>
                </div>
            </template>
        </div>

        <template #footer>
            <BaseButton variant="primary" block @click="confirmSelection">변경하기</BaseButton>
        </template>
    </BaseBottomSheet>
</template>

<style scoped>
.category-sheet__header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: var(--tt-space-3);
}

.category-sheet__title {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    line-height: var(--tt-lh-snug);
}

.category-sheet__close {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    cursor: pointer;
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.category-sheet__close:hover {
    color: var(--tt-text);
    background: var(--tt-border);
}

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

.category-sheet__merchant-toggle {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    padding: var(--tt-space-3) 0;
    margin-bottom: var(--tt-space-2);
    cursor: pointer;
}

.category-sheet__merchant-toggle-text {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
}

.category-sheet__merchant-toggle-label {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.category-sheet__merchant-toggle-desc {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.category-sheet__switch {
    position: relative;
    flex-shrink: 0;
    width: 44px;
    height: 26px;
    appearance: none;
    background: var(--tt-border);
    border: none;
    border-radius: var(--tt-radius-full);
    cursor: pointer;
    transition: background 0.15s ease;
}

.category-sheet__switch::before {
    position: absolute;
    top: 3px;
    left: 3px;
    width: 20px;
    height: 20px;
    content: '';
    background: var(--tt-bg);
    border-radius: var(--tt-radius-full);
    box-shadow: var(--tt-elevation-1);
    transition: transform 0.15s ease;
}

.category-sheet__switch:checked {
    background: var(--tt-primary);
}

.category-sheet__switch:checked::before {
    transform: translateX(18px);
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
