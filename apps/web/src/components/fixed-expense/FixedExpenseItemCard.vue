<script setup>
import BaseBadge from '@/components/common/BaseBadge.vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { formatWon } from '@/utils/fixedExpense';

const props = defineProps({
    item: { type: Object, required: true },
    candidate: { type: Boolean, default: false },
});

defineEmits(['select']);

function accessibleLabel() {
    const status = props.candidate ? '탐지 후보' : '확정 고정지출';
    return `${status} ${props.item.name}, ${formatWon(props.item.averageAmount)}`;
}
</script>

<template>
    <BaseCard
        class="fixed-item"
        :class="{ 'fixed-item--candidate': candidate }"
        padding="none"
        clickable
        :aria-label="accessibleLabel()"
        @click="$emit('select', item.id)"
    >
        <span class="fixed-item__holes" aria-hidden="true"></span>
        <span class="fixed-item__category" :class="`fixed-item__category--${item.categoryCode}`">
            {{ item.categoryLabel }}
        </span>
        <span class="fixed-item__copy">
            <strong>{{ item.name }}</strong>
            <small>
                {{
                    candidate
                        ? item.description
                        : `${item.category} · ${item.billingCycle.type === 'monthly' ? `매월 ${item.billingCycle.day}일` : ''}`
                }}
            </small>
        </span>
        <span class="fixed-item__amount">
            <strong>{{ formatWon(item.averageAmount) }}</strong>
            <BaseBadge
                v-if="!candidate && item.paymentLabel?.startsWith('D-')"
                class="fixed-item__deadline"
            >
                {{ item.paymentLabel }}
            </BaseBadge>
            <small v-else>{{ item.paymentLabel ?? '평균 · 월' }}</small>
        </span>
    </BaseCard>
</template>

<style scoped>
.fixed-item {
    position: relative;
    overflow: hidden;
    min-height: 80px;
    border-radius: var(--tt-radius-md);
    box-shadow: var(--tt-elevation-1);
}

.fixed-item:deep(.tt-card__body) {
    position: relative;
    z-index: var(--tt-z-base);
    display: grid;
    grid-template-columns: 52px 1fr auto;
    align-items: center;
    gap: var(--tt-space-3);
    min-height: 80px;
    padding: var(--tt-space-4) var(--tt-space-4) var(--tt-space-4) 38px;
    background: linear-gradient(
        to bottom,
        var(--tt-bg) 0,
        var(--tt-bg) 40%,
        var(--tt-surface-row) 40%,
        var(--tt-surface-row) 100%
    );
}

.fixed-item--candidate {
    border-color: var(--tt-border-candidate);
}

.fixed-item--candidate:deep(.tt-card__body) {
    background: linear-gradient(
        to bottom,
        var(--tt-surface-candidate) 0,
        var(--tt-surface-candidate) 40%,
        var(--tt-surface-candidate-strong) 40%,
        var(--tt-surface-candidate-strong) 100%
    );
}

.fixed-item__holes::before,
.fixed-item__holes::after {
    position: absolute;
    left: 13px;
    z-index: 1;
    width: 10px;
    height: 10px;
    content: '';
    background: var(--tt-surface-row);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    box-shadow: inset 0 1px 2px var(--tt-border-strong);
}

.fixed-item__holes {
    position: absolute;
}

.fixed-item__holes::before {
    top: 26px;
}

.fixed-item__holes::after {
    top: 47px;
}

.fixed-item--candidate .fixed-item__holes::before,
.fixed-item--candidate .fixed-item__holes::after {
    background: var(--tt-surface-candidate-strong);
    border-color: var(--tt-border-candidate);
}

.fixed-item__category {
    display: grid;
    width: 42px;
    height: 42px;
    place-items: center;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    border-radius: var(--tt-radius-full);
}

.fixed-item__category--subscription {
    color: var(--tt-accent-strong);
    background: var(--tt-category-subscription);
}

.fixed-item__category--living {
    color: var(--tt-success);
    background: var(--tt-category-living);
}

.fixed-item__copy,
.fixed-item__amount,
.fixed-item__copy strong,
.fixed-item__copy small,
.fixed-item__amount strong,
.fixed-item__amount small {
    display: block;
}

.fixed-item__copy strong,
.fixed-item__amount strong {
    font-weight: var(--tt-fw-black);
}

.fixed-item__copy small,
.fixed-item__amount small {
    color: var(--tt-text-subtle);
}

.fixed-item--candidate .fixed-item__copy small,
.fixed-item--candidate .fixed-item__amount small {
    color: var(--tt-accent-strong);
}

.fixed-item__amount {
    text-align: right;
}

.fixed-item__amount strong {
    font-family: var(--tt-font-mono);
    white-space: nowrap;
}

.fixed-item__amount small {
    margin-top: var(--tt-space-1);
}

.fixed-item__deadline {
    margin-top: var(--tt-space-1);
    color: var(--tt-danger);
    background: var(--tt-danger-subtle);
}

@media (max-width: 360px) {
    .fixed-item:deep(.tt-card__body) {
        grid-template-columns: 40px 1fr auto;
        gap: var(--tt-space-2);
        padding-right: var(--tt-space-3);
    }

    .fixed-item__category {
        width: 36px;
        height: 36px;
    }

    .fixed-item__copy small,
    .fixed-item__amount small {
        font-size: var(--tt-fs-mono-chip);
    }
}
</style>
