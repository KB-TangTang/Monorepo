<!--
  용도: 홈 탭의 임시 진입 화면. 백엔드 연결 상태만 확인한다.
  언제 쓰는지: 홈 담당자가 실제 홈 화면을 붙이기 전까지의 자리.
  쓰면 안 되는 경우: 공용 로직을 여기에 두는 것. 공용은 components/common/ 으로 올린다.
-->
<script setup>
import { onMounted, ref } from 'vue';
import { fetchHealth } from '@/api/health';
import BaseCard from '@/components/common/BaseCard.vue';
import BaseBadge from '@/components/common/BaseBadge.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import StateError from '@/components/common/StateError.vue';

const health = ref(null);
const loading = ref(false);
const errorMessage = ref('');

async function load() {
    loading.value = true;
    errorMessage.value = '';
    try {
        health.value = await fetchHealth();
    } catch (err) {
        errorMessage.value = err.message ?? '서버에 연결하지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

onMounted(load);
</script>

<template>
    <div class="home">
        <h1 class="home__title">탕탕 · 지갑재판소</h1>

        <StateLoading v-if="loading" message="서버 상태를 확인하는 중" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="load" />
        <BaseCard v-else variant="record">
            <template #header>백엔드 연결</template>
            <p class="home__row">
                <span>상태</span>
                <BaseBadge variant="innocent">{{ health?.status ?? '-' }}</BaseBadge>
            </p>
            <p class="home__row">
                <span>서비스</span>
                <span class="home__mono">{{ health?.service ?? '-' }}</span>
            </p>
        </BaseCard>
    </div>
</template>

<style scoped>
.home {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    padding: var(--tt-space-5);
}

.home__title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.home__row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    padding: var(--tt-space-2) 0;
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}

.home__mono {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text);
}
</style>
