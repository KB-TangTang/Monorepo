<!--
  용도: 안 읽은 알림 개수를 보여주고 알림 목록으로 보내는 종 아이콘.
  언제 쓰는지: 탭 화면 헤더 우측. 현재는 홈에만 붙어 있다.
  쓰면 안 되는 경우: 알림 목록 화면 자신(이미 그 화면이다).
-->
<script setup>
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useNotificationStore } from '@/stores/notification';

const router = useRouter();
const store = useNotificationStore();
const { unreadCount } = storeToRefs(store);

function goToNotifications() {
    router.push({ name: 'notifications' });
}
</script>

<template>
    <button
        type="button"
        class="tt-bell"
        :aria-label="unreadCount > 0 ? `알림 ${unreadCount}건` : '알림'"
        @click="goToNotifications"
    >
        <svg
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="1.75"
            aria-hidden="true"
        >
            <path d="M18 8a6 6 0 1 0-12 0c0 7-3 8-3 8h18s-3-1-3-8" />
            <path d="M13.7 21a2 2 0 0 1-3.4 0" />
        </svg>
        <span v-if="unreadCount > 0" class="tt-bell__badge">{{
            unreadCount > 99 ? '99+' : unreadCount
        }}</span>
    </button>
</template>

<style scoped>
.tt-bell {
    position: relative;
    display: inline-flex;
    padding: var(--tt-space-2);
    border: 0;
    background: none;
    color: var(--tt-text);
    cursor: pointer;
}

.tt-bell svg {
    width: 24px;
    height: 24px;
}

.tt-bell__badge {
    position: absolute;
    top: 0;
    right: 0;
    min-width: 18px;
    padding: 0 var(--tt-space-1);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    line-height: 18px;
    text-align: center;
    color: var(--tt-white);
    background: var(--tt-danger);
    border-radius: var(--tt-radius-full);
}
</style>
