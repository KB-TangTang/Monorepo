<!--
  용도: 알림 목록의 한 줄. 아이콘 · 제목 · 본문 · 시간 · 액션 링크를 그린다.
  언제 쓰는지: NotificationListView 안에서만.
-->
<script setup>
import { computed } from 'vue';
import { formatRelativeTime, notificationVisual, resolveDeepLink } from '@/utils/notification';

const props = defineProps({
    notification: { type: Object, required: true },
    now: { type: Date, required: true },
});

const emit = defineEmits(['open']);

const visual = computed(() => notificationVisual(props.notification.type));
const timeLabel = computed(() => formatRelativeTime(props.notification.createdAt, props.now));
const linkable = computed(() => resolveDeepLink(props.notification.deepLinkUrl) !== null);
</script>

<template>
    <button
        type="button"
        class="tt-noti"
        :class="[`tt-noti--${visual.tone}`, { 'tt-noti--unread': !notification.isRead }]"
        @click="emit('open', notification)"
    >
        <span class="tt-noti__icon" :data-icon="visual.icon" aria-hidden="true"></span>
        <span class="tt-noti__body">
            <span class="tt-noti__title">
                {{ notification.title }}
                <span v-if="!notification.isRead" class="tt-noti__dot" aria-label="안 읽음"></span>
            </span>
            <span class="tt-noti__content">{{ notification.content }}</span>
            <span class="tt-noti__meta">
                <span class="tt-noti__time">{{ timeLabel }}</span>
                <span v-if="linkable" class="tt-noti__action">자세히 ›</span>
            </span>
        </span>
    </button>
</template>

<style scoped>
.tt-noti {
    display: flex;
    gap: var(--tt-space-3);
    width: 100%;
    padding: var(--tt-space-4);
    text-align: left;
    border: 0;
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    cursor: pointer;
}

.tt-noti--unread {
    background: var(--tt-bg-subtle);
    border-left: 3px solid var(--tt-primary);
}

.tt-noti__icon {
    flex-shrink: 0;
    width: 44px;
    height: 44px;
    border-radius: var(--tt-radius-md);
    background: var(--tt-surface-strong);
}

.tt-noti--dark .tt-noti__icon {
    background: var(--tt-text);
}

.tt-noti--accent .tt-noti__icon {
    background: var(--tt-accent);
}

.tt-noti__body {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-1);
    min-width: 0;
}

.tt-noti__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.tt-noti__dot {
    display: inline-block;
    width: 6px;
    height: 6px;
    margin-left: var(--tt-space-1);
    background: var(--tt-primary);
    border-radius: var(--tt-radius-full);
}

.tt-noti__content {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.tt-noti__meta {
    display: flex;
    justify-content: space-between;
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.tt-noti__action {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
}
</style>
