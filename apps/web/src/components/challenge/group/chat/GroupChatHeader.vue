<script setup>
import { computed } from 'vue';
import { useRouter } from 'vue-router';

/*
 * roomInfo 는 api/groupChatAdapter.js 가 정규화한 모양이다
 * ({ groupId, groupName, status, memberCount, unreadCount, dayIndex, daysLeft, isEnded, isJudging }).
 *
 * 일차·D-day 는 서버가 계산해 내려준다. 기기 시계·시간대에 따라 값이 달라지면 안 되는 값이다.
 */
const props = defineProps({
    roomInfo: { type: Object, default: null },
});

const router = useRouter();

function goBack() {
    router.back();
}

/* 있는 조각만 이어 붙인다. 시작 전(dayIndex 0)·종료 후(daysLeft 음수)에는 해당 칸을 생략한다 */
const metaLine = computed(() => {
    const info = props.roomInfo;
    if (!info) return '';

    const parts = [`멤버 ${info.memberCount}`];
    if (info.dayIndex > 0) parts.push(`${info.dayIndex}일차`);
    if (info.daysLeft > 0) parts.push(`D-${info.daysLeft}`);
    else if (info.daysLeft === 0) parts.push('오늘 종료');
    return parts.join(' · ');
});
</script>

<template>
    <header class="chat-header">
        <button class="chat-header__back" aria-label="뒤로" @click="goBack">
            <svg width="9" height="16" viewBox="0 0 9 16" fill="none" aria-hidden="true">
                <path
                    d="M8 1L1 8l7 7"
                    stroke="currentColor"
                    stroke-width="2.2"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                />
            </svg>
        </button>

        <div class="chat-header__info">
            <div class="chat-header__title-row">
                <h1 class="chat-header__title">{{ roomInfo?.groupName ?? '' }}</h1>
                <span
                    v-if="roomInfo?.isJudging"
                    class="chat-header__badge chat-header__badge--trial"
                >
                    재판 중
                </span>
                <span
                    v-else-if="roomInfo?.isEnded"
                    class="chat-header__badge chat-header__badge--ended"
                >
                    종료됨
                </span>
            </div>
            <p class="chat-header__meta">{{ metaLine }}</p>
        </div>
    </header>
</template>

<style scoped>
.chat-header {
    flex: none;
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    background: var(--tt-bg);
    border-bottom: 1px solid var(--tt-border);
    padding: var(--tt-space-3) var(--tt-screen-padding);
    padding-top: max(var(--tt-space-3), env(safe-area-inset-top));
}

.chat-header__back {
    border: none;
    background: none;
    color: var(--tt-text);
    cursor: pointer;
    padding: var(--tt-space-2);
    margin-left: calc(var(--tt-space-2) * -1);
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
    gap: 6px;
    min-width: 0;
}

.chat-header__title {
    margin: 0;
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.015em;
    color: var(--tt-text);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.chat-header__badge {
    flex: none;
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.chat-header__badge--trial {
    background: var(--tt-danger-subtle);
    color: var(--tt-danger-deep);
}

.chat-header__badge--ended {
    background: var(--tt-bg-fill);
    color: var(--tt-text-muted);
}

.chat-header__meta {
    margin: 3px 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-semibold);
}

</style>
