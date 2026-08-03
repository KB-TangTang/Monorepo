<!--
  용도: 데이터가 0건일 때 자리를 채우는 빈 상태. 아이콘 · 제목 · 설명 · 액션 슬롯으로 구성된다.
  언제 쓰는지: 목록 · 카드 영역이 비었을 때. 다음 행동(CTA)을 함께 제시할 수 있다.
  쓰면 안 되는 경우: 조회에 실패했을 때(StateError), 아직 불러오는 중일 때(StateLoading).
-->
<script setup>
defineProps({
    title: { type: String, default: '아직 아무것도 없어요' },
    description: { type: String, default: '' },
});
</script>

<template>
    <div class="tt-state">
        <div class="tt-state__icon">
            <slot name="icon">
                <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    stroke-width="1.75"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    aria-hidden="true"
                >
                    <path d="M4 7.5h16v11a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 18.5z" />
                    <path d="M4 7.5 6.5 4h11L20 7.5" />
                    <path d="M9.5 11.5h5" />
                </svg>
            </slot>
        </div>
        <p class="tt-state__title">{{ title }}</p>
        <p v-if="description" class="tt-state__desc">{{ description }}</p>
        <div v-if="$slots.action" class="tt-state__action"><slot name="action" /></div>
    </div>
</template>

<style scoped>
.tt-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-2);
    padding: var(--tt-space-10) var(--tt-space-5);
    font-family: var(--tt-font-sans);
    text-align: center;
}

.tt-state__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 56px;
    height: 56px;
    margin-bottom: var(--tt-space-1);
    color: var(--tt-gray-500);
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-full);
}

.tt-state__icon :deep(svg) {
    width: 28px;
    height: 28px;
}

.tt-state__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.tt-state__desc {
    max-width: 26em;
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.tt-state__action {
    margin-top: var(--tt-space-3);
}
</style>
