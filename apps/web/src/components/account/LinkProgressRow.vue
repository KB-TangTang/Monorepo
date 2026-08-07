<!--
  용도: 조회 진행 화면의 기관 한 줄. 이름 + 상태(대기·조회중·완료·실패)를 보여준다.
  언제 쓰는지: LinkProgressView 안에서만. 상태 판정은 utils 의 순수 함수가 하고 여기서는 그리기만 한다.

  상태 표기는 Figma 「동기화 상태」 화면의 방식을 따른다 —
  **색점 + 라벨 pill**. 아이콘 글리프(✓ ⟳)만 쓰면 상태가 목록에서 눈에 안 들어온다.
  홀짝 줄무늬는 부모가 :nth-child 로 넣는다(--tt-surface-row).
-->
<script setup>
import { computed } from 'vue';
import { resolveProgressRow } from '@/utils/account';

const props = defineProps({
    name: { type: String, required: true },
    status: { type: String, default: 'WAITING' },
});

const row = computed(() => resolveProgressRow(props.status));
const spinning = computed(() => props.status === 'FETCHING');
</script>

<template>
    <li class="progress-row">
        <span class="progress-row__name">{{ name }}</span>
        <span class="progress-row__state" :class="`progress-row__state--${row.variant}`">
            <span
                class="progress-row__dot"
                :class="{ 'progress-row__dot--spin': spinning }"
                aria-hidden="true"
            ></span>
            {{ row.label }}
        </span>
    </li>
</template>

<style scoped>
.progress-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4);
}

.progress-row__name {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.progress-row__state {
    display: inline-flex;
    align-items: center;
    gap: var(--tt-space-2);
    flex-shrink: 0;
    padding: var(--tt-space-1) var(--tt-space-3);
    border-radius: var(--tt-radius-full);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}

.progress-row__dot {
    width: 6px;
    height: 6px;
    border-radius: var(--tt-radius-full);
    background: currentColor;
}

/* 조회 중인 기관만 점이 맥동한다 — 어디가 진행 중인지 한눈에 보인다. */
.progress-row__dot--spin {
    animation: progress-row-pulse 1s ease-in-out infinite;
}

@keyframes progress-row-pulse {
    0%,
    100% {
        opacity: 1;
        transform: scale(1);
    }
    50% {
        opacity: 0.35;
        transform: scale(0.7);
    }
}

@media (prefers-reduced-motion: reduce) {
    .progress-row__dot--spin {
        animation: none;
    }
}

.progress-row__state--innocent {
    background: var(--tt-success-subtle);
    color: var(--tt-success);
}

.progress-row__state--progress {
    background: var(--tt-primary-subtle);
    color: var(--tt-primary);
}

.progress-row__state--guilty {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger);
}

.progress-row__state--default {
    background: var(--tt-bg-subtle);
    color: var(--tt-text-muted);
}
</style>
