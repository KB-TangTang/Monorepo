<!--
  멤버 소비 상태 — 진행 중 상세화면에서
  다른 멤버(나 제외)의 소비 금액/비율/프로그레스를 2열 카드 그리드로 표시한다.
  초과(일일) · 위험(기간 ≥80%) 멤버는 카드 배경이 빨갛게 하이라이트된다.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    members: { type: Array, required: true },
    evalType: { type: String, default: 'DAILY' },
});

const isDaily = computed(() => props.evalType === 'DAILY');
const sectionTitle = computed(() => isDaily.value ? '오늘의 소비 상태' : '멤버 누적 상태');

/** 기간결산에서 한도의 80% 이상 도달했지만 아직 초과는 아닌 상태 */
function isNearLimit(m) {
    return !isDaily.value && !m.isExceeded && m.usagePercent >= 80;
}

/** 카드 배경을 빨갛게 표시해야 하는지 (초과 or 위험) */
function isHighlighted(m) {
    return m.isExceeded || isNearLimit(m);
}

function barColor(m) {
    if (m.isExceeded || m.usagePercent >= 100 || isNearLimit(m)) return 'var(--tt-red)';
    if (m.usagePercent >= 50) return 'var(--tt-gold)';
    return 'var(--tt-green)';
}

function textColor(m) {
    if (m.isExceeded || m.usagePercent >= 100 || isNearLimit(m)) return 'var(--tt-red-deep)';
    if (m.usagePercent >= 50) return 'var(--tt-gold-deep)';
    return 'var(--tt-green)';
}
</script>

<template>
    <div class="member-status">
        <div class="member-status__header">
            <span class="member-status__title">{{ sectionTitle }}</span>
        </div>

        <div class="member-status__grid">
            <div
                v-for="m in members"
                :key="m.userId"
                class="member-status__card"
                :class="{ 'member-status__card--exceeded': isHighlighted(m) }"
            >
                <!-- 프로필 영역 -->
                <div class="member-status__profile">
                    <img
                        v-if="m.profileImage"
                        :src="m.profileImage"
                        :alt="m.nickname"
                        class="member-status__avatar-img"
                    >
                    <div
                        v-else
                        class="member-status__avatar"
                        :style="{ background: m.avatarColor }"
                    >
                        {{ m.initial }}
                    </div>
                    <div class="member-status__name-row">
                        <span class="member-status__name">{{ m.nickname }}</span>
                        <!-- 초과 시 사이렌 -->
                        <svg
                            v-if="m.isExceeded"
                            class="member-status__siren"
                            viewBox="0 0 24 24"
                            fill="none"
                        >
                            <line x1="5" y1="10" x2="2.5" y2="8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" opacity="0.35" />
                            <line x1="19" y1="10" x2="21.5" y2="8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" opacity="0.35" />
                            <path d="M8 14c0-4 2-7 4-7s4 3 4 7" fill="currentColor" />
                            <rect x="6" y="14" width="12" height="3" rx="1" fill="currentColor" />
                            <rect x="4.5" y="17" width="15" height="2.5" rx="1" fill="var(--tt-text-hint)" />
                        </svg>
                    </div>
                    <!-- 상태 뱃지 슬롯 (항상 고정 높이 — 프로필 위치 고정) -->
                    <div class="member-status__badge-slot">
                        <span
                            v-if="isDaily && m.isExceeded && m.trialStatus"
                            class="member-status__badge member-status__badge--trial"
                        >재판중</span>
                        <span
                            v-else-if="isNearLimit(m)"
                            class="member-status__badge member-status__badge--danger"
                        >위험</span>
                    </div>
                </div>

                <!-- 프로그레스 바 + 퍼센트 + 금액 -->
                <div class="member-status__bottom">
                    <div
                        class="member-status__bar-track"
                        :class="{ 'member-status__bar-track--exceeded': isHighlighted(m) }"
                    >
                        <div
                            class="member-status__bar-fill"
                            :style="{
                                width: Math.min(m.usagePercent, 100) + '%',
                                background: barColor(m),
                            }"
                        ></div>
                    </div>

                    <div class="member-status__stats">
                        <span
                            class="member-status__percent"
                            :style="{ color: textColor(m) }"
                        >{{ m.usagePercent }}%</span>
                        <span
                            class="member-status__amount"
                            :style="{ color: textColor(m) }"
                        >{{ m.dailyAmount.toLocaleString() }}원</span>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.member-status {
    margin-top: 8px;
}

.member-status__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

/* ── 2열 그리드 ── */
.member-status__grid {
    margin-top: 9px;
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
}

/* ── 개별 카드 ── */
.member-status__card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 14px 12px 12px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;
    aspect-ratio: 1 / 1.15;
}

.member-status__card--exceeded {
    background: #FDF4F1;
    border-color: #F0D2C9;
}

/* ── 프로필 ── */
.member-status__profile {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    flex: 1;
    justify-content: center;
}

.member-status__avatar-img {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    object-fit: cover;
    border: 2px solid var(--tt-border);
}

.member-status__avatar {
    width: 64px;
    height: 64px;
    border-radius: 50%;
    color: var(--tt-white);
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
}

.member-status__name-row {
    display: flex;
    align-items: center;
    gap: 4px;
}

.member-status__name {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.member-status__siren {
    width: 14px;
    height: 14px;
    color: var(--tt-red-deep);
    flex: none;
    animation: siren-flash 0.8s ease-in-out infinite alternate;
}

@keyframes siren-flash {
    0% { color: var(--tt-red-deep); opacity: 1; }
    100% { color: var(--tt-red); opacity: 0.4; }
}

.member-status__badge-slot {
    height: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
}

.member-status__badge {
    font-size: 9px;
    font-weight: var(--tt-fw-black);
    padding: 2px 7px;
    border-radius: var(--tt-radius-full);
}

.member-status__badge--trial {
    background: #FBE9E4;
    color: #C24B31;
}

.member-status__badge--danger {
    background: #FBE9E4;
    color: #C24B31;
}

/* ── 하단: 바 + 수치 ── */
.member-status__bottom {
    width: 80%;
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.member-status__bar-track {
    width: 100%;
    height: 6px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg-fill);
    overflow: hidden;
}

.member-status__bar-track--exceeded {
    background: #F3DFDA;
}

.member-status__bar-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    transition: width 0.6s ease;
}

/* ── 퍼센트 + 금액 ── */
.member-status__stats {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.member-status__percent {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
}

.member-status__amount {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
}
</style>
