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
    /* 반쪽 폭 자리(이번 달 누적 카드 옆)에 설 때. 탕이를 위로 올리고 세로로 쌓는다. */
    compact: { type: Boolean, default: false },
});

defineEmits(['open']);
</script>

<template>
    <button
        type="button"
        class="honor-banner"
        :class="{ 'honor-banner--compact': compact }"
        @click="$emit('open')"
    >
        <img class="honor-banner__image" :src="honorCourtImage" alt="" />

        <span class="honor-banner__content">
            <strong class="honor-banner__title">{{ title }}</strong>
            <!-- 반쪽 폭에서는 설명을 뺀다. 「명예의 전당」이라는 제목과 트로피 탕이만으로
                 어디로 가는 입구인지 이미 읽히고, 두 줄을 더 쌓으면 탕이가 밀려 작아진다. -->
            <span v-if="!compact" class="honor-banner__description">{{ description }}</span>
        </span>
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

/* ── 반쪽 폭 변형 ──────────────────────
 * 가로로 눕힌 배너를 그대로 좁히면 탕이가 글을 밀어내 제목이 두 줄로 접힌다.
 * 탕이를 글 위로 올리고 가운데 정렬해 세로로 쌓는다.
 */
.honor-banner--compact {
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 6px;
    height: 100%;
    min-height: 0;
    padding: 10px 14px;
    text-align: center;
}

.honor-banner--compact .honor-banner__image {
    position: static;
    width: 76px;
    height: 76px;
}

.honor-banner--compact .honor-banner__content {
    gap: 2px;
    /* 탕이가 더는 오른쪽 아래에 얹혀 있지 않으므로 비워둘 자리가 없다 */
    padding-right: 0;
    align-items: center;
}

.honor-banner--compact .honor-banner__title {
    font-size: var(--tt-fs-body);
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
