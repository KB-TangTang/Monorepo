<!--
  목데이터 ↔ 실 API 전환 플로팅 버튼 (개발 전용).

  프로덕션 빌드에서는 아예 렌더링되지 않는다 (devDataSource 가 DEV 를 검사한다).
  같은 화면에 다른 DEV 버튼이 이미 있으면 `offset` 으로 한 줄 위에 띄운다.
-->
<script setup>
import { CircleStackIcon } from '@heroicons/vue/24/outline';
import {
    dataSource,
    isMockMode,
    isDevToggleAvailable,
    toggleDataSource,
} from '@/services/devDataSource';

defineProps({
    /** 탭바 위쪽으로 띄울 거리(px). 다른 DEV 버튼과 겹칠 때 올린다. */
    offset: { type: Number, default: 16 },
});
</script>

<template>
    <button
        v-if="isDevToggleAvailable"
        type="button"
        class="dds-fab"
        :class="isMockMode ? 'dds-fab--mock' : 'dds-fab--live'"
        :style="{ bottom: `calc(var(--tt-tabbar-height) + ${offset}px)` }"
        @click="toggleDataSource"
    >
        <CircleStackIcon class="dds-fab__icon" />
        {{ dataSource === 'mock' ? '목데이터' : '실데이터' }}
    </button>
</template>

<style scoped>
.dds-fab {
    position: fixed;
    right: 16px;
    height: 32px;
    padding: 0 12px;
    border-radius: var(--tt-radius-full);
    background: rgba(35, 40, 66, 0.9);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    font-family: inherit;
    cursor: pointer;
    z-index: var(--tt-z-tabbar);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    display: flex;
    align-items: center;
    gap: 4px;
}

.dds-fab--mock {
    border: 1.5px solid rgba(245, 185, 33, 0.5);
    color: var(--tt-gold);
}

.dds-fab--live {
    border: 1.5px solid rgba(62, 155, 126, 0.6);
    color: var(--tt-success);
}

.dds-fab__icon {
    width: 14px;
    height: 14px;
}
</style>
