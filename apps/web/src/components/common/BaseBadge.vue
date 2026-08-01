<!--
  용도: 상태를 한 조각으로 표시하는 배지. 진행 중 · 일일결산 · 기간결산 · 마감 카운트다운 · 유죄 · 무혐의를 커버한다.
  언제 쓰는지: 카드 · 목록 항목에 상태 라벨을 붙일 때. 마감처럼 수치가 흐르는 값은 deadline 이 모노 폰트로 그린다.
  쓰면 안 되는 경우: 누를 수 있는 요소(BaseButton), 긴 문장. 배지는 한 줄짜리 짧은 라벨 전용이다.
-->
<script setup>
defineProps({
    variant: {
        type: String,
        default: 'default',
        validator: (v) =>
            ['default', 'progress', 'daily', 'period', 'deadline', 'guilty', 'innocent'].includes(
                v,
            ),
    },
});
</script>

<template>
    <span class="tt-badge" :class="`tt-badge--${variant}`"><slot /></span>
</template>

<style scoped>
.tt-badge {
    display: inline-flex;
    align-items: center;
    gap: var(--tt-space-1);
    padding: 3px var(--tt-space-2);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    line-height: 1.5;
    white-space: nowrap;
    border: 1px solid transparent;
    border-radius: var(--tt-radius-full);
}

/* 기본 — 중립 라벨 */
.tt-badge--default {
    color: var(--tt-text-muted);
    background: var(--tt-gray-100);
}

/* 진행 중 N — 브랜드 톤 */
.tt-badge--progress {
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
}

/* 일일결산 — 강조(골드) */
.tt-badge--daily {
    color: var(--tt-accent-700);
    background: var(--tt-accent-subtle);
}

/* 기간결산 — 강조 아웃라인 */
.tt-badge--period {
    color: var(--tt-accent-700);
    background: transparent;
    border-color: var(--tt-accent);
}

/* 마감 02:14:03 — 남은 시간. 숫자가 흔들리지 않게 모노 폰트 */
.tt-badge--deadline {
    font-family: var(--tt-font-mono);
    font-variant-numeric: tabular-nums;
    color: var(--tt-text-inverse);
    background: var(--tt-gray-900);
    border-radius: var(--tt-radius-sm);
}

/* 유죄 */
.tt-badge--guilty {
    color: var(--tt-danger);
    background: var(--tt-danger-subtle);
    border-color: var(--tt-guilty-300);
}

/* 무혐의 처분 */
.tt-badge--innocent {
    color: var(--tt-innocent-800);
    background: var(--tt-success-subtle);
    border-color: var(--tt-innocent-300);
}
</style>
