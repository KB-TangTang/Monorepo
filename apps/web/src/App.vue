<script setup>
import TheTabBar from '@/components/common/TheTabBar.vue';
</script>

<template>
    <div class="tt-app">
        <main class="tt-app__content">
            <RouterView />
        </main>
        <TheTabBar />
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
</style>
