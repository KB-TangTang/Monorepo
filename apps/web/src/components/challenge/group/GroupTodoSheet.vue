<script setup>
import { ref, computed } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import GroupTodoItem from './GroupTodoItem.vue';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    /** 마감 임박순 정렬된 전체 TO-DO 아이템 배열 */
    items: { type: Array, required: true },
    /** 아이템 id → { text, urgent } 맵 */
    countdowns: { type: Object, required: true },
});

const emit = defineEmits(['update:modelValue', 'open']);

const sheetRef = ref(null);
defineExpose({
    releaseHistory: () => sheetRef.value?.releaseHistory?.(),
});

const filter = ref('all');

const FILTERS = [
    { key: 'all', label: '전체' },
    { key: 'accuse', label: '기소' },
    { key: 'vote', label: '투표' },
];

const counts = computed(() => ({
    all: props.items.length,
    accuse: props.items.filter(i => i.type === 'accuse').length,
    vote: props.items.filter(i => i.type === 'vote').length,
}));

const filteredItems = computed(() => {
    if (filter.value === 'all') return props.items;
    return props.items.filter(i => i.type === filter.value);
});

const isEmpty = computed(() => filteredItems.value.length === 0);

function close() {
    emit('update:modelValue', false);
}

function onOpen(item) {
    emit('open', item);
}

function setFilter(key) {
    filter.value = key;
}
</script>

<template>
    <BaseBottomSheet
        ref="sheetRef"
        :model-value="modelValue"
        height="60vh"
        @update:model-value="emit('update:modelValue', $event)"
    >
        <template #header>
            <div class="todo-sheet__head">
                <span class="todo-sheet__title">처리할 일 {{ counts.all }}건</span>
                <span class="todo-sheet__sort">마감 임박순</span>
            </div>
        </template>

        <!-- 필터 칩 -->
        <div class="todo-sheet__filters">
            <button
                v-for="f in FILTERS"
                :key="f.key"
                :class="['todo-sheet__chip', filter === f.key ? 'todo-sheet__chip--active' : '']"
                @click="setFilter(f.key)"
            >
                {{ f.label }} {{ counts[f.key] }}
            </button>
        </div>

        <!-- 목록 -->
        <div class="todo-sheet__list">
            <GroupTodoItem
                v-for="item in filteredItems"
                :key="item.id"
                :item="item"
                :countdown="countdowns[item.id] || { text: '--:--:--', urgent: false }"
                @open="onOpen"
            />
            <div v-if="isEmpty" class="todo-sheet__empty">
                해당하는 일이 없어요
            </div>
        </div>

        <template #footer>
            <button class="todo-sheet__close" @click="close">닫기</button>
        </template>
    </BaseBottomSheet>
</template>

<style scoped>
/* ── 헤더 ───────────────────────────── */
.todo-sheet__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
}
.todo-sheet__title {
    font-size: 17px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}
.todo-sheet__sort {
    font-size: 12px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-bold);
}

/* ── 필터 칩 ─────────────────────────── */
.todo-sheet__filters {
    display: flex;
    gap: 6px;
    margin-bottom: 4px;
    flex: none;
}
.todo-sheet__chip {
    font-size: 12px;
    font-weight: var(--tt-fw-black);
    font-family: inherit;
    padding: 7px 13px;
    border-radius: var(--tt-radius-full);
    border: none;
    cursor: pointer;
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
    transition: background 0.15s ease, color 0.15s ease;
}
.todo-sheet__chip--active {
    background: var(--tt-surface-inverse);
    color: var(--tt-text-inverse, #fff);
}

/* ── 목록 ───────────────────────────── */
.todo-sheet__list {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
}

.todo-sheet__empty {
    padding: 34px 0;
    text-align: center;
    font-size: 12.5px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
}

/* ── 닫기 ───────────────────────────── */
.todo-sheet__close {
    width: 100%;
    text-align: center;
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    font-family: inherit;
    color: var(--tt-text-muted);
    background: none;
    border: none;
    cursor: pointer;
    padding: 4px 0;
    transition: color 0.15s ease;
}
.todo-sheet__close:active {
    color: var(--tt-text);
}
</style>
