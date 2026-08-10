<script setup>
import { computed } from 'vue';
import { ClockIcon } from '@heroicons/vue/24/solid';

const props = defineProps({
    /** TO-DO 아이템 객체 */
    item: { type: Object, required: true },
    /** 카운트다운 포맷 결과 { text, urgent } */
    countdown: { type: Object, required: true },
});

const emit = defineEmits(['open']);

const isAccuse = computed(() => props.item.type === 'accuse');

const subtitle = computed(() => {
    const i = props.item;
    if (isAccuse.value) {
        return { pre: '청구 ', emph: `${i.amount?.toLocaleString()}원`, post: ` · ${i.challengeName}` };
    }
    return { pre: `${i.tally} · ${i.challengeName}`, emph: '', post: '' };
});

const btnLabel = computed(() => isAccuse.value ? '변론' : '투표');
</script>

<template>
    <div class="todo-item" @click="emit('open', item)">
        <div :class="['todo-item__icon', isAccuse ? 'todo-item__icon--accuse' : 'todo-item__icon--vote']">
            <svg v-if="isAccuse" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="todo-item__glyph"><path d="M1 21h12v2H1zM5.245 8.07l2.83-2.827 14.14 14.14-2.828 2.83zM12.317 1l5.657 5.657-2.83 2.83-5.654-5.66zM3.825 9.485l5.657 5.657-2.828 2.828-5.657-5.657z"/></svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" class="todo-item__glyph"><path d="M18 13h-.68l-2 2h1.91L19 17H5l1.78-2h2.05l-2-2H6l-3 3v4c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-4l-3-3zm-1-5.05l-4.95 4.95-3.54-3.54 4.95-4.95 3.54 3.54zm-4.24-5.66L6.39 8.66c-.39.39-.39 1.02 0 1.41l4.95 4.95c.39.39 1.02.39 1.41 0l6.36-6.36c.39-.39.39-1.02 0-1.41L14.16 2.3c-.38-.4-1.01-.4-1.4-.01z"/></svg>
        </div>

        <div class="todo-item__body">
            <div class="todo-item__title">{{ item.title }}</div>
            <div class="todo-item__sub">
                {{ subtitle.pre }}<b v-if="subtitle.emph" class="todo-item__emph">{{ subtitle.emph }}</b>{{ subtitle.post }}
            </div>
        </div>

        <div class="todo-item__right">
            <span :class="['todo-item__chip', countdown.urgent ? 'todo-item__chip--urgent' : '']">
                <ClockIcon class="todo-item__clock" />{{ countdown.text }}
            </span>
            <button
                :class="['todo-item__btn', isAccuse ? 'todo-item__btn--accuse' : 'todo-item__btn--vote']"
                @click.stop="emit('open', item)"
            >
                {{ btnLabel }}
            </button>
        </div>
    </div>
</template>

<style scoped>
.todo-item {
    display: flex;
    align-items: center;
    gap: 11px;
    padding: 11px 2px;
    border-top: 1px solid var(--tt-border-light);
    cursor: pointer;
    animation: tt-rowin 0.28s ease-out both;
    transition: opacity 0.15s ease;
}
.todo-item:active {
    opacity: 0.72;
}

/* ── 아이콘 ─────────────────────────── */
.todo-item__icon {
    width: 38px;
    height: 38px;
    border-radius: 13px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}
.todo-item__icon--accuse { background: var(--tt-red-soft); }
.todo-item__icon--vote   { background: var(--tt-gold-soft); }

.todo-item__glyph {
    width: 20px;
    height: 20px;
}
.todo-item__icon--accuse .todo-item__glyph { color: var(--tt-red-deep); }
.todo-item__icon--vote .todo-item__glyph   { color: var(--tt-gold-deep); }

/* ── 본문 ───────────────────────────── */
.todo-item__body {
    flex: 1;
    min-width: 0;
}
.todo-item__title {
    font-size: 13px;
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.todo-item__sub {
    font-size: 11px;
    color: var(--tt-text-muted);
    margin-top: 3px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
.todo-item__emph {
    color: var(--tt-red-deep);
}

/* ── 우측 칩 + 버튼 ─────────────────── */
.todo-item__right {
    flex: none;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 4px;
}

.todo-item__chip {
    display: inline-flex;
    align-items: center;
    gap: 3px;
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    padding: 3px 8px;
    border-radius: var(--tt-radius-full);
    font-family: ui-monospace, Menlo, monospace;
    background: var(--tt-bg-fill);
    color: var(--tt-text-hint);
}
.todo-item__chip--urgent {
    background: var(--tt-red-soft);
    color: var(--tt-red-deep);
}
.todo-item__clock {
    width: 11px;
    height: 11px;
    flex: none;
}

.todo-item__btn {
    font-size: 12px;
    font-weight: var(--tt-fw-black);
    font-family: inherit;
    padding: 7px 14px;
    border-radius: var(--tt-radius-full);
    border: none;
    cursor: pointer;
    transition: filter 0.15s ease;
}
.todo-item__btn:active { filter: brightness(0.93); }
.todo-item__btn--accuse { background: var(--tt-red); color: var(--tt-white); }
.todo-item__btn--vote   { background: var(--tt-accent); color: var(--tt-text); }

@keyframes tt-rowin {
    0% { transform: translateY(8px); opacity: 0; }
    100% { transform: none; opacity: 1; }
}
</style>
