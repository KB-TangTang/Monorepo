<script setup>
import { computed } from 'vue';
import GroupTodoItem from './GroupTodoItem.vue';
import mascotAnxious from '@/assets/images/emotions/17_anxious.png';

const props = defineProps({
    /** 마감 임박순 정렬된 전체 TO-DO 아이템 배열 */
    items: { type: Array, required: true },
    /** 아이템 id → { text, urgent } 맵 */
    countdowns: { type: Object, required: true },
    /** 홈에 표시할 최대 건수 */
    visibleCount: { type: Number, default: 2 },
});

const emit = defineEmits(['open', 'open-sheet']);

const visibleItems = computed(() => props.items.slice(0, props.visibleCount));
const restItems = computed(() => props.items.slice(props.visibleCount));
const hasRest = computed(() => restItems.value.length > 0);

/** 아이콘 스택용 (최대 3개) */
const stackIcons = computed(() =>
    restItems.value.slice(0, 3).map((item, idx) => ({
        type: item.type,
        offset: idx > 0,
    }))
);

const mascotLine = computed(() => {
    const total = props.items.length;
    if (total > 2) return `밀린 일이 ${total}건! 급한 것부터 처리해요`;
    return `조금만 더! ${total}건 남았어요`;
});
</script>

<template>
    <div class="todo-card">
        <!-- 헤더 -->
        <div class="todo-card__header">
            <div class="todo-card__status">
                <div class="todo-card__dot" />
                <span class="todo-card__label">TO-DO · 처리할 일 {{ items.length }}건</span>
            </div>
            <span class="todo-card__sort">마감 임박순</span>
        </div>

        <!-- 아이템 목록 (상위 N건) -->
        <GroupTodoItem
            v-for="item in visibleItems"
            :key="item.id"
            :item="item"
            :countdown="countdowns[item.id] || { text: '--:--:--', urgent: false }"
            @open="emit('open', $event)"
        />

        <!-- 남은 건 접기 -->
        <div v-if="hasRest" class="todo-card__rest" @click="emit('open-sheet')">
            <div class="todo-card__stack">
                <div
                    v-for="(s, i) in stackIcons"
                    :key="i"
                    :class="['todo-card__stack-icon', s.type === 'accuse' ? 'todo-card__stack-icon--accuse' : 'todo-card__stack-icon--vote', s.offset ? 'todo-card__stack-icon--offset' : '']"
                >
                    <svg v-if="s.type === 'accuse'" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="todo-card__stack-glyph"><path d="M1 21h12v2H1zM5.245 8.07l2.83-2.827 14.14 14.14-2.828 2.83zM12.317 1l5.657 5.657-2.83 2.83-5.654-5.66zM3.825 9.485l5.657 5.657-2.828 2.828-5.657-5.657z"/></svg>
                    <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="todo-card__stack-glyph"><path d="M18 13h-.68l-2 2h1.91L19 17H5l1.78-2h2.05l-2-2H6l-3 3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-4l-3-3zm-1-5.05l-4.95 4.95-3.54-3.54 4.95-4.95 3.54 3.54zm-4.24-5.66L6.39 8.66c-.39.39-.39 1.02 0 1.41l4.95 4.95c.39.39 1.02.39 1.41 0l6.36-6.36c.39-.39.39-1.02 0-1.41L14.16 2.3c-.38-.4-1.01-.4-1.4-.01z"/></svg>
                </div>
            </div>
            <span class="todo-card__rest-label">남은 {{ restItems.length }}건 모두 보기</span>
        </div>
    </div>

    <!-- 마스코트 영역 -->
    <div class="todo-mascot">
        <div class="todo-mascot__stage">
            <div class="todo-mascot__line" />
            <div class="todo-mascot__glow" />
            <div class="todo-mascot__float">
                <img :src="mascotAnxious" alt="바쁜 탕이" class="todo-mascot__img" />
            </div>
        </div>
        <div class="todo-mascot__text">{{ mascotLine }}</div>
    </div>
</template>

<style scoped>
.todo-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border-light);
    border-radius: 22px;
    box-shadow: 0 12px 28px rgba(35, 40, 66, 0.1);
    padding: 14px 15px 0;
}

/* ── 헤더 ───────────────────────────── */
.todo-card__header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 2px 6px;
}
.todo-card__status {
    display: flex;
    align-items: center;
    gap: 8px;
}
.todo-card__dot {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: var(--tt-red);
    flex: none;
    animation: tt-tick 1.6s ease-in-out infinite;
}
.todo-card__label {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.08em;
    color: var(--tt-red-deep);
}
.todo-card__sort {
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 9px;
    border-radius: var(--tt-radius-full);
}

/* ── 남은 건 접기 ───────────────────── */
.todo-card__rest {
    border-top: 1px solid var(--tt-border-light);
    padding: 11px 2px;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
    cursor: pointer;
    transition: opacity 0.15s ease;
}
.todo-card__rest:active { opacity: 0.7; }

.todo-card__stack {
    display: flex;
}
.todo-card__stack-icon {
    width: 20px;
    height: 20px;
    border-radius: 7px;
    border: 2px solid var(--tt-bg);
    display: flex;
    align-items: center;
    justify-content: center;
}
.todo-card__stack-icon--offset { margin-left: -6px; }
.todo-card__stack-icon--accuse { background: var(--tt-red-soft); }
.todo-card__stack-icon--vote   { background: var(--tt-gold-soft); }

.todo-card__stack-glyph {
    width: 12px;
    height: 12px;
}
.todo-card__stack-icon--accuse .todo-card__stack-glyph { color: var(--tt-red-deep); }
.todo-card__stack-icon--vote .todo-card__stack-glyph   { color: var(--tt-gold-deep); }

.todo-card__rest-label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-primary);
}

/* ── 마스코트 ───────────────────────── */
.todo-mascot {
    margin-top: 10px;
    padding: 0 6px;
}
.todo-mascot__stage {
    height: 46px;
    position: relative;
}
.todo-mascot__line {
    position: absolute;
    left: 14px;
    right: 14px;
    bottom: 6px;
    height: 1.5px;
    background: rgba(35, 40, 66, 0.07);
}
.todo-mascot__glow {
    position: absolute;
    left: 50%;
    bottom: 0;
    width: 58px;
    height: 58px;
    margin-left: -29px;
    border-radius: 50%;
    background: radial-gradient(circle, var(--tt-red-soft) 0%, rgba(251, 233, 228, 0) 70%);
    animation: tt-breathe 4.6s ease-in-out infinite;
}
.todo-mascot__float {
    position: absolute;
    left: 50%;
    bottom: 2px;
    width: 46px;
    height: 46px;
    margin-left: -23px;
    animation: tt-float 3.2s ease-in-out infinite;
}
.todo-mascot__img {
    width: 46px;
    height: 46px;
    object-fit: contain;
}
.todo-mascot__text {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    text-align: center;
}

/* ── 키프레임 ───────────────────────── */
@keyframes tt-tick {
    0%, 100% { opacity: 1; }
    50% { opacity: 0.45; }
}
@keyframes tt-float {
    0%, 100% { transform: translateY(0); }
    50% { transform: translateY(-5px); }
}
@keyframes tt-breathe {
    0%, 100% { transform: scale(1); opacity: 0.32; }
    50% { transform: scale(1.07); opacity: 0.18; }
}
</style>
