<script setup>
import BaseBadge from '@/components/common/BaseBadge.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { formatBillingCycle, formatDday, formatShortDate, formatWon } from '@/utils/fixedExpense';

defineProps({
    mode: {
        type: String,
        required: true,
        validator: (value) => ['savings', 'overview', 'detail'].includes(value),
    },
    data: { type: Object, required: true },
    referenceDate: { type: Date, default: () => new Date() },
});
</script>

<template>
    <BaseCard class="fixed-summary" padding="none">
        <template v-if="mode === 'savings'">
            <div class="fixed-summary__eyebrow">SAVINGS VERDICT · 고정지출 정리</div>
            <div class="fixed-summary__body fixed-summary__body--savings">
                <p class="fixed-summary__label">지금 정리하면 매월</p>
                <p class="fixed-summary__amount">{{ formatWon(data.monthlySavings) }}</p>
                <strong class="fixed-summary__saving-year">
                    1년이면 {{ formatWon(data.yearlySavings) }} 무혐의
                </strong>
            </div>
        </template>

        <div v-else-if="mode === 'overview'" class="fixed-summary__body">
            <p class="fixed-summary__label">이번 달 예상 고정지출</p>
            <p class="fixed-summary__amount">{{ formatWon(data.expectedMonthlyAmount) }}</p>
            <p class="fixed-summary__counts">
                확정 <strong>{{ data.confirmedCount }}건</strong>
                <span
                    >탐지 후보 <strong>{{ data.candidateCount }}건</strong></span
                >
            </p>
        </div>

        <div v-else class="fixed-summary__body fixed-summary__body--detail">
            <div class="fixed-summary__identity">
                <span class="fixed-summary__category">{{ data.categoryLabel }}</span>
                <span>
                    <strong>{{ data.name }}</strong>
                    <small>{{ data.category }} · {{ data.description }}</small>
                </span>
            </div>
            <dl class="fixed-summary__facts">
                <div>
                    <dt>평균 금액</dt>
                    <dd>{{ formatWon(data.averageAmount) }}</dd>
                </div>
                <div>
                    <dt>결제 주기</dt>
                    <dd>{{ formatBillingCycle(data.billingCycle).replace(/ \d+일$/, '') }}</dd>
                </div>
                <div>
                    <dt>다음 결제일</dt>
                    <dd class="fixed-summary__next-date">
                        {{ formatShortDate(data.nextPaymentDate) }}
                        <BaseBadge class="fixed-summary__dday">
                            {{ formatDday(data.nextPaymentDate, referenceDate) }}
                        </BaseBadge>
                    </dd>
                </div>
            </dl>
        </div>
    </BaseCard>
</template>

<style scoped>
.fixed-summary {
    overflow: hidden;
    color: var(--tt-text-inverse);
    background: var(--tt-surface-strong);
    border: 0;
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-3);
}

.fixed-summary__eyebrow {
    padding: var(--tt-space-6) var(--tt-space-5) var(--tt-space-5);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.22em;
    text-align: center;
    color: var(--tt-accent);
    background: var(--tt-surface-strong-deep);
}

.fixed-summary__body {
    padding: var(--tt-space-5);
}

.fixed-summary__body--savings {
    padding-top: var(--tt-space-3);
    padding-bottom: var(--tt-space-6);
}

.fixed-summary__label {
    color: var(--tt-text-soft);
}

.fixed-summary__amount {
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-display);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    font-variant-numeric: tabular-nums;
}

.fixed-summary__saving-year {
    display: inline-block;
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-2) var(--tt-space-3);
    color: var(--tt-surface-strong);
    background: var(--tt-accent);
    border-radius: var(--tt-radius-full);
}

.fixed-summary__counts {
    color: var(--tt-text-soft);
}

.fixed-summary__counts strong {
    color: var(--tt-success-bright);
}

.fixed-summary__counts span {
    margin-left: var(--tt-space-4);
}

.fixed-summary__counts span strong {
    color: var(--tt-accent);
}

.fixed-summary__body--detail {
    padding: var(--tt-space-5);
}

.fixed-summary__identity {
    display: flex;
    align-items: center;
    gap: var(--tt-space-4);
}

.fixed-summary__identity strong,
.fixed-summary__identity small {
    display: block;
}

.fixed-summary__identity strong {
    font-size: var(--tt-fs-section);
}

.fixed-summary__identity small {
    margin-top: var(--tt-space-1);
    color: var(--tt-text-soft);
}

.fixed-summary__category {
    display: grid;
    width: 52px;
    height: 52px;
    place-items: center;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    background: var(--tt-surface-strong-muted);
    border-radius: var(--tt-radius-md);
}

.fixed-summary__facts {
    display: grid;
    grid-template-columns: 0.9fr 0.8fr 1.3fr;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-5);
}

.fixed-summary__facts dt {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-soft);
}

.fixed-summary__facts dd {
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-black);
    white-space: nowrap;
}

.fixed-summary__next-date {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.fixed-summary__dday {
    color: var(--tt-surface-strong);
    background: var(--tt-accent);
}

@media (max-width: 360px) {
    .fixed-summary__amount {
        font-size: var(--tt-fs-numeric);
    }

    .fixed-summary__facts {
        gap: var(--tt-space-2);
    }

    .fixed-summary__facts dd {
        font-size: var(--tt-fs-caption);
    }
}
</style>
