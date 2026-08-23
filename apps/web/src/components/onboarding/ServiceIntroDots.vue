<script setup>
/*
 * 서비스 소개 온보딩의 페이지 인디케이터 (이슈 #459).
 *
 * 점을 눌러도 이동하지 않는다 — 5면짜리 소개라 건너뛰기가 이미 있고,
 * 6.5px 짜리 점은 터치 목표로 너무 작아 오작동만 만든다. 그래서 button 이 아니라 span 이다.
 */
defineProps({
    /** 현재 면 번호 (0-based) */
    current: {
        type: Number,
        required: true,
    },
    /** 전체 면 개수 */
    total: {
        type: Number,
        required: true,
    },
});
</script>

<template>
    <!--
      스크린리더에는 점 5개가 아니라 「5면 중 2면」 한 문장으로 읽힌다.
      점 자체는 aria-hidden 이라 의미 없는 나열이 새어 나가지 않는다.
    -->
    <div class="intro-dots" role="status" :aria-label="`${total}면 중 ${current + 1}면`">
        <span
            v-for="index in total"
            :key="index"
            class="intro-dots__dot"
            :class="{ 'intro-dots__dot--on': index - 1 === current }"
            aria-hidden="true"
        ></span>
    </div>
</template>

<style scoped>
.intro-dots {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 6px;
}

.intro-dots__dot {
    width: 6.5px;
    height: 6.5px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-onb-dot-idle);
    transition: background-color 0.25s ease;
}

.intro-dots__dot--on {
    background: var(--tt-blue);
}

@media (prefers-reduced-motion: reduce) {
    .intro-dots__dot {
        transition: none;
    }
}
</style>
