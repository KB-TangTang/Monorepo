<!--
  용도: 개인 미션 홈의 「이번 주 판정」 카드. 월~일 일곱 칸과 연속 성공 일수만 담는다.
  이번 달 누적 점수는 PersonalScoreCard 로 분리했다 — 주간과 월간은 보는 주기가 달라
  한 카드에 묶어두면 어느 쪽 이야기인지 매번 다시 읽어야 한다.
-->
<script setup>
defineProps({
    weekDays: { type: Array, default: () => [] },
    streakDays: { type: Number, default: 0 },
    prosecutorImage: { type: String, default: '' },
});

function dayAriaLabel(status) {
    if (status === 'success') return '인정';
    if (status === 'failed') return '기각';
    if (status === 'today') return '오늘 · 판정 전';
    return '판정 없음';
}
</script>

<template>
    <!-- 제목은 카드 밖 섹션 헤더에 있다. 여기서 또 쓰면 같은 말이 두 번 나온다. -->
    <div class="weekly-card">
        <div class="weekly-card__grid">
            <div v-for="day in weekDays" :key="day.dow" class="weekly-card__day">
                <small class="weekly-card__dow">{{ day.dow }}</small>
                <div
                    class="weekly-card__circle"
                    :class="`weekly-card__circle--${day.status}`"
                    :aria-label="`${day.dow}요일 ${dayAriaLabel(day.status)}`"
                >
                    <img
                        v-if="day.status === 'today' && prosecutorImage"
                        :src="prosecutorImage"
                        alt=""
                        class="weekly-card__tangi"
                    />
                    <!-- 인정: 채운 초록 + 흰 체크 / 기각: 연한 빨강 + X.
                         기각을 성공과 같은 채도로 채우면 실패가 이어진 주가 붉은 벽이 된다. -->
                    <svg
                        v-else-if="day.status === 'success'"
                        width="18"
                        height="18"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="3"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        aria-hidden="true"
                    >
                        <path d="M5 12.5l4.5 4.5L19 7" />
                    </svg>
                    <svg
                        v-else-if="day.status === 'failed'"
                        width="15"
                        height="15"
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="3"
                        stroke-linecap="round"
                        aria-hidden="true"
                    >
                        <path d="M6 6l12 12M18 6L6 18" />
                    </svg>
                </div>
            </div>
        </div>

        <p class="weekly-card__streak">🔥 연속 성공 {{ streakDays }}일째</p>
    </div>
</template>

<style scoped>
.weekly-card {
    background: var(--tt-bg);
    border-radius: var(--tt-radius-lg);
    padding: 14px var(--tt-space-4);
}

.weekly-card__grid {
    display: grid;
    grid-template-columns: repeat(7, 1fr);
    gap: 4px;
}

.weekly-card__day {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 5px;
}

.weekly-card__dow {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
}

.weekly-card__circle {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    border-radius: 50%;
    border: 1.5px solid var(--tt-border);
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
}

/*
 * 인정만 꽉 채운다. 한 주에서 눈에 먼저 들어와야 하는 건 성공한 날이다.
 * 색은 앱 전체가 「성공」에 쓰는 초록(--tt-success)을 따른다.
 * 테두리도 같은 초록으로 칠한다 — 투명으로 두면 1.5px 테두리 자리에
 * 아래 배경이 비쳐 원 가장자리가 각지게 보인다.
 */
.weekly-card__circle--success {
    background: var(--tt-success);
    border-color: var(--tt-success);
    color: var(--tt-text-inverse);
}

/* 기각은 같은 채도로 채우지 않는다 — 실패가 이어진 주가 붉은 벽이 된다 */
.weekly-card__circle--failed {
    background: var(--tt-danger-subtle);
    border-color: var(--tt-danger-subtle-border);
    color: var(--tt-danger);
}

.weekly-card__circle--today {
    background: var(--tt-surface-inverse);
    border-color: var(--tt-surface-inverse);
    color: var(--tt-accent);
}

.weekly-card__circle--pending {
    background: var(--tt-bg-fill);
    border-color: var(--tt-border);
    color: var(--tt-text-hint);
}

.weekly-card__tangi {
    width: 26px;
    height: 26px;
    object-fit: contain;
}

.weekly-card__streak {
    margin: 11px 0 0;
    text-align: center;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

@media (max-width: 359px) {
    .weekly-card__circle {
        width: 30px;
        height: 30px;
    }
}
</style>
