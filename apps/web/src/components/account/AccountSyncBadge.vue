<!--
  용도: 계좌 동기화 상태 5종을 표시한다 (AC_02_04).
  언제 쓰는지: 연결 계좌 관리 목록의 각 행.
  쓰면 안 되는 경우: 상태 외의 라벨. 그건 BaseBadge 를 직접 쓴다.

  Figma 확정본(연결 계좌 관리)은 **정상일 때만 초록 텍스트, 주의가 필요한 상태만 pill** 로 그린다.
  다 배지로 그리면 목록이 배지밭이 되어 정작 문제 있는 계좌가 묻힌다.

  상태 값은 tbl_connected_account.sync_status 와 같다
  (NORMAL / SYNCING / NEED_SYNC / FAILED / NEED_RECONNECT).
-->
<script setup>
import { computed } from 'vue';
import BaseBadge from '@/components/common/BaseBadge.vue';
import { SYNC_STATUS, resolveSyncBadge } from '@/utils/account';

const props = defineProps({
    syncStatus: { type: String, required: true },
});

const badge = computed(() => resolveSyncBadge(props.syncStatus));
const quiet = computed(() => props.syncStatus === SYNC_STATUS.NORMAL);
</script>

<template>
    <span v-if="quiet" class="sync-badge__quiet">{{ badge.label }}</span>
    <BaseBadge v-else :variant="badge.variant">{{ badge.label }}</BaseBadge>
</template>

<style scoped>
.sync-badge__quiet {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-success);
}
</style>
