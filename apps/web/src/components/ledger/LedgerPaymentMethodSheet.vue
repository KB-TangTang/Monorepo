<!--
  용도: 결제수단으로 거래내역을 좁히는 바텀시트. "전체 수단" + 이번 달에 실제로 쓰인 수단만 보여준다.
  언제 쓰는지: LedgerView 한 곳에서만 렌더한다.
-->
<script setup>
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';

defineProps({
    modelValue: { type: Boolean, required: true },
    methods: { type: Array, default: () => [] },
    selected: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue', 'select']);

function choose(method) {
    emit('select', method);
    emit('update:modelValue', false);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        title="결제수단 선택"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <ul class="payment-sheet__list">
            <li>
                <button
                    type="button"
                    :class="{ 'payment-sheet__active': selected === '' }"
                    @click="choose('')"
                >
                    전체 수단
                </button>
            </li>
            <li v-for="method in methods" :key="method">
                <button
                    type="button"
                    :class="{ 'payment-sheet__active': selected === method }"
                    @click="choose(method)"
                >
                    {{ method }}
                </button>
            </li>
        </ul>
    </BaseBottomSheet>
</template>

<style scoped>
.payment-sheet__list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.payment-sheet__list button {
    width: 100%;
    padding: var(--tt-space-3) var(--tt-space-4);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    text-align: left;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    cursor: pointer;
}

.payment-sheet__active {
    color: var(--tt-text-inverse);
    background: var(--tt-text);
    border-color: var(--tt-text);
}
</style>
