<script setup>
import { computed, ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchChallengeReport, fetchMockChallengeReport } from '@/api/challengeReport';
import ChallengeSavingsGuide from '@/components/challenge/report/ChallengeSavingsGuide.vue';
import TempChallengeReportSourceToggle from '@/components/challenge/report/TempChallengeReportSourceToggle.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import {
    formatPeriod,
    formatSignedWon,
    formatWon,
    getPreviousPeriod,
} from '@/utils/challengeReport';
import { useChallengeReportStore } from '@/stores/challengeReport';

const route = useRoute();
const router = useRouter();
const challengeReportStore = useChallengeReportStore();
const report = ref(null);
const loading = ref(false);
const errorMessage = ref('');
const isGuideOpen = ref(false);

const selectedPeriod = computed(() =>
    typeof route.query.month === 'string' ? route.query.month : getPreviousPeriod(),
);
const successCategories = computed(
    () => report.value?.categories.filter((category) => category.status === 'success') ?? [],
);
const failureCategories = computed(
    () => report.value?.categories.filter((category) => category.status === 'failure') ?? [],
);

async function loadReport() {
    loading.value = true;
    errorMessage.value = '';
    report.value = null;
    try {
        const fetcher =
            challengeReportStore.reportSource === 'mock'
                ? fetchMockChallengeReport
                : fetchChallengeReport;
        const data = await fetcher(selectedPeriod.value);
        if (!data.hasChallengeHistory || data.netSavings == null) {
            throw new Error('해당 월에는 확인할 순 절감액이 없습니다.');
        }
        report.value = data;
    } catch (error) {
        errorMessage.value = error.message ?? '순 절감액을 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

function goBackToReport() {
    router.back();
}

function switchReportSource(source) {
    if (source === challengeReportStore.reportSource || loading.value) {
        return;
    }
    challengeReportStore.setReportSource(source);
    loadReport();
}

watch(selectedPeriod, loadReport, { immediate: true });
</script>

<template>
    <article class="net-savings-view">
        <header>
            <div class="net-savings-view__title-row">
                <button type="button" aria-label="챌린지 리포트로 돌아가기" @click="goBackToReport">
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="2"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        aria-hidden="true"
                    >
                        <path d="m15 4-8 8 8 8" />
                    </svg>
                </button>
                <h1>바뀐 내 소비습관</h1>
            </div>
            <span>{{ formatPeriod(selectedPeriod) }}</span>
        </header>

        <StateLoading v-if="loading" size="lg" message="절감 결과를 계산하고 있어요" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="loadReport" />
        <template v-else-if="report">
            <section class="settlement-card" aria-labelledby="settlement-title">
                <p>{{ Number(selectedPeriod.slice(5)) }}월 순 절감액 · SETTLEMENT</p>
                <span>평소대로 썼다면 더 나갔을 돈</span>
                <h2 id="settlement-title">{{ formatWon(report.netSavings) }}</h2>
                <div>
                    덜 쓴 돈 <strong>{{ report.savedAmount.toLocaleString('ko-KR') }}</strong>
                    <b>−</b> 더 쓴 돈 <em>{{ report.overspentAmount.toLocaleString('ko-KR') }}</em>
                </div>
                <mark>1년이면 {{ formatWon(report.annualizedNetSavings) }}</mark>
            </section>

            <section class="category-effect" aria-labelledby="category-title">
                <header>
                    <h2 id="category-title">카테고리별 효과</h2>
                    <button type="button" @click="isGuideOpen = true">
                        ?&nbsp; 어떻게 계산했나요
                    </button>
                </header>

                <div class="category-card">
                    <section>
                        <div class="category-card__group-title">
                            <strong
                                >챌린지 성공 <span>· {{ report.successfulDays }}일</span></strong
                            >
                            <mark>평소보다 덜 쓴 돈</mark>
                        </div>
                        <ul>
                            <li v-for="category in successCategories" :key="category.name">
                                <i>{{ category.code }}</i>
                                <strong
                                    >{{ category.name }} <span>{{ category.days }}일</span></strong
                                >
                                <b>{{ formatSignedWon(category.amount) }}</b>
                            </li>
                        </ul>
                    </section>

                    <section class="category-card__failure">
                        <div class="category-card__group-title">
                            <strong
                                >챌린지 실패
                                <span
                                    >· {{ report.challengeDays - report.successfulDays }}일</span
                                ></strong
                            >
                            <mark>평소보다 더 쓴 돈</mark>
                        </div>
                        <ul>
                            <li v-for="category in failureCategories" :key="category.name">
                                <i>{{ category.code }}</i>
                                <strong
                                    >{{ category.name }} <span>{{ category.days }}일</span></strong
                                >
                                <b :class="{ 'category-card__zero': category.amount === 0 }">
                                    {{ formatSignedWon(category.amount) }}
                                </b>
                            </li>
                        </ul>
                        <p>미션은 못 지켰지만 평소보다 더 쓰지는 않았어요</p>
                    </section>
                </div>
            </section>
        </template>

        <ChallengeSavingsGuide v-model="isGuideOpen" @understood="isGuideOpen = false" />
        <TempChallengeReportSourceToggle
            :source="challengeReportStore.reportSource"
            :loading="loading"
            elevated
            @toggle="switchReportSource"
        />
    </article>
</template>

<style scoped>
.net-savings-view {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-5);
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-6) var(--tt-space-5) var(--tt-space-10);
    background: var(--tt-bg-subtle);
}

.net-savings-view__title-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    margin-bottom: var(--tt-space-4);
}

.net-savings-view__title-row h1 {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
}

.net-savings-view__title-row button {
    display: flex;
    flex: 0 0 32px;
    align-items: center;
    justify-content: center;
    width: 32px;
    height: 32px;
    padding: 0;
    color: var(--tt-text);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.net-savings-view__title-row svg {
    width: 22px;
    height: 22px;
}

.net-savings-view > header span {
    padding: var(--tt-space-1) var(--tt-space-3);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-full);
}

.settlement-card {
    padding: var(--tt-space-6);
    color: var(--tt-text-inverse);
    background: color-mix(in srgb, var(--tt-text) 92%, var(--tt-bg));
    border-radius: var(--tt-radius-xl);
}

.settlement-card > p {
    margin-bottom: var(--tt-space-3);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-accent);
    letter-spacing: 0.12em;
}

.settlement-card > span,
.settlement-card > div {
    color: var(--tt-border-strong);
}

.settlement-card h2 {
    margin: var(--tt-space-1) 0 var(--tt-space-3);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-display);
    font-weight: var(--tt-fw-black);
}

.settlement-card div strong {
    color: color-mix(in srgb, var(--tt-success) 80%, var(--tt-bg));
}

.settlement-card div em {
    font-style: normal;
    font-weight: var(--tt-fw-bold);
    color: color-mix(in srgb, var(--tt-danger) 75%, var(--tt-bg));
}

.settlement-card div b {
    margin: 0 var(--tt-space-1);
}

.settlement-card > mark {
    display: inline-block;
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-2) var(--tt-space-4);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    background: var(--tt-accent);
    border-radius: var(--tt-radius-full);
}

.category-effect > header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    margin-bottom: var(--tt-space-3);
}

.category-effect h2 {
    font-size: var(--tt-fs-section);
}

.category-effect > header button {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.category-card {
    padding: var(--tt-space-5);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.category-card__failure {
    margin-top: var(--tt-space-5);
    padding-top: var(--tt-space-5);
    border-top: 1px dashed var(--tt-border-strong);
}

.category-card__group-title {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    margin-bottom: var(--tt-space-3);
}

.category-card__group-title > strong {
    color: var(--tt-success);
}

.category-card__failure .category-card__group-title > strong {
    color: var(--tt-danger);
}

.category-card__group-title span {
    color: var(--tt-border-strong);
}

.category-card__group-title mark {
    padding: var(--tt-space-1) var(--tt-space-2);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-md);
}

.category-card ul {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.category-card li {
    display: grid;
    grid-template-columns: 36px 1fr auto;
    align-items: center;
    gap: var(--tt-space-2);
}

.category-card li i {
    display: grid;
    width: 34px;
    height: 34px;
    font-style: normal;
    font-weight: var(--tt-fw-black);
    color: var(--tt-success);
    background: var(--tt-success-subtle);
    border-radius: var(--tt-radius-md);
    place-items: center;
}

.category-card__failure li i {
    color: var(--tt-danger);
    background: var(--tt-danger-subtle);
}

.category-card li strong span {
    color: var(--tt-border-strong);
}

.category-card li b {
    font-family: var(--tt-font-mono);
    color: var(--tt-success);
}

.category-card__failure li b {
    color: var(--tt-danger);
}

.category-card__failure li .category-card__zero {
    color: var(--tt-border-strong);
}

.category-card__failure > p {
    margin-top: var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-align: right;
}

@media (max-width: 360px) {
    .net-savings-view {
        padding-right: var(--tt-space-4);
        padding-left: var(--tt-space-4);
    }

    .category-card__group-title {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>
