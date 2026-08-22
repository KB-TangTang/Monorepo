<!--
  재판 목록 시트 — 지방법원 홈 (이슈 #448)

  홈이 재판을 **세 묶음**으로 접었다 — 변론할 것 / 투표할 것(할 일 격자 두 칸) ·
  기다리는 것(격자 아래 한 줄). 어느 쪽을 눌러도 목록은 이 시트 하나가 받는다.

  묶음마다 시트를 따로 만들지 않는다. 세 목록의 차이는 **제목과 담긴 항목뿐**이고
  줄 모양·정렬·CTA 는 전부 같다. 파일을 셋으로 나누면 그 셋이 조금씩 어긋난다.

  줄 모양은 `GroupTrialStatusCard` 를 그대로 쓴다. 같은 재판을 두 자리에서 다르게 그리면
  제목·아이콘·CTA 판정이 갈린다 — 그 판정은 `utils/groupTrial.js` 한 곳에만 있어야 한다.
  기다리는 입장은 CTA 가 전부 「재판 현황 보기」(`action: 'trial'`)라 이 자리에서도 말이 맞다.

  **머리줄은 시트가 그린다.** 그래서 카드에는 `heading` 을 넘기지 않는다. 넘기면
  「변론할 재판 2건」이 시트 헤더와 카드 머리줄에 두 번 뜬다.
-->
<script setup>
import { ref } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import GroupTrialStatusCard from './GroupTrialStatusCard.vue';

defineProps({
    modelValue: { type: Boolean, required: true },
    /** 시트 헤더 앞말. 「변론할 재판」 · 「투표할 재판」 · 「기다리는 재판」 */
    title: { type: String, required: true },
    /** `toTrialStatusCard` 를 지난 카드 배열 */
    items: { type: Array, required: true },
    /** 아이템 id → { text, urgent } 맵 */
    countdowns: { type: Object, required: true },
});

const emit = defineEmits(['update:modelValue', 'open']);

/*
 * 시트 안에서 화면을 이동할 때는 시트가 쌓아 둔 history 항목을 먼저 양도해야 한다.
 * 그러지 않으면 시트가 닫히면서 history.back() 이 라우터 이동을 되감는다
 * (`common/useOverlay.js` 주석 참고).
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
            <div class="trial-sheet__head">
                <span class="trial-sheet__title">{{ title }} {{ items.length }}건</span>
                <span class="trial-sheet__sort">마감 임박순</span>
            </div>
        </template>

        <div class="trial-sheet__list">
            <GroupTrialStatusCard
                :items="items"
                :countdowns="countdowns"
                @open="emit('open', $event)"
            />
        </div>
    </BaseBottomSheet>
</template>

<style scoped>
.trial-sheet__head {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.trial-sheet__title {
    /* 팀 바텀시트 제목은 전부 section 이다 (BaseBottomSheet · LedgerCategorySheet) */
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.trial-sheet__sort {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.trial-sheet__list {
    flex: 1;
    min-height: 0;
    overflow-y: auto;
}

/*
 * 시트 안에서는 카드가 카드일 이유가 없다 — 시트 자체가 이미 흰 판이다.
 * 홈에서 이 카드를 바닥에서 떼어 놓은 그림자·라운드·좌우 패딩을 여기서만 걷는다.
 */
.trial-sheet__list :deep(.trial-status) {
    box-shadow: none;
    border-radius: 0;
    padding-left: 0;
    padding-right: 0;
}
</style>
