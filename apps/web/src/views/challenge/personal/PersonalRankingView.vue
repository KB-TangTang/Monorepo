<script setup>
import StateEmpty from '@/components/common/StateEmpty.vue';
</script>

<template>
    <main class="personal-ranking">
        <StateEmpty
            title="개인 미션 랭킹 준비 중"
            description="월간 명예법정 순위 화면은 다음 작업에서 연결할 예정이에요."
        />
    </main>
</template>

<style scoped>
.personal-ranking {
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-6) var(--tt-space-5);
    font-family: var(--tt-font-sans);
    color: var(--tt-text);
    background: var(--tt-bg-subtle);
}
</style>
