<script setup>
import { computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import BaseBadge from '@/components/common/BaseBadge.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import FixedExpenseCandidateEvidence from '@/components/fixed-expense/FixedExpenseCandidateEvidence.vue';
import FixedExpensePageHeader from '@/components/fixed-expense/FixedExpensePageHeader.vue';
import { useFixedExpenseStore } from '@/stores/fixedExpense';
import { formatBillingCycle, formatWon, resolveFixedExpenseState } from '@/utils/fixedExpense';

const route = useRoute();
const router = useRouter();
const store = useFixedExpenseStore();
const { selectedCandidate, loading, error, actionLoading } = storeToRefs(store);
const state = computed(() =>
    resolveFixedExpenseState({
        loading: loading.value,
        error: error.value,
        data: selectedCandidate.value,
    }),
);

async function decide(decision) {
    const succeeded = await store.decideCandidate(route.params.candidateId, decision);
    if (succeeded) {
        router.replace('/asset/fixed-expenses');
    }
}

function goBack() {
    router.back();
}

onMounted(() => store.loadCandidate(route.params.candidateId));
</script>

<template>
    <article class="candidate-view">
        <FixedExpensePageHeader
            title="이 지출, 고정지출인가요?"
            back-label="이전 화면으로 이동"
            @back="goBack"
        />
        <p class="candidate-view__lead">
            반복 패턴을 탐지했어요. 맞으면 지정, 아니면 해제해 주세요
        </p>

        <StateLoading v-if="state === 'loading'" size="lg" message="반복 결제를 확인하고 있어요" />
        <StateError
            v-else-if="state === 'error'"
            :message="error"
            @retry="store.loadCandidate(route.params.candidateId)"
        />
        <StateEmpty v-else-if="state === 'empty'" title="이미 처리된 탐지 후보예요" />
        <template v-else>
            <BaseCard class="candidate-view__card" padding="none">
                <div class="candidate-view__meta">
                    <BaseBadge variant="daily">탐지 후보</BaseBadge>
                    <span>{{ selectedCandidate.caseNumber }}</span>
                </div>
                <div class="candidate-view__identity">
                    <span>{{ selectedCandidate.categoryLabel }}</span>
                    <div>
                        <strong>{{ selectedCandidate.name }}</strong>
                        <small
                            >{{ selectedCandidate.category }} ·
                            {{ selectedCandidate.description }}</small
                        >
                    </div>
                </div>
                <dl>
                    <div>
                        <dt>평균 금액</dt>
                        <dd>{{ formatWon(selectedCandidate.averageAmount) }}</dd>
                    </div>
                    <div>
                        <dt>예상 결제일</dt>
                        <dd>{{ formatBillingCycle(selectedCandidate.billingCycle) }}</dd>
                    </div>
                    <div>
                        <dt>탐지 근거</dt>
                        <dd>{{ selectedCandidate.evidenceCount }}회 반복</dd>
                    </div>
                </dl>
            </BaseCard>

            <FixedExpenseCandidateEvidence
                :count="selectedCandidate.evidenceCount"
                :months="selectedCandidate.evidenceMonths"
            />

            <div class="candidate-view__actions">
                <BaseButton
                    variant="ghost"
                    size="lg"
                    block
                    :loading="actionLoading"
                    @click="decide('dismiss')"
                >
                    고정지출 아니에요
                </BaseButton>
                <BaseButton
                    class="candidate-view__confirm"
                    size="lg"
                    block
                    :loading="actionLoading"
                    @click="decide('confirm')"
                >
                    고정지출로 지정
                </BaseButton>
            </div>
        </template>
    </article>
</template>

<style scoped>
.candidate-view {
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5) var(--tt-space-5) calc(var(--tt-space-12) * 2);
    background: var(--tt-bg-subtle);
}

.candidate-view__lead {
    margin-top: var(--tt-space-1);
    color: var(--tt-text-subtle);
}

.candidate-view__card {
    position: relative;
    margin-top: var(--tt-space-5);
    overflow: hidden;
    border-radius: var(--tt-radius-md);
}

.candidate-view__card::before {
    position: absolute;
    top: 55px;
    left: 15px;
    z-index: 1;
    width: 10px;
    height: 10px;
    content: '';
    background: var(--tt-surface-row);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    box-shadow:
        0 42px 0 var(--tt-surface-row),
        0 42px 0 1px var(--tt-border),
        0 84px 0 var(--tt-surface-row),
        0 84px 0 1px var(--tt-border);
}

.candidate-view__card:deep(.tt-card__body) {
    background: linear-gradient(
        to bottom,
        var(--tt-bg) 0,
        var(--tt-bg) 22%,
        var(--tt-surface-row) 22%,
        var(--tt-surface-row) 100%
    );
}

.candidate-view__meta {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: var(--tt-space-4) var(--tt-space-5) 0;
    font-family: var(--tt-font-mono);
    color: var(--tt-text-soft);
}

.candidate-view__identity {
    display: flex;
    align-items: center;
    gap: var(--tt-space-4);
    padding: var(--tt-space-4) var(--tt-space-10);
}

.candidate-view__identity > span {
    display: grid;
    width: 52px;
    height: 52px;
    place-items: center;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-strong);
    background: var(--tt-category-subscription);
    border-radius: var(--tt-radius-full);
}

.candidate-view__identity strong,
.candidate-view__identity small {
    display: block;
}

.candidate-view__identity strong {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}

.candidate-view__identity small {
    color: var(--tt-text-subtle);
}

.candidate-view__card dl {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--tt-space-3);
    margin: 0 var(--tt-space-10);
    padding: var(--tt-space-4) 0 var(--tt-space-5);
    border-top: 1px dashed var(--tt-border);
}

.candidate-view__card dt {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-soft);
}

.candidate-view__card dd {
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-black);
    white-space: nowrap;
}

.candidate-view > .candidate-evidence {
    margin-top: var(--tt-space-4);
}

.candidate-view__actions {
    position: fixed;
    right: max(var(--tt-space-5), calc((100vw - var(--tt-content-max)) / 2 + var(--tt-space-5)));
    bottom: calc(var(--tt-tabbar-height) + env(safe-area-inset-bottom) + var(--tt-space-3));
    left: max(var(--tt-space-5), calc((100vw - var(--tt-content-max)) / 2 + var(--tt-space-5)));
    z-index: var(--tt-z-sticky);
    display: grid;
    grid-template-columns: 0.9fr 1.1fr;
    gap: var(--tt-space-2);
}

.candidate-view__actions :deep(.tt-btn) {
    padding-right: var(--tt-space-3);
    padding-left: var(--tt-space-3);
    font-size: var(--tt-fs-body);
    white-space: nowrap;
}

.candidate-view__confirm {
    background: var(--tt-surface-strong);
}

.candidate-view__confirm:not(:disabled):hover {
    background: var(--tt-surface-strong-muted);
}

@media (max-width: 360px) {
    .candidate-view {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }

    .candidate-view__identity,
    .candidate-view__card dl {
        margin-right: var(--tt-space-4);
        margin-left: var(--tt-space-4);
    }

    .candidate-view__identity {
        padding-right: 0;
        padding-left: 0;
    }

    .candidate-view__card dd {
        font-size: var(--tt-fs-mono-chip);
    }

    .candidate-view__actions {
        right: var(--tt-space-4);
        left: var(--tt-space-4);
    }
}
</style>
