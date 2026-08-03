<!--
  용도: 사건기록부 카드. 사건번호(모노) · 제목 · 피고 N인 · 기간 · 누적 혐의액을 한 장으로 보여준다.
  언제 쓰는지: 재판 탭의 챌린지·사건 목록 항목. 목록에서 하나를 골라 상세로 들어가는 자리.
  쓰면 안 되는 경우: 판결이 끝난 건(JudgmentCard), 금액 명세를 줄줄이 보여줘야 할 때(ReceiptCard).
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';

const props = defineProps({
    caseNo: { type: String, required: true },
    title: { type: String, required: true },
    defendantCount: { type: Number, default: null },
    period: { type: String, default: '' },
    /* 누적 혐의액(원). 표시 형식은 이 컴포넌트가 정한다 — 호출부에서 포맷하지 말 것 */
    chargedAmount: { type: Number, default: null },
    clickable: { type: Boolean, default: true },
});

defineEmits(['click']);

const amountText = computed(() =>
    props.chargedAmount === null ? '' : `${props.chargedAmount.toLocaleString('ko-KR')}원`,
);

const metaText = computed(() =>
    [props.defendantCount === null ? '' : `피고 ${props.defendantCount}인`, props.period]
        .filter(Boolean)
        .join(' · '),
);
</script>

<template>
    <BaseCard variant="record" :clickable="clickable" @click="$emit('click')">
        <template #header>
            <div class="record__head">
                <span class="record__no">{{ caseNo }}</span>
                <slot name="badge" />
            </div>
            <p class="record__title">{{ title }}</p>
        </template>

        <p v-if="metaText" class="record__meta">{{ metaText }}</p>
        <slot />

        <template #footer>
            <div class="record__amount">
                <span class="record__amount-label">누적 혐의액</span>
                <span class="record__amount-value">{{ amountText }}</span>
            </div>
        </template>
    </BaseCard>
</template>

<style scoped>
.record__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-2);
}

.record__no {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-medium);
    letter-spacing: 0.04em;
    color: var(--tt-text-muted);
}

.record__title {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    line-height: var(--tt-lh-snug);
    color: var(--tt-text);
}

.record__meta {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.record__amount {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-3);
}

.record__amount-label {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.record__amount-value {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger);
}
</style>
