<script setup>
/**
 * 그룹 챌린지 홈 — 평온 상태 카드
 * 기소·투표 이벤트가 모두 없을 때 이벤트 카드 자리에 렌더링된다.
 */
</script>

<template>
    <div class="gc-peaceful">
        <div class="gc-peaceful__header">
            <div class="gc-peaceful__status">
                <span class="gc-peaceful__dot" />
                <span class="gc-peaceful__label">PEACEFUL · 처리할 일 0건</span>
            </div>
            <span class="gc-peaceful__badge">이상 없음</span>
        </div>
        <div class="gc-peaceful__title">기소도 투표도 없어요</div>
        <div class="gc-peaceful__subtitle">오늘까지 모두 기준 안에서 소비했어요</div>
    </div>
</template>

<style scoped>
/* 재판 현황 카드와 같은 자리에 서는 상태 카드다. 그림자를 맞춰야 상태가 바뀔 때 층이 흔들리지 않는다 */
.gc-peaceful {
    background: var(--tt-bg);
    border-radius: var(--tt-radius-xl);
    padding: 16px 17px;
    box-shadow: var(--tt-elevation-2);
}

/* ── 상단 상태 행 ───────────────────────── */
.gc-peaceful__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-peaceful__status {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.gc-peaceful__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--tt-green);
    flex: none;
}

.gc-peaceful__label {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.08em;
    color: var(--tt-green);
}

.gc-peaceful__badge {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-green-soft);
    color: var(--tt-green);
}

/* ── 제목 + 설명 ────────────────────────── */
.gc-peaceful__title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    margin-top: 11px;
    color: var(--tt-text);
}

.gc-peaceful__subtitle {
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
    margin-top: 4px;
}
</style>
