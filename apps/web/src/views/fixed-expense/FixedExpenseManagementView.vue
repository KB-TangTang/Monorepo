<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import BaseBadge from '@/components/common/BaseBadge.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import FixedExpenseItemCard from '@/components/fixed-expense/FixedExpenseItemCard.vue';
import FixedExpensePageHeader from '@/components/fixed-expense/FixedExpensePageHeader.vue';
import FixedExpenseSummaryCard from '@/components/fixed-expense/FixedExpenseSummaryCard.vue';
import TempFixedExpenseSourceToggle from '@/components/fixed-expense/TempFixedExpenseSourceToggle.vue';
import {
    resetFixedExpensePaymentReminders,
    runFixedExpensePaymentReminderBatch,
} from '@/api/fixedExpense';
import { useFixedExpenseStore } from '@/stores/fixedExpense';
import { useNotificationStore } from '@/stores/notification';
import { resolveFixedExpenseState } from '@/utils/fixedExpense';

const router = useRouter();
const store = useFixedExpenseStore();
const notification = useNotificationStore();
const { summary, confirmed, candidates, loading, error, source } = storeToRefs(store);
const isDev = import.meta.env.DEV;
const devAction = ref('');
const state = computed(() =>
    resolveFixedExpenseState({ loading: loading.value, error: error.value, data: summary.value }),
);

function openExpense(expenseId) {
    router.push(`/asset/fixed-expenses/${expenseId}`);
}

function openCandidate(candidateId) {
    router.push(`/asset/fixed-expenses/candidates/${candidateId}`);
}

function goBack() {
    router.back();
}

async function runPaymentReminderBatch() {
    devAction.value = 'run';
    try {
        const result = await runFixedExpensePaymentReminderBatch();
        if (result.affected === 0) {
            window.alert('발송 대상이 없어요. 예정일·확정 상태를 확인하세요.');
        }
    } catch (err) {
        window.alert(err.message ?? '배치 실행에 실패했어요.');
    } finally {
        devAction.value = '';
    }
}

async function resetPaymentReminders() {
    if (!window.confirm('결제 예정 알림과 발송 이력만 초기화할까요? 다른 알림은 유지됩니다.')) {
        return;
    }

    devAction.value = 'reset';
    try {
        await resetFixedExpensePaymentReminders();
        await notification.refreshBadge();
    } catch (err) {
        window.alert(err.message ?? '알림 초기화에 실패했어요.');
    } finally {
        devAction.value = '';
    }
}

async function switchSource(nextSource) {
    if (nextSource === source.value) {
        return;
    }
    store.setSource(nextSource);
    await store.loadOverview();
}

onMounted(() => store.loadOverview());
</script>

<template>
    <article class="management-view">
        <FixedExpensePageHeader
            title="고정지출 관리"
            back-label="절약 감정서로 이동"
            @back="goBack"
        />

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

        <div v-if="isDev" class="management-view__dev-actions" aria-label="개발용 결제 예정 알림 도구">
            <BaseButton
                variant="dark"
                size="sm"
                :loading="devAction === 'run'"
                :disabled="Boolean(devAction)"
                title="실제 로컬 DB의 결제 예정 알림 배치를 즉시 실행합니다"
                @click="runPaymentReminderBatch"
            >
                알림 실행
            </BaseButton>
            <BaseButton
                variant="danger"
                size="sm"
                :loading="devAction === 'reset'"
                :disabled="Boolean(devAction)"
                title="현재 사용자의 결제 예정 알림과 발송 이력만 초기화합니다"
                @click="resetPaymentReminders"
            >
                알림 초기화
            </BaseButton>
        </div>

        <TempFixedExpenseSourceToggle :source="source" :loading="loading" @toggle="switchSource" />
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

.management-view__dev-actions {
    position: fixed;
    bottom: calc(var(--tt-tabbar-height) + env(safe-area-inset-bottom) + var(--tt-space-3));
    left: max(var(--tt-space-3), calc((100vw - var(--tt-content-max)) / 2 + var(--tt-space-3)));
    z-index: var(--tt-z-sticky);
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: var(--tt-space-2);
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
