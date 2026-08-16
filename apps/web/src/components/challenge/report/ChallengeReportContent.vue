<script setup>
import { ref } from 'vue';
import { formatPercentage, formatPercentagePoint, formatPeriod } from '@/utils/challengeReport';
import ChallengeConsumptionHabitDropdown from '@/components/challenge/report/ChallengeConsumptionHabitDropdown.vue';

defineProps({
    report: { type: Object, required: true },
    showComparison: { type: Boolean, default: true },
});
defineEmits(['change-difficulty', 'open-group-history']);

const isWeeklyResultsOpen = ref(false);
</script>

<template>
    <div class="report-content">
        <div class="report-context">
            <span>{{ formatPeriod(report.period) }}</span>
            <strong>개인 챌린지</strong>
        </div>

        <section class="mission-card" aria-labelledby="mission-success-title">
            <header>{{ report.challengeName }} 월간 판결문</header>
            <div class="mission-card__body">
                <p id="mission-success-title">미션 성공률</p>
                <strong class="mission-card__score">
                    {{ report.missionSuccessRate }}<small>%</small>
                </strong>
                <p v-if="showComparison" class="mission-card__comparison">
                    {{ report.successfulDays }}일 성공 · 전월 대비
                    <b>{{ formatPercentagePoint(report.monthOverMonthPercentagePoint) }}</b>
                </p>
                <div class="mission-card__track" aria-hidden="true">
                    <span :style="{ width: formatPercentage(report.missionSuccessRate) }"></span>
                </div>
                <button
                    type="button"
                    class="mission-card__weekly-toggle"
                    :aria-expanded="isWeeklyResultsOpen"
                    aria-controls="weekly-results"
                    @click="isWeeklyResultsOpen = !isWeeklyResultsOpen"
                >
                    <span>주차별 성공률</span>
                    <svg viewBox="0 0 24 24" aria-hidden="true">
                        <path d="m7 10 5 5 5-5" />
                    </svg>
                </button>
            </div>
            <Transition name="receipt-slide">
                <section
                    v-if="isWeeklyResultsOpen"
                    id="weekly-results"
                    class="weekly-card"
                    aria-labelledby="weekly-title"
                >
                    <h2 id="weekly-title">주차별 성공률</h2>
                    <ul>
                        <li v-for="week in report.weeklyResults" :key="week.week">
                            <i aria-hidden="true"></i>
                            <strong>{{ week.week }}주차</strong>
                            <div class="weekly-card__track" aria-hidden="true">
                                <span :style="{ width: formatPercentage(week.successRate) }"></span>
                            </div>
                            <b>{{ week.successDays }}/{{ week.totalDays }}</b>
                        </li>
                    </ul>
                </section>
            </Transition>
        </section>

        <ChallengeConsumptionHabitDropdown v-if="report.netSavings != null" :report="report" />

        <section class="challenge-summary" aria-label="챌린지 요약">
            <div>
                <span>최고 연속 성공</span>
                <strong>{{ report.bestStreakDays }}일</strong>
                <small>가장 잘 지킨 요일 · {{ report.bestWeekday }}</small>
            </div>
            <div>
                <span>도전 일수</span>
                <strong>{{ report.challengeDays }}일</strong>
            </div>
            <div>
                <span>획득 점수</span>
                <strong>{{ report.earnedPoints }}점</strong>
            </div>
        </section>

        <section class="performance-section" aria-labelledby="performance-title">
            <h2 id="performance-title">난이도별 성과</h2>

            <div class="difficulty-card">
                <ul>
                    <li v-for="difficulty in report.difficulties" :key="difficulty.level">
                        <span
                            class="difficulty-card__level"
                            :class="`difficulty-card__level--${difficulty.tone}`"
                        >
                            {{ difficulty.level }}
                        </span>
                        <span class="difficulty-card__attempts">{{ difficulty.attempts }}회</span>
                        <div class="difficulty-card__track" aria-hidden="true">
                            <span
                                :style="{ width: formatPercentage(difficulty.successRate) }"
                            ></span>
                        </div>
                        <strong>{{ difficulty.successRate }}%</strong>
                    </li>
                </ul>
                <footer>
                    <strong>{{ report.difficultySummary }}</strong>
                    <button type="button" @click="$emit('change-difficulty')">
                        난이도 바꾸기 ›
                    </button>
                </footer>
            </div>
        </section>

        <section v-if="report.groupRecord" class="group-section" aria-labelledby="group-title">
            <h2 id="group-title">그룹 전적</h2>
            <div class="group-card">
                <div class="group-card__stats">
                    <div>
                        <strong>{{ report.groupRecord.participatingGroups }}개</strong
                        ><span>참여 그룹</span>
                    </div>
                    <div>
                        <strong>{{ report.groupRecord.survivedCount }}승</strong><span>생존</span>
                    </div>
                    <div>
                        <strong>{{ report.groupRecord.eliminatedCount }}패</strong><span>탈락</span>
                    </div>
                </div>
                <footer>
                    <div>
                        <span class="group-card__stamp group-card__stamp--success"
                            >무죄 {{ report.groupRecord.acquittedCount }}</span
                        >
                        <span class="group-card__stamp group-card__stamp--danger"
                            >유죄 {{ report.groupRecord.convictedCount }}</span
                        >
                        <span>· 피기소 {{ report.groupRecord.indictedCount }}회</span>
                    </div>
                    <button type="button" @click="$emit('open-group-history')">이력 ›</button>
                </footer>
            </div>
        </section>
    </div>
</template>

<style scoped>
.report-content {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-5);
}

.report-context {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
}

.report-context span {
    padding: var(--tt-space-2) var(--tt-space-4);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-full);
}

.report-context strong {
    font-size: var(--tt-fs-section);
}

.mission-card {
    overflow: hidden;
    color: var(--tt-text);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-1);
}

.mission-card header {
    padding: var(--tt-space-4);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-accent);
    text-align: center;
    letter-spacing: 0.18em;
    background: var(--tt-text);
    border-radius: 0 0 var(--tt-radius-full) var(--tt-radius-full);
}

.mission-card__body {
    padding: var(--tt-space-6);
}

.mission-card__body > p:first-child {
    color: var(--tt-text-muted);
}

.mission-card__score {
    display: block;
    margin: var(--tt-space-1) 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-display);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
}

.mission-card__score small {
    margin-left: var(--tt-space-1);
    font-size: var(--tt-fs-section);
}

.mission-card__comparison {
    color: var(--tt-text-muted);
}

.mission-card__comparison b {
    color: color-mix(in srgb, var(--tt-success) 80%, var(--tt-bg));
}

.mission-card__track,
.weekly-card__track,
.difficulty-card__track {
    overflow: hidden;
    border-radius: var(--tt-radius-full);
}

.mission-card__track {
    height: var(--tt-space-2);
    margin-top: var(--tt-space-4);
    background: var(--tt-border);
}

.mission-card__track span,
.weekly-card__track span,
.difficulty-card__track span {
    display: block;
    height: 100%;
    background: var(--tt-success);
    border-radius: inherit;
    transform-origin: left center;
    animation: report-progress-fill 720ms ease-out both;
}

.mission-card__weekly-toggle {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    margin-top: var(--tt-space-5);
    padding: var(--tt-space-3) 0 0;
    font: inherit;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    cursor: pointer;
    background: transparent;
    border: 0;
    border-top: 1px solid var(--tt-border);
}

.mission-card__weekly-toggle svg {
    width: 20px;
    height: 20px;
    fill: none;
    stroke: currentColor;
    stroke-linecap: round;
    stroke-linejoin: round;
    stroke-width: 2;
}

.weekly-card {
    overflow: hidden;
    padding: var(--tt-space-4);
    background: var(--tt-bg-subtle);
    border-top: 1px solid var(--tt-border);
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

.weekly-card h2 {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}

.weekly-card ul {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.weekly-card li {
    display: grid;
    grid-template-columns: 10px 44px minmax(0, 1fr) 38px;
    align-items: center;
    gap: var(--tt-space-2);
}

.weekly-card li > i {
    width: 10px;
    height: 10px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.weekly-card li > strong,
.weekly-card li > b {
    font-family: var(--tt-font-mono);
}

.weekly-card li > strong {
    color: var(--tt-text);
}

.weekly-card li > b {
    color: var(--tt-text);
}

.weekly-card li > b {
    text-align: right;
}

.weekly-card__track {
    height: var(--tt-space-2);
    background: var(--tt-border);
}

.difficulty-card__track {
    height: var(--tt-space-2);
    background: var(--tt-border);
}

@keyframes report-progress-fill {
    from {
        transform: scaleX(0);
    }

    to {
        transform: scaleX(1);
    }
}

@media (prefers-reduced-motion: reduce) {
    .receipt-slide-enter-active,
    .receipt-slide-leave-active,
    .mission-card__track span,
    .weekly-card__track span,
    .difficulty-card__track span {
        animation: none;
    }
}

.challenge-summary {
    display: grid;
    grid-template-columns: 1.4fr 1fr 1fr;
    overflow: hidden;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.challenge-summary > div {
    min-width: 0;
    padding: var(--tt-space-4);
    background: var(--tt-bg);
}

.challenge-summary > div + div {
    border-left: 1px solid var(--tt-border);
}

.challenge-summary span,
.challenge-summary strong,
.challenge-summary small {
    display: block;
}

.challenge-summary span,
.challenge-summary small {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.challenge-summary strong {
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.04em;
}

.challenge-summary > div:first-child strong {
    color: var(--tt-success);
}

.challenge-summary > div:nth-child(2) strong {
    color: var(--tt-brand-700);
}

.challenge-summary > div:last-child strong {
    color: var(--tt-text);
}

.performance-section {
    padding-top: var(--tt-space-8);
}

.difficulty-card {
    margin-top: var(--tt-space-3);
    padding: var(--tt-space-5);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-1);
}

.difficulty-card ul {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
}

.difficulty-card li {
    display: grid;
    grid-template-columns: 44px 44px minmax(0, 1fr) 42px;
    align-items: center;
    gap: var(--tt-space-2);
}

.difficulty-card__level {
    display: grid;
    width: 40px;
    height: 40px;
    font-weight: var(--tt-fw-black);
    border-radius: var(--tt-radius-full);
    place-items: center;
}

.difficulty-card__level--low {
    color: var(--tt-success);
    background: var(--tt-success-subtle);
}

.difficulty-card__level--middle {
    color: var(--tt-text);
    background: var(--tt-accent-subtle);
}

.difficulty-card__level--high {
    color: var(--tt-danger);
    background: var(--tt-danger-subtle);
}

.difficulty-card__attempts {
    color: var(--tt-text-muted);
}

.difficulty-card li > strong {
    font-family: var(--tt-font-mono);
    text-align: right;
}

.difficulty-card footer,
.group-card footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    margin-top: var(--tt-space-5);
    padding-top: var(--tt-space-4);
    border-top: 1px solid var(--tt-border);
}

.difficulty-card footer button,
.group-card footer button {
    flex-shrink: 0;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    background: transparent;
    border: 0;
    cursor: pointer;
}

.performance-section > h2,
.group-section > h2 {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-section);
}

.group-card {
    padding: var(--tt-space-5);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-lg);
}

.group-card__stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    text-align: center;
}

.group-card__stats > div + div {
    border-left: 1px solid color-mix(in srgb, var(--tt-primary) 20%, var(--tt-primary-subtle));
}

.group-card__stats strong,
.group-card__stats span {
    display: block;
}

.group-card__stats strong {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-title);
}

.group-card__stats > div:nth-child(2) strong {
    color: var(--tt-success);
}

.group-card__stats > div:nth-child(3) strong {
    color: var(--tt-danger);
}

.group-card__stats span,
.group-card footer span {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.group-card footer > div {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: var(--tt-space-2);
}

.group-card__stamp {
    padding: var(--tt-space-1) var(--tt-space-2);
    font-weight: var(--tt-fw-bold);
    background: var(--tt-bg-subtle);
    border: 1px solid currentColor;
    border-radius: var(--tt-radius-sm);
    transform: rotate(-3deg);
}

.group-card__stamp--success {
    color: var(--tt-success) !important;
}

.group-card__stamp--danger {
    color: var(--tt-danger) !important;
    transform: rotate(3deg);
}

@media (max-width: 360px) {
    .challenge-summary {
        grid-template-columns: 1.3fr 1fr 1fr;
    }

    .challenge-summary > div {
        padding: var(--tt-space-3);
    }

    .difficulty-card {
        padding: var(--tt-space-4);
    }

    .difficulty-card li {
        grid-template-columns: 40px 38px minmax(0, 1fr) 38px;
        gap: var(--tt-space-1);
    }

    .difficulty-card footer,
    .group-card footer {
        align-items: flex-start;
        flex-direction: column;
    }
}
</style>
