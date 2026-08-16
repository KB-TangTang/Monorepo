<!--
  용도: 거래 상세내역 화면의 검색창을 눌렀을 때 진입하는 조건 검색 화면.
  가맹점(검색어) · 카테고리 · 금액 범위 · 기간 조건을 조합해 데이터가 있는 모든 월의 거래를 훑는다.
  결과는 조건을 바꿀 때마다 이 화면 안에서 즉시 갱신된다 — "이 조건으로 검색" 은 화면을 벗어나지 않는다.
  금액 슬라이더의 상·하한은 전체 기간 절대값이 아니라 **선택한 기간 안에서의** 최고·최저 금액이다
  (periodFilteredTransactions 참고). 기간을 바꾸면 슬라이더 범위도 그 기간 기준으로 다시 채워진다.
  언제 쓰는지: router/index.js 의 /ledger/search (name: ledgerSearch) 라우트.
-->
<script setup>
import { computed, nextTick, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { fetchLedgerMonths, fetchLedgerSearchTransactions } from '@/api/ledger';
import BaseButton from '@/components/common/BaseButton.vue';
import LedgerAmountRangeSlider from '@/components/ledger/LedgerAmountRangeSlider.vue';
import LedgerCategoryFilterSheet from '@/components/ledger/LedgerCategoryFilterSheet.vue';
import LedgerSearchConditionChips from '@/components/ledger/LedgerSearchConditionChips.vue';
import LedgerSearchPeriodSheet from '@/components/ledger/LedgerSearchPeriodSheet.vue';
import LedgerTransactionRow from '@/components/ledger/LedgerTransactionRow.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import { findExpenseParentByName } from '@/utils/category';
import {
    filterTransactionsByTab,
    formatMonthLabel,
    formatShortDayLabel,
    formatWon,
    resolveAmountBounds,
    resolveLedgerState,
} from '@/utils/ledger';

const DIRECTION_TABS = [
    { value: 'ALL', label: '전체' },
    { value: 'CONSUMPTION', label: '지출' },
    { value: 'INCOME', label: '입금' },
];

const router = useRouter();

const months = ref([]);
const allTransactions = ref([]);
const loading = ref(true);
const errorMessage = ref('');

const searchText = ref('');
const category = ref('');
const isCategorySheetOpen = ref(false);
const directionTab = ref('ALL');

const amountMin = ref(0);
const amountMax = ref(0);

const periodFrom = ref('');
const periodTo = ref('');
const isPeriodSheetOpen = ref(false);

const resultsHeading = ref(null);

const state = computed(() =>
    resolveLedgerState({ loading: loading.value, error: errorMessage.value, data: allTransactions.value }),
);

const periodLabel = computed(() => {
    if (!periodFrom.value) {
        return '기간 선택';
    }
    if (periodFrom.value === periodTo.value) {
        return formatMonthLabel(periodFrom.value);
    }
    return `${formatMonthLabel(periodFrom.value)} ~ ${formatMonthLabel(periodTo.value)}`;
});

/* 기간 조건까지만 적용한 목록. 금액 슬라이더 상·하한을 "선택한 기간 안에서의 최고/최저 금액"으로
 * 좁히는 데 쓴다 — 전체 기간 절대값이 아니라 실제로 검색 대상인 기간을 기준으로 보여줘야 한다. */
const periodFilteredTransactions = computed(() => {
    let list = allTransactions.value;
    if (periodFrom.value) {
        list = list.filter((tx) => tx.date.slice(0, 7) >= periodFrom.value);
    }
    if (periodTo.value) {
        list = list.filter((tx) => tx.date.slice(0, 7) <= periodTo.value);
    }
    return list;
});

const amountBounds = computed(() => resolveAmountBounds(periodFilteredTransactions.value));

const filteredResults = computed(() => {
    const term = searchText.value.trim();
    let list = filterTransactionsByTab(periodFilteredTransactions.value, directionTab.value);

    if (term !== '') {
        list = list.filter((tx) => (tx.merchant ?? '').includes(term));
    }
    if (category.value) {
        list = list.filter(
            (tx) =>
                tx.category === category.value ||
                findExpenseParentByName(tx.category)?.name === category.value,
        );
    }
    list = list.filter((tx) => {
        const abs = Math.abs(tx.amount);
        return abs >= amountMin.value && abs <= amountMax.value;
    });

    return [...list].sort((a, b) => b.date.localeCompare(a.date));
});

/* TRANSFER(계좌간 이체)는 지출도 입금도 아니라 합계에서 뺀다 — 백엔드 summary.totalSpent/
 * totalDeposit과 같은 규칙(TransactionQueryService.resolveSignedAmount 주석 참고). 이걸 빼지
 * 않으면 "전체" 탭 합계가 "지출" 탭 합계 + "입금" 탭 합계와 안 맞아 보인다(이체만큼 차이 남). */
const resultsTotal = computed(() =>
    filteredResults.value
        .filter((tx) => tx.classification !== 'TRANSFER')
        .reduce((sum, tx) => sum + tx.amount, 0),
);

function resetAmountRange() {
    amountMin.value = amountBounds.value.min;
    amountMax.value = amountBounds.value.max;
}

function resetPeriodRange() {
    const withData = months.value.filter((month) => month.hasData).map((month) => month.value).sort();
    periodFrom.value = withData[0] ?? '';
    periodTo.value = withData[withData.length - 1] ?? '';
}

function resetAll() {
    searchText.value = '';
    category.value = '';
    directionTab.value = 'ALL';
    /* 기간을 먼저 되돌려야 amountBounds(기간 내 최고·최저 금액)가 새 기간 기준으로 다시 계산된
     * 뒤에 금액 범위를 그 값으로 채운다 — 순서를 바꾸면 옛 기간의 상한이 잠깐 남는다. */
    resetPeriodRange();
    resetAmountRange();
}

function goBack() {
    router.back();
}

function removeMerchant() {
    searchText.value = '';
}

function selectCategory(name) {
    category.value = name;
}

function removeCategory() {
    category.value = '';
}

function selectDirection(tab) {
    directionTab.value = tab;
}

function removeDirection() {
    directionTab.value = 'ALL';
}

function onPeriodSelect({ from, to }) {
    periodFrom.value = from;
    periodTo.value = to;
    /* 기간이 바뀌면 그 기간 안의 최고·최저 금액으로 슬라이더를 다시 채운다. */
    resetAmountRange();
}

async function confirmSearch() {
    await nextTick();
    resultsHeading.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
}

async function loadData() {
    loading.value = true;
    errorMessage.value = '';
    try {
        months.value = await fetchLedgerMonths();
        allTransactions.value = await fetchLedgerSearchTransactions();
        resetPeriodRange();
        resetAmountRange();
    } catch (err) {
        errorMessage.value = err.message ?? '거래내역을 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

onMounted(loadData);
</script>

<template>
    <article class="ledger-search-view">
        <div class="ledger-search-view__fixed">
            <header class="ledger-search-view__header">
                <button type="button" class="ledger-search-view__back" aria-label="뒤로가기" @click="goBack">
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="m15 4-8 8 8 8" />
                    </svg>
                </button>

                <div class="ledger-search-view__field">
                    <svg class="ledger-search-view__field-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <circle cx="11" cy="11" r="7" />
                        <path d="m20 20-3.5-3.5" />
                    </svg>
                    <input
                        v-model="searchText"
                        type="text"
                        class="ledger-search-view__field-input"
                        placeholder="가맹점명을 검색해보세요"
                        aria-label="가맹점명 검색"
                    />
                </div>

                <button type="button" class="ledger-search-view__cancel" @click="goBack">취소</button>
            </header>

            <template v-if="state === 'ready'">
                <LedgerSearchConditionChips
                    :merchant-term="searchText.trim()"
                    :category="category"
                    :direction="directionTab"
                    @remove-merchant="removeMerchant"
                    @open-category="isCategorySheetOpen = true"
                    @remove-category="removeCategory"
                    @remove-direction="removeDirection"
                />

                <section class="ledger-search-view__condition">
                    <h2>구분</h2>
                    <div class="ledger-search-view__direction" role="tablist" aria-label="수입/지출 구분">
                        <button
                            v-for="tab in DIRECTION_TABS"
                            :key="tab.value"
                            type="button"
                            role="tab"
                            class="ledger-search-view__direction-pill"
                            :class="{
                                'ledger-search-view__direction-pill--active': directionTab === tab.value,
                            }"
                            :aria-selected="directionTab === tab.value"
                            @click="selectDirection(tab.value)"
                        >
                            {{ tab.label }}
                        </button>
                    </div>
                </section>

                <section class="ledger-search-view__condition">
                    <h2>금액 조건</h2>
                    <LedgerAmountRangeSlider
                        v-model:model-min="amountMin"
                        v-model:model-max="amountMax"
                        :min="amountBounds.min"
                        :max="amountBounds.max"
                    />
                </section>

                <section class="ledger-search-view__condition">
                    <h2>기간</h2>
                    <button
                        type="button"
                        class="ledger-search-view__period"
                        @click="isPeriodSheetOpen = true"
                    >
                        <span>{{ periodLabel }}</span>
                        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m9 5 7 7-7 7" /></svg>
                    </button>
                </section>
            </template>
        </div>

        <StateLoading v-if="state === 'loading'" size="lg" message="거래내역을 불러오는 중" />
        <StateError v-else-if="state === 'error'" :message="errorMessage" @retry="loadData" />
        <section v-else-if="state === 'ready'" class="ledger-search-view__list" aria-label="검색 결과">
            <div ref="resultsHeading" class="ledger-search-view__list-header">
                <h2>검색 결과 {{ filteredResults.length }}건</h2>
                <span :class="{ 'ledger-search-view__list-total--income': resultsTotal > 0 }">
                    합계 {{ formatWon(resultsTotal) }}
                </span>
            </div>

            <StateEmpty
                v-if="filteredResults.length === 0"
                title="조건에 맞는 거래내역이 없어요"
                description="조건을 조정해서 다시 검색해 보세요."
            />
            <ul v-else class="ledger-search-view__rows">
                <LedgerTransactionRow
                    v-for="tx in filteredResults"
                    :key="tx.id"
                    :transaction="tx"
                    :subtitle="`${formatShortDayLabel(tx.date)} · ${tx.paymentMethod}`"
                />
            </ul>
        </section>

        <footer v-if="state === 'ready'" class="ledger-search-view__actions">
            <BaseButton variant="ghost" @click="resetAll">초기화</BaseButton>
            <BaseButton variant="primary" block @click="confirmSearch">이 조건으로 검색</BaseButton>
        </footer>

        <LedgerCategoryFilterSheet
            v-model="isCategorySheetOpen"
            :selected="category"
            @select="selectCategory"
        />

        <LedgerSearchPeriodSheet
            v-model="isPeriodSheetOpen"
            :months="months"
            :from="periodFrom"
            :to="periodTo"
            @select="onPeriodSelect"
        />
    </article>
</template>

<style scoped>
.ledger-search-view {
    display: flex;
    flex-direction: column;
    height: calc(100vh - var(--tt-tabbar-height) - env(safe-area-inset-bottom) - var(--tt-space-4));
    padding: var(--tt-space-3) var(--tt-space-5) 0;
    background: var(--tt-bg-subtle);
}

.ledger-search-view__fixed {
    display: flex;
    flex-direction: column;
    flex-shrink: 0;
    gap: var(--tt-space-3);
    padding-bottom: var(--tt-space-3);
}

.ledger-search-view__header {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.ledger-search-view__back {
    display: grid;
    flex: 0 0 32px;
    width: 32px;
    height: 40px;
    padding: 0;
    color: var(--tt-text);
    background: transparent;
    border: 0;
    cursor: pointer;
    place-items: center;
}

.ledger-search-view__back svg {
    width: 24px;
    height: 24px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.2;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.ledger-search-view__field {
    display: flex;
    flex: 1;
    align-items: center;
    min-width: 0;
    padding: 0 var(--tt-space-3);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.ledger-search-view__field-icon {
    flex-shrink: 0;
    width: 18px;
    height: 18px;
    margin-right: var(--tt-space-2);
    color: var(--tt-text-muted);
    fill: none;
    stroke: currentColor;
    stroke-width: 2;
    stroke-linecap: round;
}

.ledger-search-view__field-input {
    flex: 1;
    min-width: 0;
    padding: var(--tt-space-3) 0;
    font-family: inherit;
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
    background: transparent;
    border: 0;
    outline: none;
}

.ledger-search-view__cancel {
    flex-shrink: 0;
    padding: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    cursor: pointer;
    background: transparent;
    border: 0;
}

.ledger-search-view__condition {
    padding: var(--tt-space-3);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.ledger-search-view__condition h2 {
    margin-bottom: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.ledger-search-view__direction {
    display: flex;
    gap: var(--tt-space-2);
}

.ledger-search-view__direction-pill {
    flex: 1 1 0;
    padding: var(--tt-space-2) var(--tt-space-1);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    text-align: center;
    cursor: pointer;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.ledger-search-view__direction-pill--active {
    color: var(--tt-text-inverse);
    background: var(--tt-primary);
    border-color: var(--tt-primary);
}

.ledger-search-view__period {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding: var(--tt-space-1) 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    cursor: pointer;
    background: transparent;
    border: 0;
}

.ledger-search-view__period svg {
    width: 16px;
    height: 16px;
    color: var(--tt-text-muted);
    fill: none;
    stroke: currentColor;
    stroke-width: 2.2;
    stroke-linecap: round;
    stroke-linejoin: round;
}

.ledger-search-view__list {
    display: flex;
    flex: 1 1 auto;
    flex-direction: column;
    min-height: 0;
    padding: var(--tt-space-3);
    overflow-y: auto;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.ledger-search-view__list-header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-2);
    margin-bottom: var(--tt-space-1);
    scroll-margin-top: var(--tt-space-3);
}

.ledger-search-view__list-header h2 {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.ledger-search-view__list-header span {
    flex-shrink: 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger);
}

.ledger-search-view__list-header .ledger-search-view__list-total--income {
    color: var(--tt-success);
}

.ledger-search-view__rows {
    display: flex;
    flex-direction: column;
}

.ledger-search-view__actions {
    display: flex;
    flex-shrink: 0;
    gap: var(--tt-space-2);
    padding: var(--tt-space-3) 0 calc(var(--tt-space-3) + env(safe-area-inset-bottom));
}

.ledger-search-view__actions > :first-child {
    flex-shrink: 0;
    white-space: nowrap;
}

@media (max-width: 360px) {
    .ledger-search-view {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }
}
</style>
