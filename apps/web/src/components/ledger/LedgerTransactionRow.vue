<!--
  용도: 거래내역 목록의 한 행. 카테고리 아이콘 배지 + 가맹점명 + 카테고리·결제수단 + 금액.
  탭하면 카테고리 선택 팝업을 열도록 click 이벤트를 emit 한다.
  언제 쓰는지: LedgerView 의 날짜별 그룹 안에서 반복 렌더한다.
-->
<script setup>
import CategoryIcon from '@/components/common/CategoryIcon.vue';
import { resolveCategoryIcon, resolveCategoryTone } from '@/utils/category';
import { formatWon } from '@/utils/ledger';

defineProps({
    transaction: { type: Object, required: true },
});

defineEmits(['click']);
</script>

<template>
    <li class="ledger-row">
        <button type="button" class="ledger-row__button" @click="$emit('click')">
            <span
                class="ledger-row__avatar"
                :class="`ledger-row__avatar--${resolveCategoryTone(transaction.category)}`"
                aria-hidden="true"
            >
                <CategoryIcon :icon="resolveCategoryIcon(transaction.category)" />
            </span>
            <div class="ledger-row__info">
                <p class="ledger-row__name">{{ transaction.merchant }}</p>
                <p class="ledger-row__meta">
                    {{ transaction.category }} · {{ transaction.paymentMethod }}
                </p>
            </div>
            <p
                class="ledger-row__amount"
                :class="{ 'ledger-row__amount--income': transaction.amount > 0 }"
            >
                {{ formatWon(transaction.amount) }}
            </p>
        </button>
    </li>
</template>

<style scoped>
.ledger-row + .ledger-row {
    border-top: 1px solid var(--tt-border);
}

.ledger-row__button {
    display: flex;
    align-items: center;
    width: 100%;
    padding: var(--tt-space-2) 0;
    text-align: left;
    cursor: pointer;
    background: none;
    border: none;
    gap: var(--tt-space-3);
}

.ledger-row__avatar {
    display: grid;
    flex-shrink: 0;
    width: 40px;
    height: 40px;
    font-weight: var(--tt-fw-bold);
    border-radius: var(--tt-radius-full);
    place-items: center;
}

.ledger-row__avatar--primary {
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
}

.ledger-row__avatar--accent {
    color: var(--tt-accent-strong);
    background: var(--tt-accent-subtle);
}

.ledger-row__avatar--success {
    color: var(--tt-success);
    background: var(--tt-success-subtle);
}

.ledger-row__avatar--muted {
    color: var(--tt-text-muted);
    background: var(--tt-gray-100);
}

.ledger-row__info {
    flex: 1;
    min-width: 0;
}

.ledger-row__name {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.ledger-row__meta {
    margin-top: 2px;
    overflow: hidden;
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
    text-overflow: ellipsis;
    white-space: nowrap;
}

.ledger-row__amount {
    flex-shrink: 0;
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger);
    white-space: nowrap;
}

.ledger-row__amount--income {
    color: var(--tt-success);
}
</style>
