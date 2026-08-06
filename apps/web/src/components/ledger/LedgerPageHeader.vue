<!--
  용도: 전체 거래내역 화면 상단. 제목 + 검색 화면 진입 버튼을 담당한다.
  언제 쓰는지: LedgerView 한 곳에서만 렌더한다.
-->
<script setup>
defineEmits(['open-search']);
</script>

<template>
    <header class="ledger-header">
        <div class="ledger-header__row">
            <h1 class="ledger-header__title">전체 거래내역</h1>
            <button
                type="button"
                class="ledger-header__search-toggle"
                aria-label="거래내역 검색으로 이동"
                @click="$emit('open-search')"
            >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="11" cy="11" r="6.5" />
                    <path d="m20 20-4.3-4.3" />
                </svg>
            </button>
        </div>
    </header>
</template>

<style scoped>
.ledger-header {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.ledger-header__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
}

.ledger-header__title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.ledger-header__search-toggle {
    display: grid;
    flex-shrink: 0;
    width: 40px;
    height: 40px;
    color: var(--tt-text);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-full);
    cursor: pointer;
    place-items: center;
}

.ledger-header__search-toggle svg {
    width: 18px;
    height: 18px;
    fill: none;
    stroke: currentColor;
    stroke-width: 2;
    stroke-linecap: round;
}
</style>
