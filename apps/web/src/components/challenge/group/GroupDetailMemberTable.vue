<!--
  멤버 일일 소비 상태 테이블 — 진행 중 상세화면에서
  다른 멤버(나 제외)의 오늘 소비 금액/비율/프로그레스를 리스트로 표시한다.
  초과한 멤버는 배경이 빨갛게 하이라이트된다.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    members: { type: Array, required: true },
    evalType: { type: String, default: 'DAILY' },
    limitAmount: { type: Number, default: 0 },
});

const isDaily = computed(() => props.evalType === 'DAILY');
const sectionTitle = computed(() => isDaily.value ? '오늘의 소비 상태' : '멤버 누적 상태');
const sectionSub = computed(() =>
    `나 제외 ${props.members.length}명 · 한도 ${props.limitAmount.toLocaleString()}원`
);

function barColor(percent, isExceeded) {
    if (isExceeded || percent >= 100) return 'var(--tt-red)';
    if (percent >= 50) return 'var(--tt-gold)';
    return 'var(--tt-green)';
}

function textColor(percent, isExceeded) {
    if (isExceeded || percent >= 100) return 'var(--tt-red-deep)';
    if (percent >= 50) return 'var(--tt-gold-deep)';
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
                    'member-table__row--exceeded': m.isExceeded,
                    'member-table__row--bordered': idx < members.length - 1 && !m.isExceeded,
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
                    <span
                        v-if="m.isExceeded"
                        class="member-table__exceeded-badge"
                    >초과</span>
                    <span
                        class="member-table__amount"
                        :style="{ color: textColor(m.usagePercent, m.isExceeded) }"
                    >
                        {{ m.dailyAmount.toLocaleString() }}원
                    </span>
                    <span
                        class="member-table__percent"
                        :style="{ color: textColor(m.usagePercent, m.isExceeded) }"
                    >
                        {{ m.usagePercent }}%
                    </span>
                </div>
                <div class="member-table__bar-track" :class="{ 'member-table__bar-track--exceeded': m.isExceeded }">
                    <div
                        class="member-table__bar-fill"
                        :style="{
                            width: Math.min(m.usagePercent, 100) + '%',
                            background: barColor(m.usagePercent, m.isExceeded),
                        }"
                    ></div>
                </div>
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
    padding: 3px 13px;
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

.member-table__exceeded-badge {
    background: var(--tt-red-soft);
    color: var(--tt-red-deep);
    font-size: 9px;
    font-weight: var(--tt-fw-black);
    padding: 1px 5px;
    border-radius: var(--tt-radius-full);
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
</style>
