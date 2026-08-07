<!--
  기간 카드 — 기간결산 챌린지의 LivesBar 대체 컴포넌트.
  시작 전(RECRUITING)에는 소비 집계 기간(시작·종료 날짜 박스),
  진행 중(ACTIVE)에는 남은 기간 프로그레스 바를 표시한다.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    startDate: { type: String, required: true },
    endDate: { type: String, required: true },
    totalDays: { type: Number, required: true },
    /** 'upcoming' | 'active' */
    mode: { type: String, default: 'active' },
    /** 진행 중 전용 */
    currentDay: { type: Number, default: 0 },
});

const isUpcoming = computed(() => props.mode === 'upcoming');

/* 시작 전: 시작일/종료일 */
const startLabel = computed(() => {
    const d = new Date(props.startDate);
    return `${d.getMonth() + 1}/${d.getDate()} 00:00`;
});
const endLabel = computed(() => {
    const d = new Date(props.endDate);
    return `${d.getMonth() + 1}/${d.getDate()} 23:59`;
});

/* 진행 중: 남은 기간 */
const remainingDays = computed(() => props.totalDays - props.currentDay);
const remainingLabel = computed(() => `${remainingDays.value}일 남음`);
const progressWidth = computed(() =>
    Math.min(Math.round((props.currentDay / props.totalDays) * 100), 100) + '%'
);

/* 날짜 간략 표시 */
const startShort = computed(() => {
    const d = new Date(props.startDate);
    return `${d.getMonth() + 1}/${d.getDate()} 시작`;
});
const endShort = computed(() => {
    const d = new Date(props.endDate);
    const next = new Date(d);
    next.setDate(next.getDate() + 1);
    return `${d.getMonth() + 1}/${d.getDate()} 종료 · ${next.getMonth() + 1}/${next.getDate()} 판정`;
});
</script>

<template>
    <div class="period-bar">
        <!-- 시작 전: 소비 집계 기간 -->
        <template v-if="isUpcoming">
            <div class="period-bar__header">
                <span class="period-bar__title">소비 집계 기간</span>
                <span class="period-bar__accent">{{ totalDays }}일간</span>
            </div>
            <div class="period-bar__dates">
                <div class="period-bar__date-box">
                    <div class="period-bar__date-label">시작</div>
                    <div class="period-bar__date-value">{{ startLabel }}</div>
                </div>
                <span class="period-bar__arrow">→</span>
                <div class="period-bar__date-box">
                    <div class="period-bar__date-label">종료</div>
                    <div class="period-bar__date-value">{{ endLabel }}</div>
                </div>
            </div>
        </template>

        <!-- 진행 중: 남은 기간 프로그레스 -->
        <template v-else>
            <div class="period-bar__header">
                <span class="period-bar__title">남은 기간</span>
                <span class="period-bar__accent">{{ remainingLabel }}</span>
            </div>
            <div class="period-bar__track">
                <div
                    class="period-bar__fill"
                    :style="{ width: progressWidth }"
                ></div>
            </div>
            <div class="period-bar__labels">
                <span class="period-bar__label">{{ startShort }}</span>
                <span class="period-bar__label">{{ endShort }}</span>
            </div>
        </template>
    </div>
</template>

<style scoped>
.period-bar {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 11px 15px;
}

.period-bar__header {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
}

.period-bar__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.period-bar__accent {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-blue);
}

/* ── 시작 전: 날짜 박스 ── */
.period-bar__dates {
    margin-top: 9px;
    display: flex;
    align-items: center;
    gap: 9px;
}

.period-bar__date-box {
    flex: 1;
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    border-radius: 12px;
    padding: 6px 11px;
}

.period-bar__date-label {
    font-size: 10.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

.period-bar__date-value {
    font-size: 13.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    margin-top: 2px;
}

.period-bar__arrow {
    color: var(--tt-border-strong);
    font-weight: var(--tt-fw-black);
    flex: none;
}

/* ── 진행 중: 프로그레스 바 ── */
.period-bar__track {
    margin-top: 5px;
    height: 6px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-bg-fill);
    overflow: hidden;
}

.period-bar__fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    background: var(--tt-blue);
    transition: width 0.6s ease;
}

.period-bar__labels {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin-top: 3px;
}

.period-bar__label {
    font-size: 10.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}
</style>
