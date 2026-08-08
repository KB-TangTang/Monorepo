<!--
  멤버 소비 상태 테이블 — 진행 중 상세화면에서
  다른 멤버(나 제외)의 소비 금액/비율/프로그레스를 리스트로 표시한다.
  초과(일일) · 위험(기간 ≥80%) 멤버는 배경이 빨갛게 하이라이트된다.
  하단에 그룹 채팅 바로가기 행이 붙는다.
-->
<script setup>
import { computed } from 'vue';
import { ChevronRightIcon } from '@heroicons/vue/24/solid';
import mascotChat from '@/assets/images/emotions/57_chat.png';

const props = defineProps({
    members: { type: Array, required: true },
    evalType: { type: String, default: 'DAILY' },
    limitAmount: { type: Number, default: 0 },
    chat: { type: Object, default: null },
});

const isDaily = computed(() => props.evalType === 'DAILY');
const sectionTitle = computed(() => isDaily.value ? '오늘의 소비 상태' : '멤버 누적 상태');
const sectionSub = computed(() =>
    `나 제외 ${props.members.length}명 · 한도 ${props.limitAmount.toLocaleString()}원`
);

/** 기간결산에서 한도의 80% 이상 도달했지만 아직 초과는 아닌 상태 */
function isNearLimit(m) {
    return !isDaily.value && !m.isExceeded && m.usagePercent >= 80;
}

/** 행 배경을 빨갛게 표시해야 하는지 (초과 or 위험) */
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
    <div class="member-table">
        <div class="member-table__header">
            <span class="member-table__title">{{ sectionTitle }}</span>
            <span class="member-table__sub">{{ sectionSub }}</span>
        </div>

        <div class="member-table__list">
            <div
                v-for="(m, idx) in members"
                :key="m.userId"
                class="member-table__row"
                :class="{
                    'member-table__row--exceeded': isHighlighted(m),
                    'member-table__row--bordered': idx < members.length - 1 && !isHighlighted(m),
                }"
            >
                <div class="member-table__info">
                    <div
                        class="member-table__avatar"
                        :style="{ background: m.avatarColor }"
                    >
                        {{ m.initial }}
                    </div>
                    <span class="member-table__name">{{ m.nickname }}</span>

                    <!-- 일일결산: 초과 시 사이렌 + 재판중 -->
                    <svg
                        v-if="m.isExceeded"
                        class="member-table__siren"
                        viewBox="0 0 24 24"
                        fill="none"
                    >
                        <line x1="5" y1="10" x2="2.5" y2="8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" opacity="0.35" />
                        <line x1="19" y1="10" x2="21.5" y2="8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" opacity="0.35" />
                        <path d="M8 14c0-4 2-7 4-7s4 3 4 7" fill="currentColor" />
                        <rect x="6" y="14" width="12" height="3" rx="1" fill="currentColor" />
                        <rect x="4.5" y="17" width="15" height="2.5" rx="1" fill="var(--tt-text-hint)" />
                    </svg>
                    <span
                        v-if="isDaily && m.isExceeded && m.trialStatus"
                        class="member-table__trial-label"
                    >재판중</span>

                    <!-- 기간결산: 한도 근접 시 위험 뱃지 -->
                    <span v-if="isNearLimit(m)" class="member-table__danger-badge">위험</span>

                    <span
                        class="member-table__amount"
                        :style="{ color: textColor(m) }"
                    >
                        {{ m.dailyAmount.toLocaleString() }}원
                    </span>
                    <span
                        class="member-table__percent"
                        :style="{ color: textColor(m) }"
                    >
                        {{ m.usagePercent }}%
                    </span>
                </div>
                <div class="member-table__bar-track" :class="{ 'member-table__bar-track--exceeded': isHighlighted(m) }">
                    <div
                        class="member-table__bar-fill"
                        :style="{
                            width: Math.min(m.usagePercent, 100) + '%',
                            background: barColor(m),
                        }"
                    ></div>
                </div>
            </div>

            <!-- 그룹 채팅 링크 -->
            <div v-if="chat" class="member-table__chat-link">
                <img :src="mascotChat" alt="채팅" class="member-table__chat-icon">
                <div class="member-table__chat-body">
                    <span class="member-table__chat-title">그룹 채팅</span>
                    <span v-if="chat.lastMessage" class="member-table__chat-preview">
                        {{ chat.lastMessage.sender }}: {{ chat.lastMessage.text }}
                    </span>
                </div>
                <span v-if="chat.unreadCount" class="member-table__chat-badge">
                    {{ chat.unreadCount }}
                </span>
                <ChevronRightIcon class="member-table__chat-arrow" />
            </div>
        </div>
    </div>
</template>

<style scoped>
.member-table__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.member-table__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.member-table__sub {
    font-size: 11.5px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.member-table__list {
    margin-top: 9px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 16px;
    box-shadow: var(--tt-elevation-2);
    overflow: hidden;
}

.member-table__row {
    padding: 10px 13px;
}

.member-table__row--bordered {
    border-bottom: 1px solid var(--tt-bg-fill);
}

.member-table__row--exceeded {
    background: #FDF4F1;
}

.member-table__info {
    display: flex;
    align-items: center;
    gap: 7px;
}

.member-table__avatar {
    width: 22px;
    height: 22px;
    border-radius: 50%;
    color: var(--tt-white);
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.member-table__name {
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.member-table__siren {
    width: 18px;
    height: 18px;
    color: var(--tt-red-deep);
    flex: none;
    animation: siren-flash 0.8s ease-in-out infinite alternate;
}

@keyframes siren-flash {
    0% { color: var(--tt-red-deep); opacity: 1; }
    100% { color: var(--tt-red); opacity: 0.4; }
}

.member-table__trial-label {
    font-size: 9.5px;
    font-weight: var(--tt-fw-black);
    color: #C24B31;
    flex: none;
}

.member-table__danger-badge {
    background: #FBE9E4;
    color: #C24B31;
    font-size: 9px;
    font-weight: var(--tt-fw-black);
    padding: 1px 5px;
    border-radius: var(--tt-radius-full);
    flex: none;
}

.member-table__amount {
    margin-left: auto;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
}

.member-table__percent {
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    width: 34px;
    text-align: right;
}

.member-table__bar-track {
    margin-top: 4px;
    height: 6px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg-fill);
    overflow: hidden;
}

.member-table__bar-track--exceeded {
    background: #F3DFDA;
}

.member-table__bar-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    transition: width 0.6s ease;
}

/* ── 그룹 채팅 링크 ── */
.member-table__chat-link {
    padding: 11px 13px;
    border-top: 1px solid var(--tt-bg-fill);
    display: flex;
    align-items: center;
    gap: 9px;
    cursor: pointer;
    background: var(--tt-bg);
}

.member-table__chat-icon {
    width: 28px;
    height: 22px;
    object-fit: contain;
    flex: none;
}

.member-table__chat-body {
    flex: 1;
    min-width: 0;
}

.member-table__chat-title {
    display: block;
    font-size: 12px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.member-table__chat-preview {
    display: block;
    font-size: 11px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
    margin-top: 2px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.member-table__chat-badge {
    flex: none;
    min-width: 17px;
    height: 17px;
    padding: 0 5px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-red);
    color: var(--tt-white);
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
}

.member-table__chat-arrow {
    width: 18px;
    height: 18px;
    color: var(--tt-border-strong);
    flex: none;
}
</style>
