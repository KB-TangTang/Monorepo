<!--
  용도: 장부 화면에서 "자세히보기"로 진입하는, 선택한 달의 거래 전체를 날짜별로 훑어보는 화면.
  진입 시 쿼리로 받은 date 가 있으면 그 날짜 그룹이 화면 위쪽에 오도록 스크롤한다.
  언제 쓰는지: router/index.js 의 /ledger/transactions (name: ledgerMonthTransactions) 라우트.
-->
<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchLedgerMonths, fetchLedgerTransactions } from '@/api/ledger';
import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
import BaseInput from '@/components/common/BaseInput.vue';
import ChallengeReportToggle from '@/components/challenge/report/ChallengeReportToggle.vue';
import LedgerCategoryFilterSheet from '@/components/ledger/LedgerCategoryFilterSheet.vue';
import LedgerCategorySheet from '@/components/ledger/LedgerCategorySheet.vue';
import LedgerDirectionTabs from '@/components/ledger/LedgerDirectionTabs.vue';
import LedgerMonthNav from '@/components/ledger/LedgerMonthNav.vue';
import LedgerPaymentMethodSheet from '@/components/ledger/LedgerPaymentMethodSheet.vue';
import LedgerTransactionRow from '@/components/ledger/LedgerTransactionRow.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import {
    formatDayLabel,
    formatWon,
    filterTransactionsByTab,
    groupTransactionsByDate,
    resolveAnchorDate,
    resolveDefaultLedgerPeriod,
    resolveLedgerState,
} from '@/utils/ledger';

const route = useRoute();
const router = useRouter();

const months = ref([]);
const period = ref(String(route.query.month ?? ''));
const transactions = ref([]);
const loading = ref(true);
const errorMessage = ref('');

const searchTerm = ref('');
const activeTab = ref('ALL');
const selectedCategory = ref('');
const selectedPaymentMethod = ref('');
const isPaymentSheetOpen = ref(false);
const isCategoryFilterSheetOpen = ref(false);
const selectedTransaction = ref(null);
const isCategorySheetOpen = ref(false);

const rootEl = ref(null);
const groupRefs = ref({});
let pendingAnchorDate = typeof route.query.date === 'string' ? route.query.date : '';

const state = computed(() =>
    resolveLedgerState({ loading: loading.value, error: errorMessage.value, data: transactions.value }),
);

const paymentMethods = computed(() => [
    ...new Set(transactions.value.map((tx) => tx.paymentMethod)),
]);

const categoriesInMonth = computed(() => [
    ...new Set(transactions.value.map((tx) => tx.category)),
]);

const filteredTransactions = computed(() => {
    let list = filterTransactionsByTab(transactions.value, activeTab.value);
    if (selectedPaymentMethod.value) {
        list = list.filter((tx) => tx.paymentMethod === selectedPaymentMethod.value);
    }
    if (selectedCategory.value) {
        list = list.filter((tx) => tx.category === selectedCategory.value);
    }
    const keyword = searchTerm.value.trim();
    if (keyword !== '') {
        list = list.filter((tx) => tx.merchant.includes(keyword));
    }
    return list;
});

const groupedTransactions = computed(() => groupTransactionsByDate(filteredTransactions.value));

const monthIndex = computed(() => months.value.findIndex((month) => month.value === period.value));
const disablePrev = computed(() => monthIndex.value <= 0);
const disableNext = computed(
    () => monthIndex.value === -1 || monthIndex.value >= months.value.length - 1,
);

function setGroupRef(date, el) {
    if (el) {
        groupRefs.value[date] = el;
    } else {
        delete groupRefs.value[date];
    }
}

async function scrollToAnchorIfNeeded() {
    const anchorDate = resolveAnchorDate(groupedTransactions.value, pendingAnchorDate);
    pendingAnchorDate = '';
    if (!anchorDate) {
        return;
    }
    await nextTick();
    groupRefs.value[anchorDate]?.scrollIntoView({ block: 'start' });
}

async function loadPeriod({ resetScroll } = { resetScroll: false }) {
    loading.value = true;
    errorMessage.value = '';
    try {
        transactions.value = await fetchLedgerTransactions(period.value);
    } catch (err) {
        errorMessage.value = err.message ?? '거래내역을 불러오지 못했습니다.';
        loading.value = false;
        return;
    }
    loading.value = false;
    if (resetScroll) {
        groupRefs.value = {};
        rootEl.value?.scrollTo({ top: 0 });
    } else {
        await scrollToAnchorIfNeeded();
    }
}

function changeMonth(delta) {
    const nextIndex = monthIndex.value + delta;
    if (nextIndex < 0 || nextIndex >= months.value.length) {
        return;
    }
    period.value = months.value[nextIndex].value;
    selectedPaymentMethod.value = '';
    selectedCategory.value = '';
    activeTab.value = 'ALL';
    loadPeriod({ resetScroll: true });
}

function selectTab(tab) {
    activeTab.value = tab;
}

function selectPaymentMethod(method) {
    selectedPaymentMethod.value = method;
}

function selectCategoryFilter(category) {
    selectedCategory.value = category;
}

function openCategorySheet(tx) {
    selectedTransaction.value = tx;
    isCategorySheetOpen.value = true;
}

function applyCategory({ transactionId, categoryName, applyToMerchant }) {
    const tx = transactions.value.find((item) => item.id === transactionId);
    if (!tx) {
        return;
    }
    if (applyToMerchant) {
        const merchant = tx.merchant;
        transactions.value.forEach((item) => {
            if (item.merchant === merchant) {
                item.category = categoryName;
            }
        });
    } else {
        tx.category = categoryName;
    }
}

function goToLedger() {
    router.push({ name: 'ledger' });
}

function openReport() {
    router.push({ name: 'monthlyConsumptionReport', query: { month: period.value } });
}

onMounted(async () => {
    try {
        months.value = await fetchLedgerMonths();
        if (period.value && !months.value.some((m) => m.value === period.value)) {
            period.value = resolveDefaultLedgerPeriod(months.value);
        }
        if (!period.value) {
            errorMessage.value = '조회할 월 정보가 없습니다.';
            loading.value = false;
            return;
        }
        await loadPeriod();
    } catch (err) {
        errorMessage.value = err.message ?? '거래내역을 불러오지 못했습니다.';
        loading.value = false;
    }
});
</script>

<template>
    <article ref="rootEl" class="ledger-month-view">
        <BaseBackHeader title="거래 상세내역" back-label="전체 거래내역으로 돌아가기" />

        <StateLoading v-if="state === 'loading'" size="lg" message="거래내역을 불러오는 중" />
        <StateError v-else-if="state === 'error'" :message="errorMessage" @retry="loadPeriod" />
        <template v-else-if="state === 'ready'">
            <LedgerMonthNav
                :period="period"
                :disable-prev="disablePrev"
                :disable-next="disableNext"
                :payment-method-label="selectedPaymentMethod || '전체 수단'"
                @prev="changeMonth(-1)"
                @next="changeMonth(1)"
                @open-payment-filter="isPaymentSheetOpen = true"
            />

            <BaseInput
                v-model="searchTerm"
                placeholder="거래명이나 금액을 검색해보세요"
                aria-label="거래명이나 금액으로 검색"
            />

            <LedgerDirectionTabs
                :active="activeTab"
                :category-label="selectedCategory || '카테고리'"
                :payment-label="selectedPaymentMethod || '수단'"
                @select="selectTab"
                @open-category-filter="isCategoryFilterSheetOpen = true"
                @open-payment-filter="isPaymentSheetOpen = true"
            />

            <section class="ledger-month-view__list" aria-label="이달 거래내역">
                <StateEmpty
                    v-if="groupedTransactions.length === 0"
                    title="조건에 맞는 거래내역이 없어요"
                    description="필터나 검색어를 바꿔서 다시 확인해 보세요."
                />
                <div
                    v-for="group in groupedTransactions"
                    :key="group.date"
                    :ref="(el) => setGroupRef(group.date, el)"
                    class="ledger-month-view__group"
                >
                    <div class="ledger-month-view__group-header">
                        <h2>{{ formatDayLabel(group.date) }}</h2>
                        <span :class="{ 'ledger-month-view__group-total--income': group.netAmount > 0 }">
                            {{ formatWon(group.netAmount) }}
                        </span>
                    </div>
                    <ul class="ledger-month-view__group-rows">
                        <LedgerTransactionRow
                            v-for="tx in group.items"
                            :key="tx.id"
                            :transaction="tx"
                            @click="openCategorySheet(tx)"
                        />
                    </ul>
                </div>
            </section>
        </template>

        <LedgerPaymentMethodSheet
            v-model="isPaymentSheetOpen"
            :methods="paymentMethods"
            :selected="selectedPaymentMethod"
            @select="selectPaymentMethod"
        />

        <LedgerCategoryFilterSheet
            v-model="isCategoryFilterSheetOpen"
            :categories="categoriesInMonth"
            :selected="selectedCategory"
            @select="selectCategoryFilter"
        />

        <LedgerCategorySheet
            v-model="isCategorySheetOpen"
            :transaction="selectedTransaction"
            @select="applyCategory"
        />

        <ChallengeReportToggle active="transactions" @open-transactions="goToLedger" @open-monthly-report="openReport" />
    </article>
</template>

<style scoped>
.ledger-month-view {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
    height: calc(100vh - var(--tt-tabbar-height) - env(safe-area-inset-bottom) - var(--tt-space-4));
    padding: var(--tt-space-3) var(--tt-space-5) calc(var(--tt-space-12) + 56px);
    overflow-y: auto;
    background: var(--tt-bg-subtle);
}

.ledger-month-view__list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
}

.ledger-month-view__group {
    padding: var(--tt-space-3);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    scroll-margin-top: var(--tt-space-3);
}

.ledger-month-view__group-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-2);
    margin-bottom: var(--tt-space-1);
}

.ledger-month-view__group-header h2 {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.ledger-month-view__group-header span {
    flex-shrink: 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger);
}

.ledger-month-view__group-header .ledger-month-view__group-total--income {
    color: var(--tt-success);
}

.ledger-month-view__group-rows {
    display: flex;
    flex-direction: column;
}

@media (max-width: 360px) {
    .ledger-month-view {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }
}
</style>
