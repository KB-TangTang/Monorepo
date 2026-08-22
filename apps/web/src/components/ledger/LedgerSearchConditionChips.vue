<!--
  용도: 검색 화면 상단의 조건 칩 목록. "가맹점 · 검색어" / "카테고리 · 값" 은 값이 있을 때만 나타나고
  X 로 지울 수 있다.
  언제 쓰는지: LedgerSearchView 한 곳에서만 렌더한다.
-->
<script setup>
const DIRECTION_LABELS = {
    CONSUMPTION: '지출',
    INCOME: '입금',
};

defineProps({
    merchantTerm: { type: String, default: '' },
    category: { type: String, default: '' },
    direction: { type: String, default: 'ALL' },
});

defineEmits(['remove-merchant', 'open-category', 'remove-category', 'remove-direction']);
</script>

<template>
    <div class="search-chips" role="list" aria-label="검색 조건">
        <span v-if="merchantTerm" role="listitem" class="search-chips__chip search-chips__chip--active">
            가맹점 · {{ merchantTerm }}
            <button type="button" aria-label="가맹점 조건 지우기" @click="$emit('remove-merchant')">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" /></svg>
            </button>
        </span>

        <span v-if="category" role="listitem" class="search-chips__chip search-chips__chip--active">
            카테고리 · {{ category }}
            <button type="button" aria-label="카테고리 조건 지우기" @click="$emit('remove-category')">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" /></svg>
            </button>
        </span>
        <button v-else type="button" class="search-chips__chip" @click="$emit('open-category')">
            카테고리
        </button>

        <span
            v-if="direction !== 'ALL'"
            role="listitem"
            class="search-chips__chip search-chips__chip--active"
        >
            구분 · {{ DIRECTION_LABELS[direction] }}
            <button type="button" aria-label="구분 조건 지우기" @click="$emit('remove-direction')">
                <svg viewBox="0 0 24 24" aria-hidden="true"><path d="m6 6 12 12M18 6 6 18" /></svg>
            </button>
        </span>
    </div>
</template>

<style scoped>
.search-chips {
    display: flex;
    flex-wrap: wrap;
    gap: var(--tt-space-2);
}

.search-chips__chip {
    display: inline-flex;
    align-items: center;
    gap: var(--tt-space-1);
    padding: var(--tt-space-2) var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    white-space: nowrap;
    cursor: pointer;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
}

.search-chips__chip--active {
    color: var(--tt-text-inverse);
    background: var(--tt-text);
    border-color: var(--tt-text);
}

.search-chips__chip button {
    display: grid;
    width: 14px;
    height: 14px;
    padding: 0;
    color: inherit;
    cursor: pointer;
    background: transparent;
    border: 0;
    place-items: center;
}

.search-chips__chip svg {
    width: 12px;
    height: 12px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2.4;
    stroke-linecap: round;
}
</style>
