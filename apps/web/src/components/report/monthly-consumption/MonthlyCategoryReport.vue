<script setup>
import { computed, ref, watch } from 'vue';
import { formatChangeRate, formatWon, splitMonthlyCategories } from '@/utils/monthlyConsumption';

const props = defineProps({
    report: { type: Object, required: true },
    showComparison: { type: Boolean, default: true },
});
const reportMonth = computed(() => Number(props.report.period.slice(5)));
const isReceiptExpanded = ref(false);
const categoryGroups = computed(() => splitMonthlyCategories(props.report.categories));
const primaryCategories = computed(() => categoryGroups.value.primaryCategories);
const additionalCategories = computed(() => categoryGroups.value.additionalCategories);

watch(
    () => props.report.period,
    () => {
        isReceiptExpanded.value = false;
    },
);

function toggleReceipt() {
    isReceiptExpanded.value = !isReceiptExpanded.value;
}

const CHART_COLORS = {
    primary: 'var(--tt-primary)',
    accent: 'var(--tt-accent)',
    success: 'var(--tt-success)',
    danger: 'var(--tt-danger)',
    info: 'var(--tt-info)',
    other: 'var(--tt-text-muted)',
};
const chartStyle = computed(() => {
    if (!props.report.parentCategories.length) {
        return { background: 'var(--tt-border)' };
    }
    let offset = 0;
    const segments = props.report.parentCategories.map((category) => {
        const nextOffset = offset + category.ratio;
        const segment = `${CHART_COLORS[category.tone]} ${offset}% ${nextOffset}%`;
        offset = nextOffset;
        return segment;
    });
    return { background: `conic-gradient(${segments.join(', ')})` };
});
</script>

<template>
    <section class="category-report" aria-labelledby="category-title">
        <h2 id="category-title">카테고리별</h2>
        <div class="category-report__ratio-card">
            <h3>TOP 5</h3>
            <div class="category-report__chart-row">
                <div
                    class="category-report__donut"
                    :style="chartStyle"
                    aria-label="카테고리별 지출 비율"
                >
                    <span></span>
                    <b>{{ reportMonth }}월</b>
                </div>
                <ul class="category-report__legend">
                    <li v-for="category in report.parentCategories" :key="category.name">
                        <i :class="`category-report__dot--${category.tone}`"></i>
                        <span>{{ category.name }}</span
                        ><b>{{ category.ratio }}%</b>
                    </li>
                </ul>
            </div>
        </div>
        <div class="category-report__receipt">
            <p>선고 명세&nbsp; · &nbsp;카테고리별</p>
            <ul>
                <li v-for="category in primaryCategories" :key="category.categoryId">
                    <i :class="`category-report__icon--${category.tone}`">{{ category.code }}</i>
                    <span>{{ category.name }}</span>
                    <strong>{{ formatWon(category.amount) }}</strong>
                    <b
                        v-if="showComparison"
                        :class="{
                            'category-report__change--up': category.changeRate > 0,
                            'category-report__change--down': category.changeRate < 0,
                        }"
                    >
                        {{ formatChangeRate(category.changeRate) }}
                    </b>
                </li>
            </ul>
            <Transition name="receipt-slide">
                <ul v-if="isReceiptExpanded" class="category-report__receipt-additional">
                    <li v-for="category in additionalCategories" :key="category.categoryId">
                        <i :class="`category-report__icon--${category.tone}`">{{
                            category.code
                        }}</i>
                        <span>{{ category.name }}</span>
                        <strong>{{ formatWon(category.amount) }}</strong>
                        <b
                            v-if="showComparison"
                            :class="{
                                'category-report__change--up': category.changeRate > 0,
                                'category-report__change--down': category.changeRate < 0,
                            }"
                        >
                            {{ formatChangeRate(category.changeRate) }}
                        </b>
                    </li>
                </ul>
            </Transition>
            <button
                v-if="additionalCategories.length"
                type="button"
                class="category-report__receipt-more"
                :aria-expanded="isReceiptExpanded"
                @click="toggleReceipt"
            >
                <span>
                    {{ isReceiptExpanded ? '접기' : `더보기 ${additionalCategories.length}개` }}
                </span>
            </button>
            <footer>
                <span>합계</span><strong>{{ formatWon(report.totalSpent) }}</strong>
            </footer>
        </div>
    </section>
</template>

<style scoped>
.category-report {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
}
.category-report h2 {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}
.category-report__ratio-card,
.category-report__receipt {
    padding: var(--tt-space-5);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
}
.category-report__ratio-card h3 {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
}
.category-report__ratio-card em {
    font-style: normal;
    color: var(--tt-danger);
}
.category-report__ratio-card h3 span {
    margin-left: var(--tt-space-1);
    color: var(--tt-text-muted);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-medium);
}
.category-report__chart-row {
    display: flex;
    align-items: center;
    justify-content: space-around;
    gap: var(--tt-space-4);
    margin-top: var(--tt-space-5);
}
.category-report__donut {
    position: relative;
    flex: 0 0 132px;
    width: 132px;
    height: 132px;
    border-radius: 50%;
}
.category-report__donut span {
    position: absolute;
    inset: 31px;
    background: var(--tt-bg);
    border-radius: 50%;
}
.category-report__donut > b {
    position: absolute;
    inset: 0;
    display: grid;
    font-size: var(--tt-fs-caption);
    place-items: center;
}
.category-report__legend {
    display: flex;
    flex: 1;
    flex-direction: column;
    gap: var(--tt-space-2);
}
.category-report__legend li {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    color: var(--tt-text-muted);
}
.category-report__legend b {
    margin-left: auto;
    color: var(--tt-text);
}
.category-report__legend i {
    width: 16px;
    height: 16px;
    border-radius: var(--tt-radius-sm);
}
.category-report__dot--primary,
.category-report__icon--primary {
    --tt-category-color: var(--tt-primary);
}
.category-report__dot--accent,
.category-report__icon--accent {
    --tt-category-color: var(--tt-accent);
}
.category-report__dot--success,
.category-report__icon--success {
    --tt-category-color: var(--tt-success);
}
.category-report__dot--danger,
.category-report__icon--danger {
    --tt-category-color: var(--tt-danger);
}
.category-report__dot--info,
.category-report__icon--info {
    --tt-category-color: var(--tt-info);
}
.category-report__dot--other,
.category-report__icon--other {
    --tt-category-color: var(--tt-text-muted);
}
.category-report__legend i {
    background: var(--tt-category-color);
}
.category-report__receipt {
    overflow: hidden;
    border-radius: var(--tt-radius-lg);
}
.category-report__receipt > p {
    margin-bottom: var(--tt-space-4);
    padding-bottom: var(--tt-space-4);
    font-family: var(--tt-font-mono);
    color: var(--tt-text-soft);
    text-align: center;
    border-bottom: 2px dashed var(--tt-border);
}
.category-report__receipt ul {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
}
.category-report__receipt-additional {
    margin-top: var(--tt-space-4);
}
.category-report__receipt li {
    display: grid;
    grid-template-columns: 42px 1fr auto auto;
    align-items: center;
    gap: var(--tt-space-2);
}
.category-report__icon--primary,
.category-report__icon--accent,
.category-report__icon--success,
.category-report__icon--danger,
.category-report__icon--info,
.category-report__icon--other {
    display: grid;
    width: 40px;
    height: 40px;
    font-style: normal;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-category-color);
    background: color-mix(in srgb, var(--tt-category-color) 15%, var(--tt-bg));
    border-radius: var(--tt-radius-md);
    place-items: center;
}
.category-report__receipt li > strong {
    font-family: var(--tt-font-mono);
}
.category-report__receipt li > b {
    min-width: 46px;
    font-family: var(--tt-font-mono);
    color: var(--tt-text-soft);
    text-align: right;
}
.category-report__change--up {
    color: var(--tt-danger) !important;
}
.category-report__change--down {
    color: var(--tt-success) !important;
}
.category-report__receipt-more {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-1);
    width: 100%;
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-3) 0 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    background: transparent;
    border: 0;
    border-top: 2px dashed var(--tt-border);
    cursor: pointer;
}
.category-report__receipt-more:hover {
    color: var(--tt-text);
}
.receipt-slide-enter-active,
.receipt-slide-leave-active {
    max-height: 4000px;
    overflow: hidden;
    transition: all 0.25s ease;
}
.receipt-slide-enter-from,
.receipt-slide-leave-to {
    max-height: 0;
    margin-top: 0;
    opacity: 0;
}
.category-report__receipt footer {
    display: flex;
    justify-content: space-between;
    margin-top: var(--tt-space-5);
    padding-top: var(--tt-space-5);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    border-top: 2px dashed var(--tt-border);
}
.category-report__receipt footer strong {
    font-family: var(--tt-font-mono);
}
@media (max-width: 360px) {
    .category-report__chart-row {
        align-items: flex-start;
        flex-direction: column;
    }
    .category-report__receipt li {
        grid-template-columns: 38px 1fr auto;
    }
    .category-report__receipt li > b:not(:first-child) {
        grid-column: 2 / -1;
        margin-top: calc(var(--tt-space-2) * -1);
    }
}
</style>
