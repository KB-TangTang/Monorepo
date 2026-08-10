<script setup>
import { computed } from 'vue';
import { tangiSceneWatch } from '@/fixtures/personalChallenge';
import { formatWon, calculatePersonalMissionProgress } from '@/services/personalMissionFlow';

const props = defineProps({
    categoryName: { type: String, required: true },
    alibiCondition: { type: String, required: true },
    currentAmount: { type: Number, required: true },
    limitAmount: { type: Number, required: true },
    streakDays: { type: Number, default: 0 },
    prosecutorName: { type: String, default: '' },
    prosecutorImage: { type: String, default: '' },
});

defineEmits(['prosecutor-click']);

const progress = computed(() =>
    calculatePersonalMissionProgress(props.currentAmount, props.limitAmount),
);

const remaining = computed(() => props.limitAmount - props.currentAmount);
</script>

<template>
    <div class="briefing-card">
        <div class="briefing-card__stamp" aria-hidden="true">탕탕</div>

        <div class="briefing-card__badges">
            <span class="briefing-card__badge briefing-card__badge--danger">수사 브리핑</span>
            <span v-if="streakDays > 0" class="briefing-card__badge briefing-card__badge--streak">
                🔥 연속 {{ streakDays }}일
            </span>
        </div>

        <div class="briefing-card__details">
            <div class="briefing-card__row">
                <span class="briefing-card__label">수사 대상</span>
                <span class="briefing-card__value">{{ categoryName }}</span>
            </div>
            <div class="briefing-card__row">
                <span class="briefing-card__label">알리바이 조건</span>
                <span class="briefing-card__value">
                    {{ alibiCondition }}
                </span>
            </div>
            <div class="briefing-card__row">
                <span class="briefing-card__label">담당 검사</span>
                <button
                    type="button"
                    class="briefing-card__prosecutor-badge"
                    @click="$emit('prosecutor-click')"
                >
                    <span class="briefing-card__prosecutor-avatar">
                        <img :src="prosecutorImage" :alt="prosecutorName" />
                    </span>
                    <span class="briefing-card__prosecutor-name">{{ prosecutorName }}</span>
                    <svg
                        width="7"
                        height="11"
                        viewBox="0 0 7 11"
                        fill="none"
                        aria-hidden="true"
                    >
                        <path
                            d="M1.5 1.5l4 4-4 4"
                            stroke="currentColor"
                            stroke-width="1.8"
                            stroke-linecap="round"
                            stroke-linejoin="round"
                        />
                    </svg>
                </button>
            </div>
        </div>

        <div class="briefing-card__gauge-section">
            <div class="briefing-card__gauge-wrap">
                <img
                    :src="tangiSceneWatch"
                    alt=""
                    class="briefing-card__gauge-tangi"
                    :style="{ left: progress + '%' }"
                />
                <div class="briefing-card__gauge-track">
                    <div
                        class="briefing-card__gauge-fill"
                        :style="{ width: progress + '%' }"
                    ></div>
                    <div class="briefing-card__gauge-marker"></div>
                </div>
                <div class="briefing-card__gauge-labels">
                    <span class="briefing-card__gauge-remain">
                        한도까지
                        <b>{{ formatWon(remaining) }}</b>
                    </span>
                    <span class="briefing-card__gauge-limit">
                        한도 {{ formatWon(limitAmount) }}
                    </span>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.briefing-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 20px;
    padding: 15px var(--tt-space-4);
    box-shadow: var(--tt-elevation-3);
    position: relative;
    overflow: hidden;
}

.briefing-card__stamp {
    position: absolute;
    top: 13px;
    right: 14px;
    width: 46px;
    height: 46px;
    border: 2px solid rgba(194, 75, 49, 0.5);
    border-radius: 50%;
    transform: rotate(-14deg);
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: var(--tt-fs-caption);
    font-weight: 900;
    color: rgba(194, 75, 49, 0.6);
    letter-spacing: 0.05em;
}

.briefing-card__badges {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.briefing-card__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
    white-space: nowrap;
}

.briefing-card__badge--danger {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
}

.briefing-card__badge--streak {
    background: var(--tt-accent-subtle);
    color: var(--tt-accent-deep);
    font-family: var(--tt-font-mono);
}

.briefing-card__details {
    margin-top: var(--tt-space-3);
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.briefing-card__row {
    display: flex;
    gap: var(--tt-space-3);
    align-items: center;
}

.briefing-card__label {
    flex: none;
    width: 76px;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-hint);
    letter-spacing: 0.06em;
}

.briefing-card__value {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
}

.briefing-card__prosecutor-badge {
    display: inline-flex;
    align-items: center;
    gap: var(--tt-space-2);
    background: var(--tt-info-subtle);
    border: 1px solid var(--tt-info-subtle-border);
    border-radius: var(--tt-radius-full);
    padding: 4px 10px 4px 5px;
    cursor: pointer;
    color: var(--tt-info);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    border: 1px solid var(--tt-info-subtle-border);
}

.briefing-card__prosecutor-avatar {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    background: var(--tt-white);
    display: flex;
    align-items: center;
    justify-content: center;
    overflow: hidden;
}

.briefing-card__prosecutor-avatar img {
    width: 19px;
    height: 19px;
    object-fit: contain;
}

.briefing-card__prosecutor-name {
    color: var(--tt-info);
}

.briefing-card__gauge-section {
    margin-top: var(--tt-space-3);
    border-top: 1px dashed var(--tt-border-divider);
    padding-top: 14px;
}

.briefing-card__gauge-wrap {
    position: relative;
    padding-top: 22px;
}

.briefing-card__gauge-tangi {
    position: absolute;
    top: 0;
    transform: translateX(-50%);
    width: 32px;
    height: 32px;
    object-fit: contain;
    animation: tt-tangi-float 3.2s ease-in-out infinite;
}

@keyframes tt-tangi-float {
    0%,
    100% {
        transform: translateX(-50%) translateY(0);
    }
    50% {
        transform: translateX(-50%) translateY(-5px);
    }
}

.briefing-card__gauge-track {
    position: relative;
    height: 10px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
}

.briefing-card__gauge-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    background: var(--tt-accent);
    transition: width 0.4s ease;
}

.briefing-card__gauge-marker {
    position: absolute;
    right: 0;
    top: -6px;
    width: 2px;
    height: 22px;
    border-radius: 1px;
    background: var(--tt-border-divider);
}

.briefing-card__gauge-labels {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: var(--tt-space-2);
}

.briefing-card__gauge-remain {
    font-size: var(--tt-fs-badge);
    color: var(--tt-text-muted);
}

.briefing-card__gauge-remain b {
    color: var(--tt-accent-deep);
}

.briefing-card__gauge-limit {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
</style>
