<!--
  용도: 홈의 탕이 말풍선. 오늘의 개인 미션 한 건을 한 줄로 알려주고 눌러서 챌린지로 보낸다.
  이슈 #450 결정 ① — /api/missions/today 가 하루 한 건만 내려주므로 시안의 「1/3」 페이저는 두지 않는다.
-->
<script setup>
import { computed } from 'vue';
import { getHomeMissionBubbleCopy } from '@/utils/home';
import tangImage from '@/assets/images/tang_home.png';

const props = defineProps({
    /* toHomeMission() 의 결과. 오늘 배정된 미션이 없으면 null. */
    mission: { type: Object, default: null },
});

defineEmits(['open']);

const copy = computed(() => getHomeMissionBubbleCopy(props.mission));
</script>

<template>
    <button type="button" class="mission-bubble" @click="$emit('open')">
        <span class="mission-bubble__text">
            <span class="mission-bubble__lead">{{ copy.lead }}</span>
            <strong class="mission-bubble__highlight">{{ copy.highlight }}</strong>
        </span>

        <img class="mission-bubble__image" :src="tangImage" alt="" />
    </button>
</template>

<style scoped>
.mission-bubble {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    width: 100%;
    padding: var(--tt-space-3) var(--tt-space-4);
    border: 1px solid var(--tt-success-subtle-border);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-success-subtle);
    font-family: inherit;
    text-align: left;
    cursor: pointer;
}

.mission-bubble__text {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-1);
    min-width: 0;
}

.mission-bubble__lead {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-success-deep);
}

.mission-bubble__highlight {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-success-deep);
}

.mission-bubble__image {
    flex: none;
    width: 58px;
    height: 58px;
    object-fit: contain;
}
</style>
