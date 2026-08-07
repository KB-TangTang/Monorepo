<!--
  용도: 알림 목록의 한 줄. 아이콘 · 제목 · 본문 · 시간 · 액션 링크를 그린다.
  언제 쓰는지: NotificationListView 안에서만.
  쓰면 안 되는 경우: 마스코트 일러스트를 이 자리에 넣지 말 것 — 전신 그림이라 44px 로 줄이면
    얼굴이 뭉개지고 흰 몸통이 흰 카드에 묻힌다. 마스코트는 모달·오버레이의 큰 자리에서 쓴다.
-->
<script setup>
import { computed } from 'vue';
import { formatRelativeTime, notificationVisual, resolveDeepLink } from '@/utils/notification';

/*
 * 알림 종류별 글리프. notificationVisual(type).icon 이 여기 키와 짝을 이룬다.
 * 24 그리드 stroke 아이콘으로 통일했다 — 이 프로젝트의 다른 상태 아이콘(StateError 등)과 같은 방식이다.
 */
const ICON_PATHS = {
    gavel: ['M12.5 2.5l6 6-3 3-6-6z', 'M10.5 8.5 3 16', 'M2 21h10'],
    clock: ['M12 3a9 9 0 1 0 0 18 9 9 0 0 0 0-18z', 'M12 7.5V12l3 1.8'],
    book: [
        'M3 4h5a3 3 0 0 1 3 3v13a2.5 2.5 0 0 0-2.5-2.5H3z',
        'M21 4h-5a3 3 0 0 0-3 3v13a2.5 2.5 0 0 1 2.5-2.5H21z',
    ],
    calendar: ['M4 5.5h16v15H4z', 'M8 3v5', 'M16 3v5', 'M4 11h16'],
    refresh: ['M20 12a8 8 0 1 1-2.34-5.66', 'M20 3.5V9h-5.5'],
    bell: ['M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8', 'M13.7 21a2 2 0 0 1-3.4 0'],
};

const props = defineProps({
    notification: { type: Object, required: true },
    now: { type: Date, required: true },
});

const emit = defineEmits(['open']);

const visual = computed(() => notificationVisual(props.notification.type));
const iconPaths = computed(() => ICON_PATHS[visual.value.icon] ?? ICON_PATHS.bell);
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
        <span class="tt-noti__icon" aria-hidden="true">
            <svg
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
            >
                <path v-for="(d, index) in iconPaths" :key="index" :d="d" />
            </svg>
        </span>
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
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 44px;
    height: 44px;
    border-radius: var(--tt-radius-md);
    background: var(--tt-surface-strong);
    color: var(--tt-text-muted);
}

.tt-noti__icon svg {
    width: 22px;
    height: 22px;
}

/* 재판 계열은 어두운 타일에 강조색 글리프. 참고화면의 판사봉 처리와 같다 */
.tt-noti--dark .tt-noti__icon {
    background: var(--tt-text);
    color: var(--tt-accent);
}

/* 마감 임박처럼 시간이 걸린 알림은 강조 타일에 어두운 글리프 */
.tt-noti--accent .tt-noti__icon {
    background: var(--tt-accent);
    color: var(--tt-text);
}

/*
 * flex: 1 이 없으면 본문이 내용 폭만큼만 차지해서, 시간 문자열 길이에 따라
 * `자세히 ›` 의 오른쪽 끝이 줄마다 달라진다 (2026-08-06 실제 발생).
 */
.tt-noti__body {
    flex: 1;
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
