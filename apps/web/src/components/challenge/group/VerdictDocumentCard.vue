<!--
  판결문 문서형 카드 — 이중 괘선 + 무죄/유죄 도장.
  01(무죄) / 02(유죄) 화면에서 공용으로 사용한다.
-->
<script setup>
import { computed } from 'vue';
import verdictImg from '@/assets/images/emotions/49_verdict.png';
import guiltyImg from '@/assets/images/emotions/52_judge_thumbs_down.png';

const props = defineProps({
    /** 'INNOCENT' | 'GUILTY' */
    outcome: { type: String, required: true },
    /** 판결 메시지. 기본값은 outcome 에 따라 자동 생성 */
    message: { type: String, default: '' },
});

const isInnocent = computed(() => props.outcome === 'INNOCENT');

const mascotSrc = computed(() => isInnocent.value ? verdictImg : guiltyImg);
const mascotAlt = computed(() => isInnocent.value ? '탕이 무죄 판결' : '탕이 유죄 선고');

const headingText = computed(() =>
    isInnocent.value ? '무죄로\n판결됐어요' : '유죄로\n판결됐어요',
);

const subText = computed(() =>
    props.message || (isInnocent.value
        ? '변론 내용과 실제 부담 사정이 인정되었습니다.'
        : '소비 기준 위반이 인정되었습니다.'),
);

const stampLabel = computed(() => isInnocent.value ? '무죄' : '유죄');
const stampSub = computed(() => isInnocent.value ? 'NOT GUILTY' : 'GUILTY');
</script>

<template>
    <div
        class="verdict-doc"
        :class="isInnocent ? 'verdict-doc--innocent' : 'verdict-doc--guilty'"
    >
        <!-- 이중 괘선 -->
        <div class="verdict-doc__inner-border"></div>

        <div class="verdict-doc__body">
            <div class="verdict-doc__title">판 결 문</div>
            <div class="verdict-doc__divider"></div>
            <img :src="mascotSrc" :alt="mascotAlt" class="verdict-doc__mascot">
            <h2 class="verdict-doc__heading">{{ headingText }}</h2>
            <p class="verdict-doc__sub">{{ subText }}</p>
        </div>

        <!-- 도장 -->
        <div class="verdict-doc__stamp">
            <span class="verdict-doc__stamp-label">{{ stampLabel }}</span>
            <span class="verdict-doc__stamp-sub">{{ stampSub }}</span>
        </div>
    </div>
</template>

<style scoped>
.verdict-doc {
    position: relative;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-3);
    padding: 18px;
}

.verdict-doc__inner-border {
    position: absolute;
    inset: 8px;
    border: 1.5px solid var(--tt-bg-fill);
    border-radius: 15px;
    pointer-events: none;
}

.verdict-doc__body {
    position: relative;
    text-align: center;
    padding-top: var(--tt-space-2);
}

.verdict-doc__title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.5em;
    text-indent: 0.5em;
    color: var(--tt-text);
}

.verdict-doc__divider {
    width: 34px;
    height: 2px;
    background: var(--tt-accent);
    margin: 11px auto 0;
}

.verdict-doc__mascot {
    width: 84px;
    height: 84px;
    object-fit: contain;
    margin-top: 10px;
    filter: drop-shadow(0 8px 14px rgba(35, 40, 66, 0.14));
}

.verdict-doc__heading {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    line-height: var(--tt-lh-snug);
    margin-top: var(--tt-space-1);
    white-space: pre-line;
}

.verdict-doc__sub {
    font-size: 12.5px;
    color: var(--tt-text-muted);
    margin-top: var(--tt-space-2);
    line-height: 1.55;
    font-weight: var(--tt-fw-medium);
    padding-bottom: var(--tt-space-1);
}

/* ── 도장 ── */
.verdict-doc__stamp {
    position: absolute;
    top: 58px;
    right: 16px;
    transform: rotate(11deg);
    width: 78px;
    height: 78px;
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
}

.verdict-doc--innocent .verdict-doc__stamp {
    background: rgba(228, 244, 236, 0.9);
    border: 2.5px solid var(--tt-success);
    box-shadow: inset 0 0 0 3.5px rgba(46, 158, 107, 0.22);
}

.verdict-doc--guilty .verdict-doc__stamp {
    background: rgba(251, 233, 228, 0.9);
    border: 2.5px solid var(--tt-danger);
    box-shadow: inset 0 0 0 3.5px rgba(224, 102, 75, 0.22);
}

.verdict-doc__stamp-label {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    line-height: 1;
}

.verdict-doc--innocent .verdict-doc__stamp-label {
    color: var(--tt-success-deep);
}

.verdict-doc--guilty .verdict-doc__stamp-label {
    color: var(--tt-danger-deep);
}

.verdict-doc__stamp-sub {
    font-size: 7px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.16em;
    margin-top: 3px;
}

.verdict-doc--innocent .verdict-doc__stamp-sub {
    color: var(--tt-success);
}

.verdict-doc--guilty .verdict-doc__stamp-sub {
    color: var(--tt-danger);
}
</style>
