<!--
  용도: 개인 미션 홈의 「이번 달 누적」 카드. 점수와 월간 순위 게이지만 담는다.
  주간 판정은 PersonalWeeklyVerdictCard 로 분리했다.
  명예의 전당 배너와 좌우로 나란히 서므로 반쪽 폭에서 안 깨지게 짜여 있다.
-->
<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue';
import { calculateRankingFill } from '@/services/missionMonthlyScore';

const props = defineProps({
    score: { type: Number, required: true },
    topPercent: { type: Number, default: null },
});

const rankingFill = computed(() => calculateRankingFill(props.topPercent));

const rankingLabel = computed(() =>
    rankingFill.value === null
        ? '월간 순위 집계 전'
        : `월간 순위 상위 ${props.topPercent}% — 참여자의 ${rankingFill.value}% 보다 앞서 있다`,
);

/* 게이지가 바닥에서 차오르게 한다. 첫 프레임을 넘긴 뒤 켜야 transition 이 걸린다. */
const gaugeRevealed = ref(false);
const gaugeHeight = computed(() => (gaugeRevealed.value ? (rankingFill.value ?? 0) : 0));
let animationFrameId = null;

onMounted(() => {
    animationFrameId = requestAnimationFrame(() => {
        gaugeRevealed.value = true;
        animationFrameId = null;
    });
});

onBeforeUnmount(() => {
    if (animationFrameId !== null) cancelAnimationFrame(animationFrameId);
});
</script>

<template>
    <div class="score-card">
        <div class="score-card__numbers">
            <div class="score-card__overline">이번 달 누적</div>
            <div class="score-card__score-row">
                <span class="score-card__score">{{ score }}</span>
                <span class="score-card__unit">점</span>
            </div>
            <!-- 게이지가 가리키는 위치를 숫자로 한 번 말한다. 점수 바로 아래에 붙여야
                 「이번 달 누적 → 몇 점 → 상위 몇 %」로 이어 읽힌다.
                 카드 구석에 떼어 놓으면 어느 값에 딸린 말인지 모른 채 떠 있다. -->
            <span class="score-card__percentile">
                {{ topPercent === null ? '순위 집계 전' : `상위 ${topPercent}%` }}
            </span>
        </div>

        <!-- 순위는 쌓이는 값이 아니라 위치라 세로 게이지로 보여준다.
             내가 앞선 만큼이 바닥에서 차오르고 그 경계가 곧 내 위치 점선이다.
             점선은 막대보다 좌우로 5px 씩 넘겨 그어야 「기준선」으로 읽힌다. -->
        <div class="score-card__gauge" role="img" :aria-label="rankingLabel">
            <small class="score-card__gauge-cap">상위</small>
            <div class="score-card__gauge-track">
                <div class="score-card__gauge-bar">
                    <div class="score-card__gauge-fill" :style="{ height: `${gaugeHeight}%` }" />
                </div>
                <div
                    v-if="rankingFill !== null"
                    class="score-card__gauge-mark"
                    :style="{ bottom: `${gaugeHeight}%` }"
                />
            </div>
            <small class="score-card__gauge-cap">하위</small>
        </div>
    </div>
</template>

<style scoped>
.score-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-2);
    height: 100%;
    background: var(--tt-bg);
    border-radius: var(--tt-radius-lg);
    padding: 14px;
}

.score-card__numbers {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    min-width: 0;
}

.score-card__overline {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
    /* 반쪽 폭이라 자간을 벌리면 「이번 달 누적」이 두 줄로 접힌다 */
    letter-spacing: 0.02em;
    white-space: nowrap;
}

.score-card__score-row {
    display: flex;
    align-items: baseline;
    gap: 4px;
    margin-top: 3px;
}

.score-card__score {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    letter-spacing: -0.02em;
    font-family: var(--tt-font-mono);
}

.score-card__unit {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-deep);
}

.score-card__percentile {
    margin-top: 6px;
    background: var(--tt-accent-subtle);
    color: var(--tt-accent-deep);
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    padding: 3px 8px;
    border-radius: var(--tt-radius-full);
    white-space: nowrap;
}

/* ── 월간 순위 게이지 ──────────────────── */
.score-card__gauge {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 3px;
    flex: none;
}

/* 방향 라벨. 어느 쪽이 잘한 것인지 없으면 게이지를 거꾸로 읽는다. */
.score-card__gauge-cap {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-bold);
    line-height: 1;
    color: var(--tt-text-hint);
}

.score-card__gauge-track {
    position: relative;
    width: 18px;
    /* 옆 명예의 전당 카드가 이 높이를 따라온다 — 너무 낮으면 두 카드가 납작해진다 */
    height: 56px;
}

.score-card__gauge-bar {
    position: absolute;
    inset: 0;
    /* 완전한 pill 로 두면 위아래가 캡슐처럼 잘려 눈금이 어디까지인지 흐려진다 */
    border-radius: 4px;
    background: var(--tt-border-track);
    overflow: hidden;
}

.score-card__gauge-fill {
    position: absolute;
    left: 0;
    right: 0;
    bottom: 0;
    background: var(--tt-accent);
    transition: height 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

/*
 * 내 위치 점선. 금색 채움과 회색 트랙에 반씩 걸치므로 양쪽에서 다 읽히면서
 * 눈을 찌르지 않는 회색을 쓴다. 막대 폭만큼만 그으면 점선인지 알 수 없어 좌우로 넘긴다.
 */
.score-card__gauge-mark {
    position: absolute;
    left: -5px;
    right: -5px;
    height: 0;
    border-top: 2px dashed var(--tt-text-body);
    transform: translateY(1px);
    transition: bottom 0.6s cubic-bezier(0.22, 1, 0.36, 1);
}

@media (prefers-reduced-motion: reduce) {
    .score-card__gauge-fill,
    .score-card__gauge-mark {
        transition: none;
    }
}

@media (max-width: 359px) {
    .score-card__gauge-track {
        width: 14px;
    }
}
</style>
