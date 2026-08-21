<!--
  용도: 개인 미션 홈에서 월간 명예의 전당(성적표)으로 이동시키는 배너.
  홈 탭의 명예의 전당 카드(HomeView.vue `.honor-court`)와 같은 모양이다 —
  같은 곳으로 가는 입구가 화면마다 다르게 생기면 사용자가 다른 기능으로 읽는다.
-->
<script setup>
import honorCourtImage from '@/assets/images/emotions/56_with_trophy_ver4.png';

defineProps({
    title: { type: String, default: '명예의 전당' },
    description: { type: String, default: '월간 랭킹 순위를 확인해보세요.' },
});

defineEmits(['open']);
</script>

<template>
    <button type="button" class="honor-banner" @click="$emit('open')">
        <span class="honor-banner__content">
            <strong class="honor-banner__title">{{ title }}</strong>
            <span class="honor-banner__description">{{ description }}</span>
        </span>

        <img class="honor-banner__image" :src="honorCourtImage" alt="" />
    </button>
</template>

<style scoped>
.honor-banner {
    position: relative;
    display: flex;
    align-items: center;
    width: 100%;
    min-height: 108px;
    padding: var(--tt-space-4);
    overflow: hidden;
    font-family: var(--tt-font-sans);
    text-align: left;
    cursor: pointer;
    background: var(--tt-info-subtle);
    border: 1px solid var(--tt-info-subtle);
    border-radius: var(--tt-radius-lg);
}

.honor-banner__content {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
    /* 탕이(92px)가 오른쪽 아래에 얹히므로 글이 그 밑으로 들어가지 않게 비워둔다 */
    min-width: 0;
    padding-right: 96px;
}

.honor-banner__title {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-snug);
    color: var(--tt-text);
}

.honor-banner__description {
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-info);
}

.honor-banner__image {
    position: absolute;
    right: var(--tt-space-3);
    bottom: var(--tt-space-3);
    width: 92px;
    height: 92px;
    object-fit: contain;
}

.honor-banner:hover {
    filter: brightness(0.98);
}

.honor-banner:focus-visible {
    outline: 2px solid var(--tt-info);
    outline-offset: 2px;
}

@media (max-width: 359px) {
    .honor-banner__content {
        padding-right: 84px;
    }

    .honor-banner__image {
        width: 80px;
        height: 80px;
    }
}
</style>
