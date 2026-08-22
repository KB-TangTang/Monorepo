<!--
  용도: 홈 「다가오는 고정지출」. 결제일이 가까운 확정 고정지출 3건과 이번 달 예상 합계를 보여주고
        고정지출 관리 화면으로 보낸다.
  D-day · 금액 · 결제 주기 표기는 고정지출 화면과 같은 utils/fixedExpense.js 를 쓴다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { formatBillingCycle, formatDday, formatWon } from '@/utils/fixedExpense';
import { pickUpcomingFixedExpenses } from '@/utils/home';

/* 행마다 다른 색을 돌려 세 건이 한눈에 구분되게 한다 — 자산 종류처럼 의미가 있는 색은 아니다. */
const ROW_TONES = ['info', 'success', 'accent'];

const props = defineProps({
    /* 확정된 고정지출 목록(FixedExpenseOverviewDto.confirmed). */
    items: { type: Array, default: () => [] },
    /* summary.expectedMonthlyAmount — 이번 달 예상 합계. 못 불러왔으면 null. */
    monthlyAmount: { type: Number, default: null },
    referenceDate: { type: Date, default: () => new Date() },
});

defineEmits(['open-all', 'open-item']);

const upcoming = computed(() =>
    pickUpcomingFixedExpenses(props.items, { referenceDate: props.referenceDate }),
);
</script>

<template>
    <section class="fixed-upcoming" aria-labelledby="home-fixed-title">
        <div class="fixed-upcoming__head">
            <h2 id="home-fixed-title" class="fixed-upcoming__title">다가오는 고정지출</h2>

            <button type="button" class="fixed-upcoming__total" @click="$emit('open-all')">
                <template v-if="monthlyAmount === null">전체 보기 ›</template>
                <template v-else>월 {{ formatWon(monthlyAmount) }} ›</template>
            </button>
        </div>

        <BaseCard v-if="upcoming.length" class="fixed-upcoming__card" padding="none">
            <ul class="fixed-upcoming__list">
                <li v-for="(item, index) in upcoming" :key="item.id">
                    <button type="button" class="fixed-row" @click="$emit('open-item', item.id)">
                        <span
                            class="fixed-row__badge"
                            :class="`fixed-row__badge--${ROW_TONES[index % ROW_TONES.length]}`"
                            aria-hidden="true"
                        >
                            {{ item.name.charAt(0) }}
                        </span>

                        <span class="fixed-row__body">
                            <span class="fixed-row__name">{{ item.name }}</span>
                            <span class="fixed-row__meta">
                                {{ item.category }} · {{ formatBillingCycle(item.billingCycle) }}
                            </span>
                            <span class="fixed-row__due">
                                {{ formatDday(item.nextPaymentDate, referenceDate) }} ·
                                {{ formatWon(item.averageAmount) }}
                            </span>
                        </span>

                        <span class="fixed-row__chevron" aria-hidden="true">›</span>
                    </button>
                </li>
            </ul>
        </BaseCard>

        <BaseCard v-else class="fixed-upcoming__card" padding="md">
            <p class="fixed-upcoming__empty">
                아직 확정된 고정지출이 없어요. 탐지된 후보를 확인해 보세요.
            </p>
        </BaseCard>
    </section>
</template>

<style scoped>
.fixed-upcoming__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    margin-bottom: var(--tt-space-2);
    padding: 0 var(--tt-space-1);
}

.fixed-upcoming__title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
}

.fixed-upcoming__total {
    flex: none;
    border: 0;
    background: none;
    color: var(--tt-text-muted);
    font-family: inherit;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    cursor: pointer;
}

.fixed-upcoming__card {
    padding: 0 var(--tt-space-4);
    border-radius: var(--tt-radius-xl);
}

.fixed-upcoming__list {
    list-style: none;
}

.fixed-upcoming__list > li + li {
    border-top: 1px solid var(--tt-border-light);
}

.fixed-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    width: 100%;
    padding: var(--tt-space-3) 0;
    border: 0;
    background: none;
    font-family: inherit;
    text-align: left;
    cursor: pointer;
}

.fixed-row__badge {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    width: 38px;
    height: 38px;
    border-radius: var(--tt-radius-full);
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
}

.fixed-row__badge--info {
    background: var(--tt-info-subtle);
    color: var(--tt-info);
}

.fixed-row__badge--success {
    background: var(--tt-success-subtle);
    color: var(--tt-success);
}

.fixed-row__badge--accent {
    background: var(--tt-accent-subtle);
    color: var(--tt-accent-warn);
}

.fixed-row__body {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex: 1;
    min-width: 0;
}

.fixed-row__name {
    overflow: hidden;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    white-space: nowrap;
    text-overflow: ellipsis;
}

.fixed-row__meta {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.fixed-row__due {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-info);
}

.fixed-row__chevron {
    flex: none;
    color: var(--tt-text-hint);
    font-size: var(--tt-fs-label);
}

.fixed-upcoming__empty {
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}
</style>
