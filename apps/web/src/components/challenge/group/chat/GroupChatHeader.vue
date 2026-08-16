<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

/*
 * roomInfo 는 api/groupChatAdapter.js 가 정규화한 모양이다
 * ({ groupId, groupName, status, memberCount, unreadCount, isEnded, isJudging }).
 * 서버는 진행 일차·D-day 를 주지 않으므로 화면에서도 만들지 않는다 — 예전엔 없는 필드를 읽어
 * "undefined일차 · D-undefined" 가 찍혔다.
 */
const props = defineProps({
    roomInfo: { type: Object, default: null },
});

const emit = defineEmits(['open-menu']);
const router = useRouter();

function goBack() {
    router.back();
}

const subInfo = computed(() => {
    if (!props.roomInfo) return '';
    return `멤버 ${props.roomInfo.memberCount}명`;
});
</script>

<template>
    <header class="chat-header">
        <div class="chat-header__row">
            <button class="chat-header__back" @click="goBack">
                <svg width="9" height="16" viewBox="0 0 9 16" fill="none">
                    <path d="M8 1L1 8l7 7" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
            </button>

            <div class="chat-header__info">
                <div class="chat-header__title-row">
                    <span class="chat-header__title">
                        {{ roomInfo?.groupName ?? '' }}
                    </span>
                    <span v-if="roomInfo?.isJudging" class="chat-header__badge chat-header__badge--trial">
                        재판 중
                    </span>
                    <span v-if="roomInfo?.isEnded" class="chat-header__badge chat-header__badge--ended">
                        종료됨
                    </span>
                </div>
                <div class="chat-header__sub">{{ subInfo }}</div>
            </div>

            <button class="chat-header__menu" @click="emit('open-menu')">
                <span class="chat-header__dot" />
                <span class="chat-header__dot" />
                <span class="chat-header__dot" />
            </button>
        </div>
    </header>
</template>

<style scoped>
.chat-header {
    flex: none;
    background: var(--tt-bg);
    border-bottom: 1px solid var(--tt-border);
    padding: var(--tt-space-3) var(--tt-screen-padding) var(--tt-space-3);
}

.chat-header__row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
}

.chat-header__back {
    border: none;
    background: none;
    color: var(--tt-text);
    cursor: pointer;
    padding: var(--tt-space-2);
    flex: none;
    display: flex;
    align-items: center;
}

.chat-header__info {
    flex: 1;
    min-width: 0;
}

.chat-header__title-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.chat-header__title {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    color: var(--tt-text);
}

.chat-header__badge {
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
    flex: none;
}

.chat-header__badge--trial {
    background: var(--tt-accent-subtle);
    color: var(--tt-accent-deep);
}

.chat-header__badge--ended {
    background: var(--tt-bg-fill);
    color: var(--tt-text-muted);
}

.chat-header__sub {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
    margin-top: 3px;
}

.chat-header__menu {
    border: none;
    background: none;
    cursor: pointer;
    display: flex;
    gap: 3px;
    padding: 6px 2px;
    flex: none;
}

.chat-header__dot {
    width: 4px;
    height: 4px;
    border-radius: 50%;
    background: var(--tt-text-hint);
}
</style>
