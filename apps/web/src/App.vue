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
            notification.clearSession();
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
    /*
     * 100vh 는 주소창이 보이는 모바일 브라우저에서 실제 화면보다 커서, 내용이 한 화면에
     * 들어가도 늘 주소창 높이만큼 스크롤이 생긴다(안드·iOS 공통). dvh 로 실제 높이를 쓴다.
     * 아래 줄을 모르는 브라우저는 위의 100vh 를 그대로 쓴다.
     */
    min-height: 100vh;
    min-height: 100dvh;
    margin: 0 auto;
    background: var(--tt-bg);
}

/* 하단 탭바에 가리지 않도록 콘텐츠 아래 여백을 확보한다 */
.tt-app__content {
    flex: 1;
    /* 값은 tokens.css 의 --tt-app-bottom-inset. 페이지가 이 여백을 되짚어야 할 때가 있어 토큰으로 뺐다 */
    padding-bottom: var(--tt-app-bottom-inset);
}

/* 탭바가 없는 화면(로그인 등)은 여백도 필요 없다 */
.tt-app__content--bare {
    padding-bottom: 0;
}
</style>
