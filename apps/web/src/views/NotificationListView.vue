<!--
  용도: 알림 목록 (NT_01_03). 오늘·어제·날짜로 묶어 최신순으로 보여준다.
  언제 쓰는지: 홈 헤더의 종을 눌렀을 때. router 의 /notifications.
  쓰면 안 되는 경우: 탭바를 숨기지 말 것 — 참고화면에 뒤로가기가 없어 사용자가 갇힌다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import NotificationRow from '@/components/notification/NotificationRow.vue';
import BaseBackButton from '@/components/common/BaseBackButton.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import { useNotificationStore } from '@/stores/notification';
import { groupByDay, resolveDeepLink } from '@/utils/notification';

const router = useRouter();
const store = useNotificationStore();
const { items, unreadCount, loading, error, actionError, hasMore } = storeToRefs(store);

const now = ref(new Date());
const groups = computed(() => groupByDay(items.value, now.value));

onMounted(() => {
    now.value = new Date();
    store.load();
});

async function open(notification) {
    await store.markRead(notification.id);
    const path = resolveDeepLink(notification.deepLinkUrl);
    if (path) {
        router.push(path);
    }
}
</script>

<template>
    <div class="noti-list">
        <header class="noti-list__header">
            <BaseBackButton label="이전 화면으로 돌아가기" />
            <h1 class="noti-list__title">
                알림
                <span v-if="unreadCount > 0" class="noti-list__count">{{ unreadCount }}</span>
            </h1>
            <button
                v-if="unreadCount > 0"
                type="button"
                class="noti-list__read-all"
                @click="store.markAllRead()"
            >
                모두 읽음
            </button>
        </header>

        <!--
          읽음 처리 같은 액션 실패는 여기 인라인으로만 알린다.
          목록을 StateError 로 덮으면 실패 한 번에 알림이 전부 사라진다.
        -->
        <p v-if="actionError" class="noti-list__action-error" role="alert">{{ actionError }}</p>

        <StateLoading v-if="loading && items.length === 0" message="알림을 불러오는 중" />
        <StateError v-else-if="error" :message="error" @retry="store.load()" />
        <StateEmpty
            v-else-if="items.length === 0"
            title="아직 알림이 없어요"
            description="계좌 연동·미션·판결 소식이 여기로 와요."
        />
        <template v-else>
            <section v-for="group in groups" :key="group.label" class="noti-list__group">
                <p class="noti-list__label">{{ group.label }}</p>
                <NotificationRow
                    v-for="item in group.items"
                    :key="item.id"
                    :notification="item"
                    :now="now"
                    @open="open"
                />
            </section>
            <button v-if="hasMore" type="button" class="noti-list__more" @click="store.loadMore()">
                더 보기
            </button>
        </template>
    </div>
</template>

<style scoped>
.noti-list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.noti-list__header {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

/* 제목이 남은 폭을 먹어야 '모두 읽음' 이 오른쪽 끝에 붙는다.
   space-between 으로 두면 뒤로가기·제목·모두읽음 이 3등분돼 제목이 가운데로 떠버린다 */
.noti-list__title {
    flex: 1;
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.noti-list__count {
    min-width: 22px;
    padding: 0 var(--tt-space-1);
    font-size: var(--tt-fs-caption);
    line-height: 22px;
    text-align: center;
    color: var(--tt-white);
    background: var(--tt-danger);
    border-radius: var(--tt-radius-full);
}

.noti-list__read-all {
    border: 0;
    background: none;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
    cursor: pointer;
}

.noti-list__action-error {
    padding: var(--tt-space-2) var(--tt-space-3);
    border: 1px solid var(--tt-danger);
    border-radius: var(--tt-radius-md);
    background: var(--tt-bg);
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

.noti-list__group {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.noti-list__label {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.noti-list__more {
    padding: var(--tt-space-3);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    font-family: var(--tt-font-sans);
    color: var(--tt-text-muted);
    cursor: pointer;
}
</style>
