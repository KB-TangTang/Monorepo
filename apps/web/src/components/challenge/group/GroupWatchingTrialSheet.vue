<!--
  지켜보는 재판 — 지방법원 홈 (이슈 #448)

  홈의 「재판 현황」이 **내 차례인 재판만** 남기면서(할 일 큐), 지금 내가 할 게 없는 재판
  — 변론을 이미 냈거나, 남이 변론을 쓰는 중이거나, 투표를 이미 던진 것 — 이 갈 곳이 없어졌다.
  없앨 수는 없다. 「내 재판이 심판받는 중」은 할 일이 아니지만 **가장 궁금한 것**이다.

  그래서 홈에는 한 줄만 남기고 목록은 이 시트가 받는다.

  줄 모양은 `GroupTrialStatusCard` 를 그대로 쓴다. 같은 재판을 두 자리에서 다르게 그리면
  뱃지·제목·CTA 판정이 갈린다 — 그 판정은 `utils/groupTrial.js` 한 곳에만 있어야 한다.
  지켜보는 입장은 CTA 가 전부 「재판 현황 보기」(`action: 'trial'`)라 이 자리에서도 말이 맞다.

  **기존 `GroupTodoSheet` 를 되살리지 않은 이유**: 그 시트는 처리할 일 전용이다.
  머리글이 「처리할 일 N건」이고 필터 칩이 「기소 / 투표」, 행마다 「변론」·「투표」 버튼이 붙는다.
  지켜보는 재판에 그 버튼을 달면 눌러서 아무것도 못 하는 화면으로 보낸다.
-->
<script setup>
import { ref } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import GroupTrialStatusCard from './GroupTrialStatusCard.vue';

defineProps({
    modelValue: { type: Boolean, required: true },
    /** `toTrialStatusCard` 를 지난, `actionable === false` 인 카드 배열 */
    items: { type: Array, required: true },
    /** 아이템 id → { text, urgent } 맵 */
    countdowns: { type: Object, required: true },
});

const emit = defineEmits(['update:modelValue', 'open']);

/*
 * 시트 안에서 화면을 이동할 때는 시트가 쌓아 둔 history 항목을 먼저 양도해야 한다.
 * 그러지 않으면 시트가 닫히면서 history.back() 이 라우터 이동을 되감는다
 * (`common/useOverlay.js` 주석 · GroupTodoSheet 가 같은 이유로 같은 것을 노출한다).
 */
const sheetRef = ref(null);
defineExpose({
    releaseHistory: () => sheetRef.value?.releaseHistory?.(),
});
</script>

<template>
    <BaseBottomSheet
        ref="sheetRef"
        :model-value="modelValue"
        height="60vh"
        @update:model-value="emit('update:modelValue', $event)"
    >
        <template #header>
            <div class="watch-sheet__head">
                <span class="watch-sheet__title">지켜보는 재판 {{ items.length }}건</span>
                <span class="watch-sheet__sort">마감 임박순</span>
            </div>
        </template>

        <div class="watch-sheet__list">
            <GroupTrialStatusCard
                :items="items"
                :countdowns="countdowns"
                @open="emit('open', $event)"
            />
        </div>
    </BaseBottomSheet>
</template>

<style scoped>
.watch-sheet__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.watch-sheet__title {
    /* 팀 바텀시트 제목은 전부 section 이다 (BaseBottomSheet · GroupTodoSheet · LedgerCategorySheet) */
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.watch-sheet__sort {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.watch-sheet__list {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
}

/*
 * 시트 안에서는 카드가 카드일 이유가 없다 — 시트 자체가 이미 흰 판이다.
 * 홈에서 이 카드를 바닥에서 떼어 놓은 그림자·라운드·좌우 패딩을 여기서만 걷는다.
 */
.watch-sheet__list :deep(.trial-status) {
    box-shadow: none;
    border-radius: 0;
    padding-left: 0;
    padding-right: 0;
}
</style>
