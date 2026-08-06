<script setup>
import { computed } from 'vue';

const props = defineProps({
    challenge: { type: Object, required: true },
});

const STATUS_MAP = {
    TRIAL_PENDING: { label: '판결 대기', bg: 'var(--tt-gold-soft)', color: 'var(--tt-gold-deep)', bar: 'var(--tt-gold)', life: 'var(--tt-gold)' },
    OBJECTION: { label: '이의 심리', bg: 'var(--tt-blue-soft)', color: 'var(--tt-blue)', bar: 'var(--tt-blue)', life: 'var(--tt-blue)' },
    PEACEFUL: { label: '평온', bg: 'var(--tt-bg-fill)', color: 'var(--tt-text-body)', bar: 'var(--tt-green)', life: 'var(--tt-green)' },
};

const statusStyle = computed(() => STATUS_MAP[props.challenge.status] || STATUS_MAP.PEACEFUL);

const badgeText = computed(() => {
    const s = statusStyle.value;
    if (props.challenge.status === 'TRIAL_PENDING' && props.challenge.trialCount > 0) {
        return `${s.label} ${props.challenge.trialCount}`;
    }
    return s.label;
});

const progressPercent = computed(() => {
    if (!props.challenge.totalDays) return 0;
    return Math.round((props.challenge.currentDay / props.challenge.totalDays) * 100);
});

const limitDesc = computed(() => {
    const ch = props.challenge;
    const type = ch.evalType === 'DAILY' ? '일일결산' : '기간평가';
    if (ch.dailyLimit) return `${type} · 일 ${ch.dailyLimit.toLocaleString()}원`;
    if (ch.totalLimit) return `${type} · 총 ${ch.totalLimit.toLocaleString()}원`;
    return type;
});
</script>

<template>
    <div class="gac-card">
        <!-- 상단: 이름 + 상태 뱃지 -->
        <div class="gac-card__top">
            <span class="gac-card__name">{{ challenge.name }}</span>
            <span
                class="gac-card__badge"
                :style="{ background: statusStyle.bg, color: statusStyle.color }"
            >
                {{ badgeText }}
            </span>
        </div>

        <!-- 중간: 설명 + 진행률 + 목숨 -->
        <p class="gac-card__desc">
            {{ limitDesc }} · {{ challenge.currentDay }}일차
        </p>
        <div class="gac-card__progress">
            <div class="gac-progress-track">
                <div
                    class="gac-progress-fill"
                    :style="{ width: progressPercent + '%', background: statusStyle.bar }"
                />
            </div>
            <span class="gac-card__lives" :style="{ color: statusStyle.life }">
                {{ challenge.lives }}/{{ challenge.maxLives }}
            </span>
        </div>

        <!-- 하단: 마감/참여 -->
        <div class="gac-card__bottom">
            <template v-if="challenge.deadlineLabel">
                <span class="gac-card__deadline">
                    마감 {{ challenge.deadlineLabel }}
                </span>
            </template>
            <template v-else>
                <span class="gac-card__peaceful-label">열린 재판 없음</span>
            </template>
            <span class="gac-card__members">{{ challenge.memberCount }}명 참여 ›</span>
        </div>
    </div>
</template>

<style scoped>
.gac-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 14px 16px;
}

.gac-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gac-card__name {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gac-card__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.gac-card__desc {
    margin: 8px 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    line-height: var(--tt-lh-normal);
}

.gac-card__progress {
    margin-top: 10px;
    display: flex;
    align-items: center;
    gap: 9px;
}

.gac-progress-track {
    flex: 1;
    height: 8px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
    overflow: hidden;
}

.gac-progress-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    transition: width 0.3s ease;
}

.gac-card__lives {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
}

.gac-card__bottom {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
}

.gac-card__deadline {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-red-deep);
}

.gac-card__peaceful-label {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.gac-card__members {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}
</style>
