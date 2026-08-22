<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { calculatePersonalMissionProgress, formatWon } from '@/services/personalMissionFlow';

const props = defineProps({
    mission: {
        type: Object,
        required: true,
    },
    difficulty: {
        type: Object,
        default: null,
    },
    personalized: {
        type: Boolean,
        default: false,
    },
});

const targetAmount = computed(() => {
    return props.difficulty?.targetAmount ?? props.mission.targetAmount;
});

const progress = computed(() => {
    return calculatePersonalMissionProgress(props.mission.currentAmount, targetAmount.value);
});

const remainingAmount = computed(() => {
    return Math.max(targetAmount.value - props.mission.currentAmount, 0);
});
</script>

<template>
    <BaseCard class="personal-mission-card" padding="lg">
        <article class="mission-card">
            <div class="mission-card__rail" aria-hidden="true">
                <span></span>
                <span></span>
                <span></span>
            </div>

            <header>
                <span>
                    {{
                        personalized
                            ? `${difficulty.label} · ${difficulty.shortLabel}`
                            : '공통 미션'
                    }}
                </span>

                <small>2026-미션-0729</small>
            </header>

            <h2>{{ mission.title }}</h2>
            <p>{{ mission.description }}</p>

            <template v-if="personalized">
                <div class="mission-card__amount">
                    <span class="mission-card__current-label">현재</span>
                    <strong>{{ formatWon(mission.currentAmount) }}</strong>
                    <span>/ 목표 {{ formatWon(targetAmount) }}</span>
                    <em>여유 {{ formatWon(remainingAmount) }}</em>
                </div>

                <div
                    class="mission-card__progress"
                    role="progressbar"
                    :aria-valuenow="progress"
                    aria-valuemin="0"
                    aria-valuemax="100"
                >
                    <span :style="{ width: `${progress}%` }"></span>
                </div>

                <div class="mission-card__reward">
                    🏆 성공 시 +{{ difficulty.rewardPoints }}점 · 연속 보너스 +5점
                </div>
            </template>

            <div v-else class="mission-card__common-result">
                ✓ 현재까지 {{ mission.category }} 지출
                <strong>{{ formatWon(mission.currentAmount) }}</strong>
                · 성공 시 +{{ mission.rewardPoints }}점
            </div>
        </article>
    </BaseCard>
</template>

<style scoped>
/*
 * 셸은 BaseCard(.tt-card)가 그리고 그림자·테두리도 거기서 온다.
 * 재판탭 홈은 카드에 선도 그림자도 쓰지 않으므로 여기서만 끈다 — 팀 공용 BaseCard 는 그대로 둔다.
 * `.tt-card` 를 같이 적어 특이도를 올려야 한다. 클래스 하나끼리는 번들 순서에 따라 져버린다.
 */
.personal-mission-card.tt-card {
    border: none;
    box-shadow: none;
}

.personal-mission-card {
    position: relative;
    overflow: hidden;
    background-color: var(--tt-bg);
    background-image: repeating-linear-gradient(
        to bottom,
        transparent 0,
        transparent calc(var(--tt-space-10) - 1px),
        var(--tt-border) var(--tt-space-10)
    );
    border-radius: var(--tt-radius-sm);
}

.personal-mission-card::before {
    position: absolute;
    top: 0;
    bottom: 0;
    left: var(--tt-space-10);
    width: 1px;
    content: '';
    background: var(--tt-info-subtle);
}

.mission-card header {
    display: flex;
    gap: var(--tt-space-2);
    justify-content: space-between;
    color: var(--tt-text-muted);
}

.mission-card {
    position: relative;
    padding-left: var(--tt-space-5);
}

.mission-card__rail {
    position: absolute;
    top: var(--tt-space-12);
    bottom: var(--tt-space-8);
    left: calc(var(--tt-space-1) * -1);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    width: 2px;
    background: transparent;
}

.mission-card__rail span {
    width: var(--tt-space-3);
    height: var(--tt-space-3);
    background: var(--tt-bg-fill);
    border: 2px solid var(--tt-border);
    box-shadow: inset 0 1px 2px var(--tt-border);
    border-radius: var(--tt-radius-full);
    transform: translateX(calc((var(--tt-space-3) - 2px) / -2));
}

.mission-card header span {
    min-width: 0;
    padding: var(--tt-space-1) var(--tt-space-2);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-info-deep);
    background: var(--tt-info-subtle);
    border-radius: var(--tt-radius-full);
}

.mission-card header small {
    flex: none;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
}

.mission-card h2 {
    margin: var(--tt-space-4) 0 var(--tt-space-1);
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-snug);
}

.mission-card > p {
    margin: 0;
    color: var(--tt-text-muted);
}

.mission-card__amount {
    display: flex;
    gap: var(--tt-space-1);
    align-items: baseline;
    margin-top: var(--tt-space-5);
    color: var(--tt-text-body);
}

.mission-card__amount strong {
    font-family: var(--tt-font-mono);
    color: var(--tt-text);
}

.mission-card__amount .mission-card__current-label {
    flex: none;
    color: var(--tt-text-body);
}

.mission-card__amount span {
    min-width: 0;
    color: var(--tt-text-muted);
}

.mission-card__amount em {
    margin-left: auto;
    font-style: normal;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-success-deep);
}

.mission-card__progress {
    height: 8px;
    margin-top: var(--tt-space-3);
    overflow: hidden;
    background: var(--tt-border-track);
    border-radius: var(--tt-radius-full);
}

.mission-card__progress span {
    display: block;
    height: 100%;
    background: var(--tt-success);
    border-radius: inherit;
    transition: width 0.45s ease;
}

.mission-card__reward,
.mission-card__common-result {
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-3);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-accent-deep);
    background: var(--tt-accent-subtle);
    border: 1px solid var(--tt-accent);
    border-radius: var(--tt-radius-md);
    text-align: center;
}

.mission-card__common-result strong {
    color: var(--tt-success);
}

@media (max-width: 390px) {
    .mission-card header {
        align-items: flex-start;
    }

    .mission-card header small {
        font-size: var(--tt-fs-mono-chip);
    }

    .mission-card__amount {
        font-size: var(--tt-fs-caption);
    }
}

@media (max-width: 359px) {
    .personal-mission-card {
        padding: var(--tt-space-3);
    }

    .mission-card {
        padding-left: var(--tt-space-3);
    }

    .mission-card__reward,
    .mission-card__common-result {
        padding: var(--tt-space-2);
        font-size: calc(var(--tt-fs-mono-chip) * 0.92);
        letter-spacing: -0.04em;
        white-space: nowrap;
    }
}

@media (prefers-reduced-motion: reduce) {
    .mission-card__progress span {
        transition: none;
    }
}
</style>
