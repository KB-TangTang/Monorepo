<!--
  용도: 연결 계좌 하나에 대한 액션 목록 (재동기화 · 재연동 · 연결 해제).
  언제 쓰는지: 연결 계좌 관리 목록의 `⋮` 를 눌렀을 때.
  쓰면 안 되는 경우: 파괴적 액션 확인. 해제는 여기서 고르고 DisconnectConfirmSheet 가 확인을 받는다.

  Figma 확정본이 액션을 행에 늘어놓지 않고 더보기로 접었기 때문에 그 목적지로 만든 시트다.
  재연동이 필요한 계좌에는 `재동기화` 대신 `재연동` 을 보여준다 — 인증이 만료된 계좌는
  다시 긁어봐야 실패한다(AC_02_06).
-->
<script setup>
import { computed } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import { formatSyncTime, needsReconnect, showsResyncAction } from '@/utils/account';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    account: { type: Object, default: null },
});

const emit = defineEmits(['update:modelValue', 'resync', 'reconnect', 'disconnect', 'refresh']);

const title = computed(() => props.account?.bankName ?? '연결 계좌');
const subtitle = computed(() =>
    props.account
        ? `${props.account.accountName} · ${formatSyncTime(props.account.lastSyncAt)}`
        : '',
);
const reconnect = computed(() => needsReconnect(props.account?.syncStatus));

/**
 * 액션 선택.
 *
 * ⚠ **여기서 시트를 닫지 않는다.** 공용 useOverlay 는 열릴 때 히스토리 항목을 쌓고
 * 닫힐 때 history.back() 을 부른다. 이 시트를 닫는 즉시 확인 시트를 열면
 * 이 시트의 back() 이 확인 시트가 막 쌓은 항목을 되돌려 확인 시트가 그 자리에서 닫힌다.
 * 닫는 시점은 부모가 정한다 — 겹쳐 띄울지(해제 확인), 닫고 처리할지(재동기화)가 액션마다 다르다.
 */
function pick(event) {
    emit(event, props.account);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        :title="title"
        @update:model-value="emit('update:modelValue', $event)"
    >
        <p class="action-sheet__subtitle">{{ subtitle }}</p>

        <ul class="action-sheet__list">
            <li v-if="reconnect">
                <button class="action-sheet__item" type="button" @click="pick('reconnect')">
                    <span>재연동</span>
                    <span class="action-sheet__hint">인증이 만료돼 다시 연결해야 해요</span>
                </button>
            </li>
            <!--
              대출 표시 행(manageable=false, #467)은 기관 단위로만 다시 긁어오므로 개별 재조회가 없다.
              예전엔 이 항목이 그대로 보여 누르면 400 이었다. 「전체 즉시 조회」와 「연결 해제」는 된다.
            -->
            <li v-else-if="showsResyncAction(account)">
                <button class="action-sheet__item" type="button" @click="pick('resync')">
                    <span>지금 동기화</span>
                    <span class="action-sheet__hint">이 계좌의 거래를 다시 가져와요</span>
                </button>
            </li>
            <li>
                <button class="action-sheet__item" type="button" @click="pick('refresh')">
                    <span>전체 즉시 조회</span>
                    <span class="action-sheet__hint">연결한 모든 기관을 한 번에 갱신해요</span>
                </button>
            </li>
            <li>
                <button
                    class="action-sheet__item action-sheet__item--danger"
                    type="button"
                    @click="pick('disconnect')"
                >
                    <span>연결 해제</span>
                    <span class="action-sheet__hint">이 계좌의 거래 수집을 멈춰요</span>
                </button>
            </li>
        </ul>
    </BaseBottomSheet>
</template>

<style scoped>
.action-sheet__subtitle {
    margin: 0 0 var(--tt-space-4);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.action-sheet__list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
    margin: 0;
    padding: 0;
    list-style: none;
}

.action-sheet__item {
    display: flex;
    flex-direction: column;
    gap: 2px;
    width: 100%;
    padding: var(--tt-space-4);
    border: 0;
    border-radius: var(--tt-radius-md);
    background: var(--tt-bg-subtle);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    text-align: left;
    cursor: pointer;
}

.action-sheet__item--danger {
    color: var(--tt-danger);
}

.action-sheet__hint {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-regular);
    color: var(--tt-text-muted);
}
</style>
