<script setup>
import BaseBadge from '@/components/common/BaseBadge.vue';
import BaseCard from '@/components/common/BaseCard.vue';

defineProps({
    guiltyVotes: { type: Number, required: true },
    innocentVotes: { type: Number, required: true },
    outcome: { type: String, default: '' },
});
</script>

<template>
    <BaseCard class="tie-score" :class="outcome ? `tie-score--${outcome}` : ''" padding="md">
        <div class="tie-score__top">
            <div>
                <span class="tie-score__label">최종 투표</span>
                <strong class="tie-score__value">{{ guiltyVotes }} : {{ innocentVotes }}</strong>
            </div>
            <BaseBadge
                :variant="
                    outcome === 'GUILTY'
                        ? 'guilty'
                        : outcome === 'INNOCENT'
                          ? 'innocent'
                          : 'default'
                "
            >
                {{
                    outcome === 'GUILTY'
                        ? '유죄 확정'
                        : outcome === 'INNOCENT'
                          ? '무죄 확정'
                          : '동률'
                }}
            </BaseBadge>
        </div>
        <div class="tie-score__bar" aria-hidden="true">
            <span class="tie-score__bar-guilty"></span>
            <span class="tie-score__bar-innocent"></span>
        </div>
        <p v-if="!outcome" class="tie-score__caption">
            유죄 {{ guiltyVotes }}표 · 무죄 {{ innocentVotes }}표
        </p>
    </BaseCard>
</template>

<style scoped>
.tie-score__top {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: var(--tt-space-3);
}

.tie-score__label {
    display: block;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.tie-score__value {
    display: block;
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.05em;
    line-height: var(--tt-lh-tight);
}

.tie-score__bar {
    display: flex;
    height: 9px;
    margin-top: var(--tt-space-4);
    overflow: hidden;
    background: var(--tt-bg-fill);
    border-radius: var(--tt-radius-full);
}

.tie-score__bar-guilty,
.tie-score__bar-innocent {
    width: 50%;
}

.tie-score__bar-guilty {
    background: var(--tt-danger);
}

.tie-score__bar-innocent {
    background: var(--tt-success);
}

.tie-score__caption {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-align: center;
}

.tie-score--GUILTY {
    background: var(--tt-danger-subtle);
    border-color: var(--tt-danger);
}

.tie-score--GUILTY .tie-score__label,
.tie-score--GUILTY :deep(.tt-badge) {
    color: var(--tt-danger);
}

.tie-score--GUILTY .tie-score__bar-innocent {
    background: var(--tt-border-strong);
}

.tie-score--INNOCENT {
    background: var(--tt-success-subtle);
    border-color: var(--tt-success);
}

.tie-score--INNOCENT .tie-score__label,
.tie-score--INNOCENT :deep(.tt-badge) {
    color: var(--tt-success);
}

.tie-score--INNOCENT .tie-score__bar-innocent {
    order: -1;
}

.tie-score--INNOCENT .tie-score__bar-guilty {
    background: var(--tt-border-strong);
}

.tie-score :deep(.tt-badge) {
    padding: 0;
    border: 0;
    background: transparent;
    border-radius: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    transform: translateY(-4px);
}
</style>
