<!--
  용도: 하단 탭바 "자료실" 진입 화면. 월별 지출·입금 요약 + 달력 + 전체 거래내역 목록을 보여준다.
  언제 쓰는지: router/index.js 의 /ledger 라우트. 백엔드 연동 전까지 api/ledger.js 가 목업 데이터를 반환한다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchLedgerMonths, fetchLedgerSummary, fetchLedgerTransactions } from '@/api/ledger';
import ChallengeReportToggle from '@/components/challenge/report/ChallengeReportToggle.vue';
import LedgerCalendar from '@/components/ledger/LedgerCalendar.vue';
import LedgerMonthNav from '@/components/ledger/LedgerMonthNav.vue';
import LedgerPageHeader from '@/components/ledger/LedgerPageHeader.vue';
import LedgerPaymentMethodSheet from '@/components/ledger/LedgerPaymentMethodSheet.vue';
import LedgerSummaryCard from '@/components/ledger/LedgerSummaryCard.vue';
import LedgerTransactionRow from '@/components/ledger/LedgerTransactionRow.vue';
import LedgerCategorySheet from '@/components/ledger/LedgerCategorySheet.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import {
    formatDayLabel,
    groupTransactionsByDate,
    resolveDefaultLedgerPeriod,
    resolveLedgerState,
} from '@/utils/ledger';

const router = useRouter();

const months = ref([]);
const period = ref('');
const summary = ref(null);
const transactions = ref([]);
const loading = ref(true);
const errorMessage = ref('');

const searchTerm = ref('');
const selectedDate = ref(null);
const selectedPaymentMethod = ref('');
const isPaymentSheetOpen = ref(false);
const selectedTransaction = ref(null);
const isCategorySheetOpen = ref(false);

const state = computed(() =>
    resolveLedgerState({ loading: loading.value, error: errorMessage.value, data: summary.value }),
);

const paymentFilteredTransactions = computed(() =>
    selectedPaymentMethod.value
        ? transactions.value.filter((tx) => tx.paymentMethod === selectedPaymentMethod.value)
        : transactions.value,
);

const transactionsByDate = computed(() => {
    const byDate = new Map();
    for (const tx of paymentFilteredTransactions.value) {
        if (!byDate.has(tx.date)) {
            byDate.set(tx.date, []);
        }
        byDate.get(tx.date).push(tx);
    }
    return byDate;
});

const visibleTransactions = computed(() => {
    const keyword = searchTerm.value.trim();
    return paymentFilteredTransactions.value.filter((tx) => {
        if (selectedDate.value && tx.date !== selectedDate.value) {
            return false;
        }
        return keyword === '' || tx.merchant.includes(keyword);
    });
});

const groupedTransactions = computed(() => groupTransactionsByDate(visibleTransactions.value));
const currentDayGroup = computed(() => groupedTransactions.value[0] ?? null);

const monthIndex = computed(() => months.value.findIndex((month) => month.value === period.value));
const disablePrev = computed(() => monthIndex.value <= 0);
const disableNext = computed(
    () => monthIndex.value === -1 || monthIndex.value >= months.value.length - 1,
);

async function loadPeriod() {
    loading.value = true;
    errorMessage.value = '';
    selectedDate.value = null;
    try {
        const [summaryData, transactionData] = await Promise.all([
            fetchLedgerSummary(period.value),
            fetchLedgerTransactions(period.value),
        ]);
        summary.value = summaryData;
        transactions.value = transactionData;
        selectedDate.value = transactionData.length
            ? transactionData.reduce(
                  (latest, tx) => (tx.date > latest ? tx.date : latest),
                  transactionData[0].date,
              )
            : null;
    } catch (err) {
        errorMessage.value = err.message ?? '거래내역을 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

function changeMonth(delta) {
    const nextIndex = monthIndex.value + delta;
    if (nextIndex < 0 || nextIndex >= months.value.length) {
        return;
    }
    period.value = months.value[nextIndex].value;
    selectedPaymentMethod.value = '';
    loadPeriod();
}

function selectDate(dateStr) {
    if (!dateStr) {
        return;
    }
    selectedDate.value = dateStr;
}

function selectPaymentMethod(method) {
    selectedPaymentMethod.value = method;
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

function openReport() {
    router.push({ name: 'monthlyConsumptionReport', query: { month: period.value } });
}

function openMonthTransactions() {
    router.push({
        name: 'ledgerMonthTransactions',
        query: { month: period.value, date: selectedDate.value },
    });
}

onMounted(async () => {
    try {
        months.value = await fetchLedgerMonths();
        period.value = resolveDefaultLedgerPeriod(months.value);
        if (!period.value) {
            throw new Error('조회 가능한 거래내역이 없습니다.');
        }
        await loadPeriod();
    } catch (err) {
        errorMessage.value = err.message ?? '거래내역을 불러오지 못했습니다.';
        loading.value = false;
    }
});
</script>

<template>
    <article class="ledger-view">
        <LedgerPageHeader v-model="searchTerm" />

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

            <LedgerSummaryCard :summary="summary" />

            <LedgerCalendar
                :period="period"
                :transactions-by-date="transactionsByDate"
                :selected-date="selectedDate"
                @select-date="selectDate"
            />

            <section class="ledger-view__list" aria-label="선택한 날짜의 거래내역">
                <StateEmpty
                    v-if="!currentDayGroup"
                    title="조건에 맞는 거래내역이 없어요"
                    description="날짜나 결제수단, 검색어를 바꿔서 다시 확인해 보세요."
                />
                <div v-else class="ledger-view__group">
                    <div class="ledger-view__group-header">
                        <h2>{{ formatDayLabel(currentDayGroup.date) }}</h2>
                        <button type="button" @click="openMonthTransactions">자세히보기 ›</button>
                    </div>
                    <ul class="ledger-view__group-rows">
                        <LedgerTransactionRow
                            v-for="tx in currentDayGroup.items"
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
            :methods="summary?.paymentMethods ?? []"
            :selected="selectedPaymentMethod"
            @select="selectPaymentMethod"
        />

        <LedgerCategorySheet
            v-model="isCategorySheetOpen"
            :transaction="selectedTransaction"
            @select="applyCategory"
        />

        <ChallengeReportToggle active="transactions" @open-monthly-report="openReport" />
    </article>
</template>

<style scoped>
.ledger-view {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
    height: calc(100vh - var(--tt-tabbar-height) - env(safe-area-inset-bottom) - var(--tt-space-4));
    padding: var(--tt-space-3) var(--tt-space-5) var(--tt-space-4);
    overflow-y: auto;
    background: var(--tt-bg-subtle);
}

.ledger-view__list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
}

.ledger-view__group {
    padding: var(--tt-space-3);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.ledger-view__group-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-2);
    margin-bottom: var(--tt-space-1);
}

.ledger-view__group-header h2 {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.ledger-view__group-header button {
    flex-shrink: 0;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    background: none;
    border: 0;
    cursor: pointer;
}

.ledger-view__group-rows {
    display: flex;
    flex-direction: column;
}

@media (max-width: 360px) {
    .ledger-view {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }
}
</style>
