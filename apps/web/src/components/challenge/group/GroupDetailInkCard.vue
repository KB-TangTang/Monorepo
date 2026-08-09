<!--
  Ink 스탯 카드 — 그룹 챌린지 상세화면의 핵심 지표 카드.
  어두운 배경(--tt-ink) 위에 한도/소비액/프로그레스 바를 표시한다.
  evalType(DAILY/PERIOD)과 status(RECRUITING/ACTIVE)에 따라 레이아웃이 달라진다.
-->
<script setup>
import { computed } from 'vue';

import mascotCalm from '@/assets/images/emotions/03_gentle_smile.png';
import mascotSmile from '@/assets/images/emotions/04_proud.png';
import mascotAnxious from '@/assets/images/emotions/17_anxious.png';

const props = defineProps({
    evalType: { type: String, required: true },
    status: { type: String, required: true },
    limitAmount: { type: Number, required: true },
    categoryName: { type: String, default: '총 소비' },
    /* 진행 중 전용 */
    currentAmount: { type: Number, default: 0 },
    usagePercent: { type: Number, default: 0 },
    remainingAmount: { type: Number, default: 0 },
});

const isActive = computed(() => props.status === 'ACTIVE');
const isDaily = computed(() => props.evalType === 'DAILY');

const evalLabel = computed(() => isDaily.value ? '일일 결산' : '기간 결산');
const limitSuffix = computed(() => isDaily.value ? '/ 하루' : '/ 기간 전체');
const limitLabel = computed(() => {
    if (isActive.value) {
        return isDaily.value
            ? `한도 ${props.limitAmount.toLocaleString()}원`
            : `기간 한도 ${props.limitAmount.toLocaleString()}원`;
    }
    /* 시작 전: evalType + 카테고리에 맞는 설명 */
    const catDesc = props.categoryName === '총 소비'
        ? '모든 카테고리 소비를 합산해'
        : `${props.categoryName} 카테고리 소비를 합산해`;
    if (isDaily.value) {
        return props.limitAmount > 0
            ? `${catDesc} 매일 자정 판정`
            : `${catDesc} 매일 무지출 판정`;
    }
    return `${catDesc} 기간 종료 후 판정`;
});

/* 진행률 색상 구간 */
const progressColor = computed(() => {
    if (props.usagePercent >= 80) return 'var(--tt-red)';
    if (props.usagePercent >= 50) return 'var(--tt-gold)';
    return 'var(--tt-green)';
});

const statusBadge = computed(() => {
    if (props.usagePercent >= 80) return { label: `위험 ${props.usagePercent}%`, bg: 'rgba(224,102,75,.22)', color: '#F5A78F' };
    if (props.usagePercent >= 50) return { label: `보통 ${props.usagePercent}%`, bg: 'rgba(245,185,33,.2)', color: '#F5B921' };
    return { label: `여유 ${Math.round(100 - props.usagePercent)}%`, bg: 'rgba(46,158,107,.2)', color: '#7FD3AC' };
});

const remainingColor = computed(() => {
    if (props.usagePercent >= 80) return '#F5A78F';
    if (props.usagePercent >= 50) return '#F5B921';
    return '#7FD3AC';
});

const mascotSrc = computed(() => {
    if (props.usagePercent >= 80) return mascotAnxious;
    if (props.usagePercent >= 50) return mascotSmile;
    return mascotCalm;
});

const barWidth = computed(() => Math.min(props.usagePercent, 100) + '%');
const mascotLeft = computed(() => Math.min(props.usagePercent, 100) + '%');
</script>

<template>
    <div class="ink-card">
        <div class="ink-card__deco"></div>

        <!-- 뱃지 행 -->
        <div class="ink-card__badges">
            <span
                class="ink-card__badge"
                :class="isDaily ? 'ink-card__badge--eval' : 'ink-card__badge--eval-period'"
            >{{ evalLabel }}</span>
            <span class="ink-card__badge ink-card__badge--category">{{ categoryName }}</span>
            <span
                v-if="isActive"
                class="ink-card__badge ink-card__badge--status"
                :style="{ background: statusBadge.bg, color: statusBadge.color }"
            >
                {{ statusBadge.label }}
            </span>
        </div>

        <!-- 금액 -->
        <div class="ink-card__amount">
            <span class="ink-card__number">
                {{ isActive ? currentAmount.toLocaleString() : limitAmount.toLocaleString() }}
            </span>
            <span class="ink-card__unit">원</span>
            <span v-if="!isActive" class="ink-card__suffix">{{ limitSuffix }}</span>
        </div>

        <!-- 진행 바 (진행 중만) -->
        <div v-if="isActive" class="ink-card__progress">
            <div class="ink-card__mascot-wrap" :style="{ left: mascotLeft }">
                <img
                    :src="mascotSrc"
                    alt="탕이"
                    class="ink-card__mascot"
                >
            </div>
            <div class="ink-card__track">
                <div
                    class="ink-card__bar"
                    :style="{ width: barWidth, background: progressColor }"
                ></div>
                <div class="ink-card__limit-line"></div>
            </div>
            <div class="ink-card__progress-labels">
                <span class="ink-card__remaining">
                    한도까지
                    <b :style="{ color: remainingColor }">{{ remainingAmount.toLocaleString() }}원</b>
                </span>
                <span class="ink-card__limit-text">{{ limitLabel }}</span>
            </div>
        </div>

        <!-- 설명 (시작 전만) -->
        <p v-if="!isActive" class="ink-card__desc">
            {{ limitLabel }}
        </p>
    </div>
</template>

<style scoped>
.ink-card {
    background: var(--tt-ink);
    border-radius: var(--tt-radius-xl);
    padding: 13px 16px 12px;
    position: relative;
    overflow: hidden;
    box-shadow: var(--tt-elevation-4);
}

.ink-card__deco {
    position: absolute;
    right: -22px;
    top: -22px;
    width: 96px;
    height: 96px;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.04);
}

/* ── 뱃지 ── */
.ink-card__badges {
    display: flex;
    align-items: center;
    gap: 6px;
    position: relative;
    z-index: 2;
}

.ink-card__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.ink-card__badge--eval {
    background: rgba(245, 185, 33, 0.16);
    color: var(--tt-gold);
}

.ink-card__badge--eval-period {
    background: rgba(62, 99, 214, 0.28);
    color: #B9C6F2;
}

.ink-card__badge--category {
    background: var(--tt-gold);
    color: var(--tt-ink);
}

.ink-card__badge--status {
    margin-left: auto;
}

/* ── 금액 ── */
.ink-card__amount {
    display: flex;
    align-items: baseline;
    gap: 6px;
    margin-top: 12px;
    position: relative;
    z-index: 2;
}

.ink-card__number {
    font-size: var(--tt-fs-stat);
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
    letter-spacing: -0.02em;
}

.ink-card__unit {
    font-size: 16px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-gold);
}

.ink-card__suffix {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: #AEB2CC;
    margin-left: 2px;
}

/* ── 프로그레스 바 ── */
.ink-card__progress {
    position: relative;
    z-index: 2;
    margin-top: 4px;
    padding-top: 20px;
}

.ink-card__mascot-wrap {
    position: absolute;
    top: 0;
    width: 32px;
    height: 32px;
    transform: translateX(-50%);
}

.ink-card__mascot {
    width: 32px;
    height: 32px;
    object-fit: contain;
    animation: tt-float 3.2s ease-in-out infinite;
}

.ink-card__track {
    position: relative;
    height: 10px;
    border-radius: var(--tt-radius-full);
    background: rgba(255, 255, 255, 0.14);
}

.ink-card__bar {
    height: 100%;
    border-radius: var(--tt-radius-full);
    transition: width 0.6s ease;
}

.ink-card__limit-line {
    position: absolute;
    right: 0;
    top: -6px;
    width: 2px;
    height: 22px;
    border-radius: 1px;
    background: rgba(255, 255, 255, 0.45);
}

.ink-card__progress-labels {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 6px;
}

.ink-card__remaining {
    font-size: var(--tt-fs-badge);
    color: #AEB2CC;
}

.ink-card__limit-text {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-white);
}

/* ── 설명 (시작 전) ── */
.ink-card__desc {
    margin-top: 9px;
    font-size: var(--tt-fs-badge);
    color: #AEB2CC;
    position: relative;
    z-index: 2;
}

/* ── 탕이 부유 애니메이션 ── */
@keyframes tt-float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-5px); }
}
</style>
