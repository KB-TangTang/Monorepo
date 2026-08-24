<!--
  그룹 명예 법정 진입 배너 — 그룹 상세 · 재판 기록 화면의 스크롤 맨 아래.

  **왜 개인 「명예의 전당」 배너를 그대로 쓰지 않나.**
  한 번 `PersonalMissionHonorBanner` 를 공용으로 올려 글만 갈아끼워 봤는데, 두 배너가
  똑같이 생겨서 개인 랭킹인지 그룹 순위인지 구별이 안 됐다. 목적지가 다른 입구는
  형태로 달라야 한다 — 여기서 갈리는 건 문구가 아니라 **탕이가 몇 명이냐**다.

  탕이 셋을 시상대와 같은 **2위–1위–3위** 순서로 세운다(`GroupHonorPodium` 의 배치 규칙).
  가운데 1위만 크고 트로피를 들고 있어, 한눈에 「여럿이 겨룬 순위」로 읽힌다.
  개인 배너는 탕이 하나라 그것만으로 두 화면이 갈린다.

  갈색(`--tt-wood`·`--tt-kraft`)은 쓰지 않는다. 그 토큰은 판사봉·인장·종이 질감 전용이고
  순위는 문서가 아니다. 골드 계열이 트로피·시상대와 같은 어휘라 여기 맞는다.
-->
<script setup>
import firstTangi from '@/assets/images/ranking/tang-ranking-first.png';
import secondTangi from '@/assets/images/ranking/tang-ranking-second.png';
import thirdTangi from '@/assets/images/ranking/tang-ranking-third.png';

defineEmits(['open']);
</script>

<template>
    <button type="button" class="honor-court" @click="$emit('open')">
        <span class="honor-court__content">
            <strong class="honor-court__title">그룹 명예 법정</strong>
            <span class="honor-court__description">우리 그룹의 절약 순위를 확인해 보세요</span>
        </span>

        <!-- 시상대와 같은 2위–1위–3위 배치. 순서를 바꾸면 1위가 가장자리로 밀린다 -->
        <span class="honor-court__crew" aria-hidden="true">
            <img class="honor-court__tangi" :src="secondTangi" alt="" />
            <img class="honor-court__tangi honor-court__tangi--first" :src="firstTangi" alt="" />
            <img class="honor-court__tangi" :src="thirdTangi" alt="" />
        </span>
    </button>
</template>

<style scoped>
.honor-court {
    position: relative;
    display: flex;
    align-items: center;
    width: 100%;
    min-height: 116px;
    padding: var(--tt-space-4);
    overflow: hidden;
    font-family: var(--tt-font-sans);
    text-align: left;
    cursor: pointer;
    background: var(--tt-accent-subtle);
    /*
     * <button> 이라 border 를 안 적으면 브라우저 기본 테두리(2px outset)가 그려진다.
     * <div> 카드처럼 선언을 지우면 되는 게 아니라 반드시 값을 적어야 한다.
     */
    border: 1px solid var(--tt-accent-subtle-border);
    border-radius: var(--tt-radius-lg);
}

.honor-court__content {
    position: relative;
    z-index: 1;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
    /* 탕이 셋이 오른쪽 아래에 얹히므로 글이 그 밑으로 들어가지 않게 비워둔다 */
    min-width: 0;
    padding-right: 146px;
}

.honor-court__title {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-snug);
    color: var(--tt-text);
}

.honor-court__description {
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-accent-deep);
}

/*
 * 바닥(`bottom: 0`)에 맞춰 세운다. 가운데를 기준으로 두면 키가 다른 셋이
 * 공중에 뜬 것처럼 어긋나 보인다. `flex-end` 로 발끝을 맞춘다.
 */
.honor-court__crew {
    position: absolute;
    right: 10px;
    bottom: 0;
    display: flex;
    align-items: flex-end;
}

.honor-court__tangi {
    width: 52px;
    height: 52px;
    object-fit: contain;
}

/* 1위만 크고 앞에 선다. 겹쳐야 「따로 선 셋」이 아니라 한 무리로 읽힌다 */
.honor-court__tangi--first {
    z-index: 1;
    width: 64px;
    height: 64px;
    margin: 0 -10px;
}

.honor-court:hover {
    filter: brightness(0.98);
}

.honor-court:focus-visible {
    outline: 2px solid var(--tt-accent-deep);
    outline-offset: 2px;
}

/* 좁은 화면에서는 셋을 더 겹치고 줄여 제목이 두 줄로 접히는 것을 막는다 */
@media (max-width: 359px) {
    .honor-court__content {
        padding-right: 124px;
    }

    .honor-court__tangi {
        width: 44px;
        height: 44px;
    }

    .honor-court__tangi--first {
        width: 56px;
        height: 56px;
        margin: 0 -12px;
    }
}
</style>
