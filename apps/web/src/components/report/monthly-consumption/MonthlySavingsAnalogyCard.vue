<script setup>
import { computed } from 'vue';
import encouragingTangi from '@/assets/images/emotions/42_thumbs_up.png';
import concernedTangi from '@/assets/images/emotions/16_worried.png';
import { resolveMonthlySavingsAnalogyCard } from '@/utils/monthlyConsumption';

const props = defineProps({
    report: { type: Object, required: true },
});

const card = computed(() => resolveMonthlySavingsAnalogyCard(props.report));
const tangiImage = computed(() =>
    card.value.variant === 'increase' ? concernedTangi : encouragingTangi,
);
const tangiAlt = computed(() =>
    card.value.variant === 'increase' ? '소비 흐름을 살피는 탕이' : '절약을 응원하는 탕이',
);
</script>

<template>
    <aside
        class="savings-analogy-card"
        :class="`savings-analogy-card--${card.variant}`"
        aria-labelledby="savings-analogy-title"
    >
        <div class="savings-analogy-card__content">
            <p class="savings-analogy-card__eyebrow">{{ card.eyebrow }}</p>
            <h2 id="savings-analogy-title">{{ card.title }}</h2>
            <p class="savings-analogy-card__description">{{ card.description }}</p>
        </div>
        <img :src="tangiImage" :alt="tangiAlt" />
    </aside>
</template>

<style scoped>
.savings-analogy-card {
    position: relative;
    min-height: 152px;
    overflow: hidden;
    padding: var(--tt-space-5) 138px var(--tt-space-5) var(--tt-space-5);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
}
.savings-analogy-card::after {
    position: absolute;
    right: -36px;
    bottom: -72px;
    width: 184px;
    height: 184px;
    content: '';
    border-radius: var(--tt-radius-full);
}
.savings-analogy-card__content {
    position: relative;
    z-index: 1;
}
.savings-analogy-card__eyebrow {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
}
.savings-analogy-card h2 {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-section);
    line-height: var(--tt-lh-normal);
    font-weight: var(--tt-fw-black);
    word-break: keep-all;
}
.savings-analogy-card__description {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    word-break: keep-all;
}
.savings-analogy-card img {
    position: absolute;
    right: var(--tt-space-2);
    bottom: -4px;
    z-index: 1;
    width: 134px;
    height: 134px;
    object-fit: contain;
}
.savings-analogy-card--saving {
    background: linear-gradient(
        135deg,
        var(--tt-bg),
        color-mix(in srgb, var(--tt-success) 14%, var(--tt-bg))
    );
    border-color: color-mix(in srgb, var(--tt-success) 36%, var(--tt-border));
}
.savings-analogy-card--saving::after {
    background: color-mix(in srgb, var(--tt-success) 18%, transparent);
}
.savings-analogy-card--saving .savings-analogy-card__eyebrow,
.savings-analogy-card--saving .savings-analogy-card__description {
    color: var(--tt-success);
}
.savings-analogy-card--increase {
    background: linear-gradient(
        135deg,
        var(--tt-bg),
        color-mix(in srgb, var(--tt-danger) 12%, var(--tt-bg))
    );
    border-color: color-mix(in srgb, var(--tt-danger) 30%, var(--tt-border));
}
.savings-analogy-card--increase::after {
    background: color-mix(in srgb, var(--tt-danger) 15%, transparent);
}
.savings-analogy-card--increase .savings-analogy-card__eyebrow,
.savings-analogy-card--increase .savings-analogy-card__description {
    color: var(--tt-danger);
}
.savings-analogy-card--start {
    background: linear-gradient(135deg, var(--tt-bg), var(--tt-accent-subtle));
    border-color: var(--tt-accent-subtle-border);
}
.savings-analogy-card--start::after {
    background: color-mix(in srgb, var(--tt-accent) 18%, transparent);
}
.savings-analogy-card--start .savings-analogy-card__eyebrow,
.savings-analogy-card--start .savings-analogy-card__description {
    color: var(--tt-accent-strong);
}
@media (max-width: 360px) {
    .savings-analogy-card {
        padding-right: 112px;
    }
    .savings-analogy-card img {
        right: -4px;
        width: 120px;
        height: 120px;
    }
}
</style>
