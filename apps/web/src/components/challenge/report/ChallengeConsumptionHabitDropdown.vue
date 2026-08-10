<script setup>
import { computed, ref } from 'vue';
import ChallengeSavingsGuide from '@/components/challenge/report/ChallengeSavingsGuide.vue';
import { formatSignedWon, formatWon } from '@/utils/challengeReport';

const props = defineProps({ report: { type: Object, required: true } });

const isDetailsOpen = ref(false);
const isGuideOpen = ref(false);
const successCategories = computed(
    () => props.report.categories?.filter((category) => category.status === 'success') ?? [],
);
const failureCategories = computed(
    () => props.report.categories?.filter((category) => category.status === 'failure') ?? [],
);
const topSavingCategory = computed(() =>
    successCategories.value.reduce(
        (topCategory, category) =>
            !topCategory || category.amount > topCategory.amount ? category : topCategory,
        null,
    ),
);
</script>

<template>
    <section class="habit-dropdown" aria-labelledby="habit-title">
        <button
            type="button"
            class="habit-settlement"
            :class="{ 'habit-settlement--open': isDetailsOpen }"
            :aria-expanded="isDetailsOpen"
            aria-controls="habit-details"
            @click="isDetailsOpen = !isDetailsOpen"
        >
            <span class="habit-settlement__summary">
                <small>이번 달 순 절감액</small>
                <em>평소대로 썼다면 더 나갔을 돈</em>
                <strong id="habit-title"
                    ><b>{{ formatWon(report.netSavings) }}</b></strong
                >
            </span>
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m7 10 5 5 5-5" /></svg>
        </button>

        <div v-if="isDetailsOpen" id="habit-details" class="habit-dropdown__details">
            <div class="habit-settlement__meta">
                <div>
                    <span>덜 쓴 돈</span>
                    <strong>{{ formatWon(report.savedAmount) }}</strong>
                </div>
                <div>
                    <span>더 쓴 돈</span>
                    <em>{{ formatWon(report.overspentAmount) }}</em>
                </div>
                <div>
                    <span>1년이면</span>
                    <mark>{{ formatWon(report.annualizedNetSavings) }}</mark>
                </div>
            </div>

            <section class="habit-category" aria-labelledby="habit-category-title">
                <button type="button" class="habit-category__toggle" @click="isGuideOpen = true">
                    <span>
                        <strong id="habit-category-title">카테고리별 효과</strong>
                    </span>
                    <span class="habit-category__help">?&nbsp; 어떻게 계산했나요</span>
                </button>
                <div class="habit-category__details">
                    <div v-if="topSavingCategory" class="habit-category__insight">
                        <span>이번 달 핵심 변화</span>
                        <strong
                            >{{ topSavingCategory.name }}에서
                            <b>{{ formatWon(topSavingCategory.amount) }}</b> 아꼈어요</strong
                        >
                    </div>
                    <div class="habit-category__card">
                        <section>
                            <div class="habit-category__title">
                                <strong
                                    >챌린지 성공
                                    <span>· {{ report.successfulDays }}일</span></strong
                                >
                                <mark>평소보다 덜 쓴 돈</mark>
                            </div>
                            <ul>
                                <li v-for="category in successCategories" :key="category.name">
                                    <i>{{ category.code }}</i>
                                    <strong
                                        >{{ category.name }}
                                        <span>{{ category.days }}일</span></strong
                                    >
                                    <b>{{ formatSignedWon(category.amount) }}</b>
                                </li>
                            </ul>
                        </section>
                        <section class="habit-category__failure">
                            <div class="habit-category__title">
                                <strong
                                    >챌린지 실패
                                    <span
                                        >·
                                        {{ report.challengeDays - report.successfulDays }}일</span
                                    ></strong
                                >
                                <mark>평소보다 더 쓴 돈</mark>
                            </div>
                            <ul>
                                <li v-for="category in failureCategories" :key="category.name">
                                    <i>{{ category.code }}</i>
                                    <strong
                                        >{{ category.name }}
                                        <span>{{ category.days }}일</span></strong
                                    >
                                    <b :class="{ 'habit-category__zero': category.amount === 0 }">
                                        {{ formatSignedWon(category.amount) }}
                                    </b>
                                </li>
                            </ul>
                            <p>미션은 못 지켰지만 평소보다 더 쓰지는 않았어요</p>
                        </section>
                    </div>
                </div>
            </section>
        </div>
        <ChallengeSavingsGuide v-model="isGuideOpen" @understood="isGuideOpen = false" />
    </section>
</template>

<style scoped>
.habit-dropdown {
    display: flex;
    flex-direction: column;
}
.habit-settlement {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    padding: var(--tt-space-5);
    text-align: left;
    color: var(--tt-text-inverse);
    cursor: pointer;
    background: var(--tt-text);
    border: 0;
    border-radius: var(--tt-radius-lg);
}

.habit-settlement--open {
    border-bottom-right-radius: 0;
    border-bottom-left-radius: 0;
}

.habit-settlement__summary small,
.habit-settlement__summary em,
.habit-settlement__summary strong {
    display: block;
}

.habit-settlement__summary small {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-accent);
}

.habit-settlement__summary em {
    margin-top: var(--tt-space-1);
    font-style: normal;
    color: var(--tt-border-strong);
}

.habit-settlement__summary strong {
    margin: var(--tt-space-1) 0 var(--tt-space-3);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
}

.habit-settlement__summary strong b {
    color: var(--tt-accent);
}

.habit-settlement > svg {
    flex: 0 0 auto;
    width: 28px;
    height: 28px;
    margin-left: var(--tt-space-3);
    fill: none;
    stroke: var(--tt-accent);
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke-width: 2;
}

.habit-dropdown__details {
    overflow: hidden;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-top: 0;
    border-radius: var(--tt-radius-lg);
    border-top-right-radius: 0;
    border-top-left-radius: 0;
}

.habit-settlement__meta {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    border-bottom: 1px solid var(--tt-border);
}

.habit-settlement__meta > div {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-1);
    min-width: 0;
    padding: var(--tt-space-4) var(--tt-space-3);
}

.habit-settlement__meta > div + div {
    border-left: 1px solid var(--tt-border);
}

.habit-settlement__meta span {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.habit-settlement__meta strong,
.habit-settlement__meta em,
.habit-settlement__meta mark {
    overflow: hidden;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    text-overflow: ellipsis;
    white-space: nowrap;
}

.habit-settlement__meta strong {
    color: var(--tt-success);
}

.habit-settlement__meta em {
    font-style: normal;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger);
}
.habit-settlement__meta mark,
.habit-category__title mark {
    display: block;
    padding: 0;
    color: var(--tt-text);
    background: transparent;
}
.habit-category {
    overflow: hidden;
    padding-top: var(--tt-space-2);
}
.habit-category__toggle,
.habit-category__title,
.habit-category__guide-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
}
.habit-category__toggle {
    width: 100%;
    padding: var(--tt-space-4);
    text-align: left;
    color: var(--tt-text);
    cursor: pointer;
    background: transparent;
    border: 0;
}
.habit-category__toggle strong {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
}
.habit-category__toggle svg {
    flex: 0 0 auto;
    width: 22px;
    height: 22px;
    fill: none;
    stroke: currentColor;
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke-width: 2;
}

.habit-category__help {
    flex: 0 0 auto;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
}
.habit-category__details {
    padding: 0 var(--tt-space-4) var(--tt-space-4);
}

.habit-category__insight {
    margin: var(--tt-space-4) 0 var(--tt-space-3);
    padding: var(--tt-space-3) var(--tt-space-4);
    background: var(--tt-success-subtle);
    border: 1px solid color-mix(in srgb, var(--tt-success) 28%, var(--tt-success-subtle));
    border-radius: var(--tt-radius-md);
}

.habit-category__insight span,
.habit-category__insight strong {
    display: block;
}

.habit-category__insight span {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-success);
}

.habit-category__insight strong {
    margin-top: var(--tt-space-1);
    color: var(--tt-text);
}

.habit-category__insight b {
    font-family: var(--tt-font-mono);
    color: var(--tt-success);
}
.habit-category__card {
    padding: var(--tt-space-4);
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}
.habit-category__title {
    margin-bottom: var(--tt-space-3);
}
.habit-category__title > strong,
.habit-category li b {
    color: var(--tt-success);
}
.habit-category__title span,
.habit-category li strong span {
    color: var(--tt-text-muted);
}
.habit-category__title mark {
    display: inline-block;
    padding: var(--tt-space-1) var(--tt-space-2);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
    background: var(--tt-bg);
    border-radius: var(--tt-radius-full);
}
.habit-category ul {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}
.habit-category li {
    display: grid;
    grid-template-columns: 32px 1fr auto;
    align-items: center;
    gap: var(--tt-space-2);
}
.habit-category li i {
    display: grid;
    width: 30px;
    height: 30px;
    font-style: normal;
    font-weight: var(--tt-fw-black);
    color: var(--tt-success);
    background: var(--tt-success-subtle);
    border-radius: var(--tt-radius-sm);
    place-items: center;
}
.habit-category li b {
    font-family: var(--tt-font-mono);
}
.habit-category__failure {
    margin-top: var(--tt-space-4);
    padding-top: var(--tt-space-4);
    border-top: 1px dashed var(--tt-border-strong);
}
.habit-category__failure .habit-category__title > strong,
.habit-category__failure li b {
    color: var(--tt-danger);
}
.habit-category__failure li i {
    color: var(--tt-danger);
    background: var(--tt-danger-subtle);
}
.habit-category__failure li .habit-category__zero {
    color: var(--tt-text-muted);
}
.habit-category__failure > p {
    margin-top: var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-align: right;
}
@media (max-width: 360px) {
    .habit-category__title {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>
