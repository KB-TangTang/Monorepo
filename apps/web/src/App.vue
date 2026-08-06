<script setup>
import { watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import TheTabBar from '@/components/common/TheTabBar.vue';
import { useAuthStore } from '@/stores/auth';
import { useNotificationStore } from '@/stores/notification';

const route = useRoute();

// 로그인 상태에 따라 알림 스트림을 연결/해제한다. 종 배지는 화면 진입과 무관하게
// 앱 단위로 하나만 유지해야 하므로 여기서 관리한다.
const auth = useAuthStore();
const notification = useNotificationStore();
const { isLoggedIn } = storeToRefs(auth);

watch(
    isLoggedIn,
    (loggedIn) => {
        if (loggedIn) {
            notification.refreshBadge();
            notification.connect();
        } else {
            notification.disconnect();
        }
    },
    { immediate: true },
);
</script>

<template>
    <div class="tt-app">
        <main class="tt-app__content" :class="{ 'tt-app__content--bare': route.meta.hideTabBar }">
            <RouterView />
        </main>
        <TheTabBar v-if="!route.meta.hideTabBar" />
    </div>
</template>

<style scoped>
.tt-app {
    display: flex;
    flex-direction: column;
    width: 100%;
    max-width: var(--tt-content-max);
    min-height: 100vh;
    margin: 0 auto;
    background: var(--tt-bg);
}

/* 하단 탭바에 가리지 않도록 콘텐츠 아래 여백을 확보한다 */
.tt-app__content {
    flex: 1;
    padding-bottom: calc(var(--tt-tabbar-height) + env(safe-area-inset-bottom) + var(--tt-space-4));
}

/* 탭바가 없는 화면(로그인 등)은 여백도 필요 없다 */
.tt-app__content--bare {
    padding-bottom: 0;
}
</style>
