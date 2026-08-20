<script setup>
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import FixedExpensePageHeader from '@/components/fixed-expense/FixedExpensePageHeader.vue';
import FixedExpenseReceiptPanel from '@/components/fixed-expense/FixedExpenseReceiptPanel.vue';
import FixedExpenseSummaryCard from '@/components/fixed-expense/FixedExpenseSummaryCard.vue';
import TempFixedExpenseSourceToggle from '@/components/fixed-expense/TempFixedExpenseSourceToggle.vue';
import { useFixedExpenseStore } from '@/stores/fixedExpense';
import {
    formatPaymentDate,
    formatWon,
    resolveFixedExpenseState,
    shouldUseFixedExpenseApiSource,
} from '@/utils/fixedExpense';

const route = useRoute();
const router = useRouter();
const store = useFixedExpenseStore();
const { selectedExpense, loading, error, source } = storeToRefs(store);
const state = computed(() =>
    resolveFixedExpenseState({
        loading: loading.value,
        error: error.value,
        data: selectedExpense.value,
    }),
);
const recentPaymentHistory = computed(
    () => selectedExpense.value?.paymentHistory.slice(0, 4) ?? [],
);

function goBack() {
    router.back();
}

async function switchSource(nextSource) {
    if (nextSource === source.value) {
        return;
    }
    store.setSource(nextSource);
    await store.loadExpense(route.params.expenseId);
}

onMounted(() => {
    if (shouldUseFixedExpenseApiSource(route.query.source)) {
        store.setSource('api');
    }
    return store.loadExpense(route.params.expenseId);
});
</script>

<template>
    <article class="detail-view">
        <FixedExpensePageHeader
            title="고정지출 상세"
            back-label="고정지출 관리로 이동"
            @back="goBack"
        />

        <StateLoading v-if="state === 'loading'" size="lg" message="결제 이력을 불러오고 있어요" />
        <StateError
            v-else-if="state === 'error'"
            :message="error"
            @retry="store.loadExpense(route.params.expenseId)"
        />
        <StateEmpty v-else-if="state === 'empty'" title="고정지출 정보를 찾을 수 없어요" />
        <template v-else>
            <FixedExpenseSummaryCard mode="detail" :data="selectedExpense" />

            <section class="detail-view__history" aria-labelledby="history-title">
                <h2 id="history-title">최근 결제 이력</h2>
                <FixedExpenseReceiptPanel heading="PAYMENT HISTORY · 6개월">
                    <ul v-if="recentPaymentHistory.length">
                        <li v-for="payment in recentPaymentHistory" :key="payment.id">
                            <span>
                                <strong>{{ formatPaymentDate(payment.date) }}</strong>
                                <small>{{ payment.provider }}</small>
                            </span>
                            <strong>{{ formatWon(payment.amount) }}</strong>
                        </li>
                    </ul>
                    <StateEmpty v-else title="최근 결제 이력이 없어요" />
                    <template #footer>
                        <div class="detail-view__total">
                            <strong>6개월 합계</strong>
                            <strong>{{ formatWon(selectedExpense.sixMonthTotal) }}</strong>
                        </div>
                    </template>
                </FixedExpenseReceiptPanel>
            </section>

            <p v-if="selectedExpense.changeNotice" class="detail-view__notice">
                {{ selectedExpense.changeNotice.month }}월부터
                <strong>{{ formatWon(selectedExpense.changeNotice.difference) }}</strong>
                인상됐어요. 요금제를 점검해볼까요?
            </p>
        </template>

        <TempFixedExpenseSourceToggle :source="source" :loading="loading" @toggle="switchSource" />
    </article>
</template>

<style scoped>
.detail-view {
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5) var(--tt-space-5) var(--tt-space-12);
    background: var(--tt-bg-subtle);
}

.detail-view > .fixed-expense-header {
    margin-bottom: var(--tt-space-3);
}

.detail-view__history {
    margin-top: var(--tt-space-4);
}

.detail-view__history h2 {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}

.detail-view__history li {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--tt-space-2) 0;
}

.detail-view__history li strong,
.detail-view__history li small {
    display: block;
}

.detail-view__history li > strong,
.detail-view__total strong:last-child {
    font-family: var(--tt-font-mono);
    color: var(--tt-surface-strong-muted);
}

.detail-view__history li small {
    color: var(--tt-text-soft);
}

.detail-view__total {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.detail-view__notice {
    margin-top: var(--tt-space-5);
    padding: var(--tt-space-4);
    color: var(--tt-accent-strong);
    background: var(--tt-accent-subtle);
    border: 1px solid var(--tt-border-candidate);
    border-radius: var(--tt-radius-md);
}

.detail-view__notice strong {
    font-family: var(--tt-font-mono);
}

@media (max-width: 360px) {
    .detail-view {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }
}
</style>
