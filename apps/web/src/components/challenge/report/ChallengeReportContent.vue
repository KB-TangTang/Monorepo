<script setup>
import {
    formatPercentage,
    formatPercentagePoint,
    formatPeriod,
    formatWon,
} from '@/utils/challengeReport';

defineProps({ report: { type: Object, required: true } });
defineEmits(['change-difficulty', 'open-group-history', 'open-net-savings']);
</script>

<template>
    <div class="report-content">
        <div class="report-context">
            <span>{{ formatPeriod(report.period) }}</span>
            <strong>{{ report.challengeName }}</strong>
        </div>

        <section class="mission-card" aria-labelledby="mission-success-title">
            <header>{{ report.challengeName }} 월간 판결문</header>
            <div class="mission-card__body">
                <p id="mission-success-title">미션 성공률</p>
                <strong class="mission-card__score">
                    {{ report.missionSuccessRate }}<small>%</small>
                </strong>
                <p class="mission-card__comparison">
                    {{ report.successfulDays }}일 성공 · 전월 대비
                    <b>{{ formatPercentagePoint(report.monthOverMonthPercentagePoint) }}</b>
                </p>
                <div class="mission-card__track" aria-hidden="true">
                    <span :style="{ width: formatPercentage(report.missionSuccessRate) }"></span>
                </div>
            </div>
        </section>

        <section class="weekly-card" aria-labelledby="weekly-title">
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

        <section class="streak-card" aria-label="최고 연속 성공 일수">
            <div>
                <span>최고 연속 성공 일수</span><strong>{{ report.bestStreakDays }}일</strong>
            </div>
            <b>가장 잘 지킨 요일 · {{ report.bestWeekday }}</b>
        </section>

        <section class="summary-cards" aria-label="챌린지 요약">
            <div>
                <span>도전 일수</span><strong>{{ report.challengeDays }}일</strong>
            </div>
            <div>
                <span>획득 점수</span><strong>{{ report.earnedPoints }}점</strong>
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

        <section class="group-section" aria-labelledby="group-title">
            <h2 id="group-title">그룹 전적</h2>
            <div class="group-card">
                <div class="group-card__stats">
                    <div>
                        <strong>{{ report.groupRecord.participatingGroups }}개</strong
                        ><span>참여 그룹</span>
                    </div>
                    <div>
                        <strong>{{ report.groupRecord.wins }}승</strong><span>생존</span>
                    </div>
                    <div>
                        <strong>{{ report.groupRecord.losses }}패</strong><span>탈락</span>
                    </div>
                </div>
                <footer>
                    <div>
                        <span class="group-card__stamp group-card__stamp--success"
                            >무죄 {{ report.groupRecord.acquittals }}</span
                        >
                        <span class="group-card__stamp group-card__stamp--danger"
                            >유죄 {{ report.groupRecord.convictions }}</span
                        >
                        <span>· 피기소 {{ report.groupRecord.dismissals }}회</span>
                    </div>
                    <button type="button" @click="$emit('open-group-history')">이력 ›</button>
                </footer>
            </div>
        </section>

        <section class="savings-card" aria-labelledby="savings-title">
            <div>
                <p>이번 달 순 절감액</p>
                <h2 id="savings-title">{{ formatWon(report.netSavings) }}</h2>
                <strong>챌린지로 바뀐 내 소비습관 확인하기</strong>
            </div>
            <button
                type="button"
                aria-label="카테고리별 순 절감액 보기"
                @click="$emit('open-net-savings')"
            >
                ›
            </button>
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
    padding: var(--tt-space-1) var(--tt-space-3);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-radius: var(--tt-radius-full);
}

.report-context strong {
    font-size: var(--tt-fs-section);
}

.mission-card {
    overflow: hidden;
    color: var(--tt-text-inverse);
    background: color-mix(in srgb, var(--tt-text) 92%, var(--tt-bg));
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-3);
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
    color: var(--tt-border-strong);
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
    color: var(--tt-border-strong);
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
    background: color-mix(in srgb, var(--tt-text) 72%, var(--tt-bg));
}

.mission-card__track span,
.weekly-card__track span,
.difficulty-card__track span {
    display: block;
    height: 100%;
    background: var(--tt-success);
    border-radius: inherit;
}

.weekly-card {
    overflow: hidden;
    padding: var(--tt-space-4);
    background: color-mix(in srgb, var(--tt-bg-subtle) 45%, var(--tt-border));
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
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
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.weekly-card li > strong,
.weekly-card li > b {
    font-family: var(--tt-font-mono);
}

.weekly-card li > b {
    text-align: right;
}

.weekly-card__track,
.difficulty-card__track {
    height: var(--tt-space-2);
    background: var(--tt-border);
}

.streak-card,
.summary-cards > div {
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
}

.streak-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-4);
    padding: var(--tt-space-5);
    background: var(--tt-accent-subtle);
    border-color: color-mix(in srgb, var(--tt-accent) 45%, var(--tt-bg));
}

.streak-card span,
.streak-card strong,
.summary-cards span,
.summary-cards strong {
    display: block;
}

.streak-card span {
    color: color-mix(in srgb, var(--tt-text) 70%, var(--tt-accent));
}

.streak-card strong {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-title);
}

.streak-card > b {
    font-size: var(--tt-fs-caption);
    color: color-mix(in srgb, var(--tt-text) 70%, var(--tt-accent));
}

.summary-cards {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: var(--tt-space-3);
}

.summary-cards > div {
    padding: var(--tt-space-5);
    background: var(--tt-bg);
}

.summary-cards span {
    color: var(--tt-text-muted);
}

.summary-cards strong {
    margin-top: var(--tt-space-1);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-title);
}

.summary-cards > div:last-child strong {
    color: var(--tt-primary);
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

.savings-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-4);
    padding: var(--tt-space-5);
    background: color-mix(in srgb, var(--tt-bg-subtle) 35%, var(--tt-border));
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
}

.savings-card p {
    color: var(--tt-text-muted);
}

.savings-card h2 {
    margin: var(--tt-space-1) 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
}

.savings-card > div > strong {
    color: var(--tt-accent);
}

.savings-card > button {
    display: grid;
    flex: 0 0 48px;
    width: 48px;
    height: 48px;
    font-size: var(--tt-fs-title);
    color: var(--tt-accent);
    background: var(--tt-text);
    border: 0;
    border-radius: var(--tt-radius-full);
    cursor: pointer;
    place-items: center;
}

@media (max-width: 360px) {
    .streak-card {
        align-items: flex-start;
        flex-direction: column;
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
