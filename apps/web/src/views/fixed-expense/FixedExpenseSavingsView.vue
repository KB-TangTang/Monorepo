<script setup>
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import BaseButton from '@/components/common/BaseButton.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import FixedExpensePageHeader from '@/components/fixed-expense/FixedExpensePageHeader.vue';
import FixedExpenseReceiptPanel from '@/components/fixed-expense/FixedExpenseReceiptPanel.vue';
import FixedExpenseSummaryCard from '@/components/fixed-expense/FixedExpenseSummaryCard.vue';
import TempFixedExpenseSourceToggle from '@/components/fixed-expense/TempFixedExpenseSourceToggle.vue';
import { useFixedExpenseStore } from '@/stores/fixedExpense';
import { formatSavingsWon, formatWon, resolveFixedExpenseState } from '@/utils/fixedExpense';

const router = useRouter();
const store = useFixedExpenseStore();
const { savings, loading, error, source } = storeToRefs(store);
const state = computed(() =>
    resolveFixedExpenseState({ loading: loading.value, error: error.value, data: savings.value }),
);

function goBack() {
    if (window.history.length > 1) {
        router.back();
        return;
    }
    router.push('/asset');
}

async function switchSource(nextSource) {
    if (nextSource === source.value) {
        return;
    }
    store.setSource(nextSource);
    await store.loadSavings();
}

onMounted(() => store.loadSavings());
</script>

<template>
    <article class="savings-view">
        <FixedExpensePageHeader
            title="절약 감정서"
            back-label="이전 화면으로 이동"
            @back="goBack"
        />

        <StateLoading
            v-if="state === 'loading'"
            size="lg"
            message="절약 가능 금액을 계산하고 있어요"
        />
        <StateError v-else-if="state === 'error'" :message="error" @retry="store.loadSavings" />
        <StateEmpty
            v-else-if="state === 'empty'"
            title="절약 가능한 고정지출이 없어요"
            description="새로운 반복 지출이 발견되면 알려드릴게요."
        />
        <template v-else>
            <FixedExpenseSummaryCard mode="savings" :data="savings" />

            <section class="savings-view__items" aria-labelledby="savings-items-title">
                <h2 id="savings-items-title">항목별 절약 가능액</h2>
                <FixedExpenseReceiptPanel heading="선고 명세 · 절약 항목">
                    <ul>
                        <li v-for="item in savings.items" :key="item.id">
                            <span
                                class="savings-view__category"
                                :class="`savings-view__category--${item.categoryCode}`"
                            >
                                {{ item.categoryLabel }}
                            </span>
                            <span class="savings-view__copy">
                                <strong>{{ item.title }}</strong>
                                <small>{{ item.description }}</small>
                            </span>
                            <strong class="savings-view__value">
                                {{ formatSavingsWon(item.savingsAmount) }}
                            </strong>
                        </li>
                    </ul>
                    <template #footer>
                        <div class="savings-view__total">
                            <strong>월 절약 합계</strong>
                            <strong>{{ formatWon(savings.monthlySavings) }}</strong>
                        </div>
                    </template>
                </FixedExpenseReceiptPanel>
            </section>

            <div class="savings-view__action">
                <BaseButton block size="lg" @click="router.push('/asset/fixed-expenses')">
                    고정지출 정리하기
                </BaseButton>
            </div>
        </template>

        <TempFixedExpenseSourceToggle
            :source="source"
            :loading="loading"
            elevated
            @toggle="switchSource"
        />
    </article>
</template>

<style scoped>
.savings-view {
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5) var(--tt-space-5) calc(var(--tt-space-12) * 3);
    background: var(--tt-bg-subtle);
}

.savings-view > .fixed-expense-header {
    margin-bottom: var(--tt-space-3);
}

.savings-view__items {
    margin-top: var(--tt-space-4);
}

.savings-view__items h2 {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}

.savings-view__items li {
    display: grid;
    grid-template-columns: 30px 1fr auto;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-2) 0;
}

.savings-view__category {
    display: grid;
    width: 28px;
    height: 28px;
    place-items: center;
    font-weight: var(--tt-fw-black);
    border-radius: var(--tt-radius-sm);
}

.savings-view__category--subscription {
    color: var(--tt-accent-strong);
    background: var(--tt-category-subscription);
}

.savings-view__category--living {
    color: var(--tt-success);
    background: var(--tt-category-living);
}

.savings-view__category--telecom {
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
}

.savings-view__copy strong,
.savings-view__copy small {
    display: block;
}

.savings-view__copy strong {
    color: var(--tt-surface-strong-muted);
}

.savings-view__copy small {
    color: var(--tt-text-soft);
}

.savings-view__value {
    font-family: var(--tt-font-mono);
    color: var(--tt-success-strong);
    white-space: nowrap;
}

.savings-view__total {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.savings-view__total strong:last-child {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-section);
    color: var(--tt-success-strong);
}

.savings-view__action {
    position: fixed;
    right: max(var(--tt-space-5), calc((100vw - var(--tt-content-max)) / 2 + var(--tt-space-5)));
    bottom: calc(var(--tt-tabbar-height) + env(safe-area-inset-bottom) + var(--tt-space-3));
    left: max(var(--tt-space-5), calc((100vw - var(--tt-content-max)) / 2 + var(--tt-space-5)));
    z-index: var(--tt-z-sticky);
}

@media (max-width: 360px) {
    .savings-view {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }

    .savings-view__action {
        right: var(--tt-space-4);
        left: var(--tt-space-4);
    }
}
</style>
