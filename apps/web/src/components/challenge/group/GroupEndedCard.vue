<script setup>
import { computed } from 'vue';

const props = defineProps({
    challenge: { type: Object, required: true },
});

const isWin = computed(() => props.challenge.finalOutcome === 'SURVIVED');
const resultLabel = computed(() => (isWin.value ? '승소' : '패소'));
const resultBg = computed(() => (isWin.value ? 'var(--tt-green-soft)' : 'var(--tt-red-soft)'));
const resultColor = computed(() => (isWin.value ? 'var(--tt-green)' : 'var(--tt-red-deep)'));
const livesColor = computed(() => (isWin.value ? 'var(--tt-green)' : 'var(--tt-red-deep)'));

const formattedSavings = computed(() => {
    if (!props.challenge.savingsAmount) return null;
    return props.challenge.savingsAmount.toLocaleString();
});

/** ISO 날짜(2026-07-28)를 표시용(2026.07.28)으로 변환 */
const formattedEndDate = computed(() => {
    if (!props.challenge.endDate) return '';
    return props.challenge.endDate.replace(/-/g, '.');
});

/** 비피쳐 카드용 요약 텍스트를 데이터 필드에서 생성 */
const summaryText = computed(() => {
    const ch = props.challenge;
    const livesStr = `목숨 ${ch.livesCount} / ${ch.maxLives}`;
    if (isWin.value) {
        const parts = [livesStr];
        if (ch.finalRank) parts.push(`${ch.finalRank}위`);
        if (ch.savingsAmount) parts.push(`절약 ${ch.savingsAmount.toLocaleString()}원`);
        return parts.join(' · ');
    }
    const parts = [livesStr];
    if (ch.finalChargeAmount > 0) {
        parts.push(`벌금 ${ch.finalChargeAmount.toLocaleString()}원 정산 완료`);
    }
    return parts.join(' · ');
});
</script>

<template>
    <div class="gec-card">
        <!-- 상단: 이름 + 결과 뱃지 -->
        <div class="gec-card__top">
            <span class="gec-card__name">{{ challenge.groupName }}</span>
            <span
                class="gec-card__badge"
                :style="{ background: resultBg, color: resultColor }"
            >
                {{ resultLabel }}
            </span>
        </div>

        <!-- 중간: 종료 날짜 · 기간 · 멤버수 -->
        <p class="gec-card__desc">
            {{ formattedEndDate }} · {{ challenge.totalDays }}일 · {{ challenge.memberCount }}명
        </p>

        <!-- 하단 (스탯 그리드 — isFeatured) -->
        <div v-if="challenge.isFeatured" class="gec-card__stats">
            <div class="gec-stat">
                <span class="gec-stat__label">최종 순위</span>
                <span class="gec-stat__value">
                    {{ challenge.finalRank }}위
                    <span class="gec-stat__sub">/ {{ challenge.totalMembers }}명</span>
                </span>
            </div>
            <div class="gec-stat">
                <span class="gec-stat__label">남은 목숨</span>
                <span class="gec-stat__value" :style="{ color: livesColor }">
                    {{ challenge.livesCount }} / {{ challenge.maxLives }}
                </span>
            </div>
            <div class="gec-stat">
                <span class="gec-stat__label">절약액</span>
                <span class="gec-stat__value">{{ formattedSavings }}원</span>
            </div>
        </div>

        <!-- 하단 (요약 + 판결기록 — !isFeatured) -->
        <div v-else class="gec-card__summary">
            <span class="gec-card__summary-text">{{ summaryText }}</span>
            <span class="gec-card__link">판결기록 ›</span>
        </div>
    </div>
</template>

<style scoped>
.gec-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 14px 16px;
}

.gec-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gec-card__name {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gec-card__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.gec-card__desc {
    margin: 8px 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    line-height: var(--tt-lh-normal);
}

/* ── 스탯 그리드 ────────────── */
.gec-card__stats {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 8px;
    margin-top: 12px;
}

.gec-stat {
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-sm);
    padding: 10px 8px;
    text-align: center;
}

.gec-stat__label {
    display: block;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
    margin-bottom: 4px;
}

.gec-stat__value {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gec-stat__sub {
    font-weight: var(--tt-fw-regular);
    color: var(--tt-text-muted);
}

/* ── 요약 한 줄 ────────────── */
.gec-card__summary {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 12px;
}

.gec-card__summary-text {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.gec-card__link {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-blue);
    white-space: nowrap;
}
</style>
