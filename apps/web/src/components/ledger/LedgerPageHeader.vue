<!--
  용도: 전체 거래내역 화면 상단. 제목 + 가맹점 검색 토글을 담당한다.
  언제 쓰는지: LedgerView 한 곳에서만 렌더한다.
-->
<script setup>
import { ref, watch } from 'vue';
import BaseInput from '@/components/common/BaseInput.vue';

const props = defineProps({
    modelValue: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue']);

const isSearchOpen = ref(false);

function toggleSearch() {
    isSearchOpen.value = !isSearchOpen.value;
    if (!isSearchOpen.value) {
        emit('update:modelValue', '');
    }
}

watch(
    () => props.modelValue,
    (value) => {
        if (value && !isSearchOpen.value) {
            isSearchOpen.value = true;
        }
    },
);
</script>

<template>
    <header class="ledger-header">
        <div class="ledger-header__row">
            <h1 class="ledger-header__title">전체 거래내역</h1>
            <button
                type="button"
                class="ledger-header__search-toggle"
                :aria-pressed="isSearchOpen"
                aria-label="가맹점 검색"
                @click="toggleSearch"
            >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="11" cy="11" r="6.5" />
                    <path d="m20 20-4.3-4.3" />
                </svg>
            </button>
        </div>

        <BaseInput
            v-if="isSearchOpen"
            class="ledger-header__search"
            :model-value="modelValue"
            placeholder="가맹점명으로 검색"
            @update:model-value="$emit('update:modelValue', $event)"
        />
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

.ledger-header__search-toggle[aria-pressed='true'] {
    color: var(--tt-primary);
    background: var(--tt-primary-subtle);
    border-color: var(--tt-primary-subtle);
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
