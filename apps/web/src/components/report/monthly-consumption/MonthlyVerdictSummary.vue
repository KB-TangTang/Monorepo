<script setup>
import { computed } from 'vue';
import { formatWon } from '@/utils/monthlyConsumption';

const props = defineProps({
    report: { type: Object, required: true },
    showComparison: { type: Boolean, default: true },
});
const reportMonth = computed(() => Number(props.report.period.slice(5)));
const verdictCopy = computed(() => {
    if (props.report.monthOverMonthRate == null) {
        return '지난달 소비가 0원이라 비교하기 어려워요';
    }
    return props.report.monthOverMonthRate <= 0
        ? `지난달보다 ${Math.abs(props.report.monthOverMonthRate)}% 절약`
        : `지난달보다 ${props.report.monthOverMonthRate}% 증가`;
});
</script>

<template>
    <section class="verdict-summary" aria-label="월간 소비 판결문 요약">
        <header>
            {{ reportMonth }}월 소비 판결문&nbsp; · &nbsp;{{ report.period.replace('-', '.') }}
        </header>
        <div class="verdict-summary__body">
            <p>이번 달 누적 혐의액</p>
            <strong>{{ formatWon(report.totalSpent) }}</strong>
            <mark
                v-if="showComparison"
                :class="{ 'verdict-summary__mark--guilty': report.monthOverMonthRate > 0 }"
            >
                {{ verdictCopy }}
            </mark>
            <p v-else class="verdict-summary__basis">
                {{ reportMonth }}월 1일부터 말일까지의 소비 내역을 분석했어요
            </p>
        </div>
    </section>
</template>

<style scoped>
.verdict-summary {
    overflow: hidden;
    color: var(--tt-text-inverse);
    background: color-mix(in srgb, var(--tt-text) 92%, var(--tt-bg));
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-3);
}
.verdict-summary header {
    padding: var(--tt-space-4);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-accent);
    text-align: center;
    letter-spacing: 0.12em;
    background: var(--tt-text);
    border-radius: 0 0 var(--tt-radius-full) var(--tt-radius-full);
}
.verdict-summary__body {
    padding: var(--tt-space-6);
}
.verdict-summary p {
    color: var(--tt-border-strong);
}
.verdict-summary strong {
    display: block;
    margin: var(--tt-space-2) 0 var(--tt-space-4);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    letter-spacing: -0.06em;
}
.verdict-summary mark {
    display: inline-block;
    padding: var(--tt-space-3) var(--tt-space-5);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    background: var(--tt-accent);
    border-radius: var(--tt-radius-full);
}
.verdict-summary__mark--guilty {
    color: var(--tt-text-inverse);
    background: var(--tt-danger);
}
.verdict-summary .verdict-summary__basis {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    margin-top: var(--tt-space-4);
    padding-top: var(--tt-space-4);
    color: var(--tt-border-strong);
    font-size: var(--tt-fs-caption);
    border-top: 1px solid color-mix(in srgb, var(--tt-bg) 12%, transparent);
}
.verdict-summary__basis::before {
    display: grid;
    flex: 0 0 24px;
    width: 24px;
    height: 24px;
    content: '1';
    color: var(--tt-text);
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-black);
    background: var(--tt-accent);
    border-radius: var(--tt-radius-full);
    place-items: center;
}
</style>
