<!--
  용도: 조회 월의 지출·입금 합계와 전월 대비 변화율을 보여주는 네이비 카드.
  언제 쓰는지: LedgerView 한 곳에서만 렌더한다.
-->
<script setup>
import { computed } from 'vue';
import BaseBadge from '@/components/common/BaseBadge.vue';
import { formatWon } from '@/utils/ledger';

const props = defineProps({
    summary: { type: Object, required: true },
});

const changeCopy = computed(() => {
    const rate = props.summary.monthOverMonthRate;
    if (rate === 0) {
        return '지난달과 동일';
    }
    return `지난달 ${rate > 0 ? '+' : ''}${rate}%`;
});
</script>

<template>
    <section class="ledger-summary" aria-label="이번 달 지출·입금 요약">
        <div class="ledger-summary__col">
            <p>{{ Number(summary.period.slice(5)) }}월 지출</p>
            <strong>{{ formatWon(summary.totalSpent) }}</strong>
        </div>
        <div class="ledger-summary__col">
            <p>입금</p>
            <strong>{{ formatWon(summary.totalDeposit) }}</strong>
            <BaseBadge
                class="ledger-summary__change"
                :variant="summary.monthOverMonthRate > 0 ? 'guilty' : 'innocent'"
            >
                {{ changeCopy }}
            </BaseBadge>
        </div>
    </section>
</template>

<style scoped>
.ledger-summary {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--tt-space-4);
    padding: var(--tt-space-5);
    color: var(--tt-text-inverse);
    background: var(--tt-surface-strong);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
}

.ledger-summary__col p {
    font-size: var(--tt-fs-caption);
    color: var(--tt-border-strong);
}

.ledger-summary__col strong {
    display: block;
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    white-space: nowrap;
}

.ledger-summary__change {
    margin-top: var(--tt-space-2);
}
</style>
