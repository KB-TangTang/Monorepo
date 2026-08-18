<!--
  배심원 한줄 코멘트 목록 (이슈 #172).

  ⚠ **익명이다. 작성자를 표시하지 않는다.**
  서버가 `VoteMapper.xml#findCommentsByIndictmentId` 에서 `tbl_user` 조인 자체를 하지 않아
  `user_id` 가 응답에 없다(이슈 #171 결정). 같은 그룹 안에서 「누가 유죄를 던졌는지」가
  드러나면 다음 재판의 투표가 눈치싸움이 된다. 순번(`배심원 1`)도 붙이지 않는다 —
  목록이 `created_at` 순이라 투표 순서를 아는 사람에게는 이름표와 다를 게 없다.

  `comments` 가 `null` 이면 **개표 전**이라는 뜻이라 아무것도 그리지 않는다.
  `[]` 는 「개표됐는데 아무도 코멘트를 안 남겼다」로 서로 다른 상태다.
-->
<script setup>
import { computed } from 'vue';

const props = defineProps({
    comments: {
        type: Array,
        default: null,
    },
});

const isTallied = computed(() => Array.isArray(props.comments));
const items = computed(() => props.comments ?? []);
</script>

<template>
    <section v-if="isTallied" class="trial-comments">
        <header class="trial-comments__head">
            <span class="trial-comments__label">배심원 한줄 코멘트</span>
            <span class="trial-comments__count">{{ items.length }}</span>
        </header>

        <ul v-if="items.length" class="trial-comments__list">
            <li v-for="(item, idx) in items" :key="idx" class="trial-comments__item">
                <span class="trial-comments__quote">“</span>
                <p class="trial-comments__text">{{ item.comment }}</p>
            </li>
        </ul>
        <p v-else class="trial-comments__empty">남긴 코멘트가 없어요.</p>

        <p class="trial-comments__hint">누가 남겼는지는 공개되지 않아요.</p>
    </section>
</template>

<style scoped>
.trial-comments {
    flex: none;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    padding: 14px var(--tt-card-padding);
    box-shadow: var(--tt-elevation-2);
}

.trial-comments__head {
    display: flex;
    align-items: center;
    gap: 6px;
}

.trial-comments__label {
    font-size: var(--tt-fs-badge);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.trial-comments__count {
    min-width: 18px;
    padding: 1px 6px;
    background: var(--tt-info-subtle);
    color: var(--tt-info);
    border-radius: var(--tt-radius-full);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    text-align: center;
}

.trial-comments__list {
    margin-top: 10px;
    display: flex;
    flex-direction: column;
    gap: 6px;
    list-style: none;
    padding: 0;
}

.trial-comments__item {
    display: flex;
    align-items: flex-start;
    gap: 6px;
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-md);
    padding: 10px 12px;
}

.trial-comments__quote {
    flex: none;
    color: var(--tt-text-hint);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    line-height: 1;
}

.trial-comments__text {
    /* 40자 제한이라 길어야 두 줄이다. 줄바꿈만 허용하고 자르지 않는다. */
    font-size: 12.5px;
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-body);
    word-break: break-word;
}

.trial-comments__empty {
    margin-top: 10px;
    font-size: 12.5px;
    color: var(--tt-text-muted);
    font-weight: var(--tt-fw-medium);
}

.trial-comments__hint {
    margin-top: 8px;
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
}
</style>
