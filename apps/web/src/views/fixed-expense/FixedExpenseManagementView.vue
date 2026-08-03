<script setup>
import { computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import BaseBadge from '@/components/common/BaseBadge.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import FixedExpenseItemCard from '@/components/fixed-expense/FixedExpenseItemCard.vue';
import FixedExpensePageHeader from '@/components/fixed-expense/FixedExpensePageHeader.vue';
import FixedExpenseSummaryCard from '@/components/fixed-expense/FixedExpenseSummaryCard.vue';
import { useFixedExpenseStore } from '@/stores/fixedExpense';
import { resolveFixedExpenseState } from '@/utils/fixedExpense';

const router = useRouter();
const store = useFixedExpenseStore();
const { summary, confirmed, candidates, loading, error } = storeToRefs(store);
const state = computed(() =>
    resolveFixedExpenseState({ loading: loading.value, error: error.value, data: summary.value }),
);

function openExpense(expenseId) {
    router.push(`/asset/fixed-expenses/${expenseId}`);
}

function openCandidate(candidateId) {
    router.push(`/asset/fixed-expenses/candidates/${candidateId}`);
}

onMounted(() => store.loadOverview());
</script>

<template>
    <article class="management-view">
        <FixedExpensePageHeader title="고정지출 관리" />

        <StateLoading v-if="state === 'loading'" size="lg" message="고정지출을 정리하고 있어요" />
        <StateError v-else-if="state === 'error'" :message="error" @retry="store.loadOverview" />
        <StateEmpty
            v-else-if="state === 'empty'"
            title="등록된 고정지출이 없어요"
            description="반복 결제가 발견되면 이곳에 모아드릴게요."
        />
        <template v-else>
            <FixedExpenseSummaryCard mode="overview" :data="summary" />

            <section class="management-view__section" aria-labelledby="confirmed-title">
                <h2 id="confirmed-title">확정된 고정지출</h2>
                <div v-if="confirmed.length" class="management-view__list">
                    <FixedExpenseItemCard
                        v-for="item in confirmed"
                        :key="item.id"
                        :item="item"
                        @select="openExpense"
                    />
                </div>
                <StateEmpty v-else title="확정된 고정지출이 없어요" />
            </section>

            <section class="management-view__section" aria-labelledby="candidate-title">
                <div class="management-view__section-title">
                    <h2 id="candidate-title">탐지된 후보</h2>
                    <BaseBadge v-if="candidates.length" variant="daily">
                        확인 필요 {{ summary.candidateCount }}
                    </BaseBadge>
                </div>
                <div v-if="candidates.length" class="management-view__list">
                    <FixedExpenseItemCard
                        v-for="item in candidates"
                        :key="item.id"
                        :item="item"
                        candidate
                        @select="openCandidate"
                    />
                </div>
                <StateEmpty v-else title="확인할 탐지 후보가 없어요" />
            </section>
        </template>
    </article>
</template>

<style scoped>
.management-view {
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5) var(--tt-space-5) var(--tt-space-12);
    background: var(--tt-bg-subtle);
}

.management-view > .fixed-expense-header {
    margin-bottom: var(--tt-space-3);
}

.management-view__section {
    margin-top: var(--tt-space-5);
}

.management-view__section h2 {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}

.management-view__section-title {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.management-view__list {
    display: grid;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-3);
}

@media (max-width: 360px) {
    .management-view {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }
}
</style>
