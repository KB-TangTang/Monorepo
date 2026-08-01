<!--
  용도: 판결문 카드. JUDGMENT 헤더 · 선고문 본문 · 판정 결과(인장)를 문서처럼 배치한다.
  언제 쓰는지: 재판이 끝난 뒤의 결과 화면, 월간 판결문(리포트).
  쓰면 안 되는 경우: 아직 진행 중인 사건(RecordCard). 판결문은 확정된 결과에만 쓴다.
-->
<script setup>
import BaseCard from '@/components/common/BaseCard.vue';
import StampSeal from '@/components/common/cards/StampSeal.vue';

defineProps({
    caseNo: { type: String, default: '' },
    title: { type: String, default: '' },
    date: { type: String, default: '' },
    /* null 이면 인장을 그리지 않는다. #seal 슬롯으로 직접 넣어도 된다 */
    verdict: {
        type: String,
        default: null,
        validator: (v) => v === null || ['guilty', 'innocent', 'confirmed'].includes(v),
    },
});
</script>

<template>
    <BaseCard variant="judgment" padding="lg">
        <template #header>
            <div class="judgment__head">
                <p class="judgment__kicker">JUDGMENT</p>
                <p v-if="caseNo" class="judgment__no">{{ caseNo }}</p>
                <h3 v-if="title" class="judgment__title">{{ title }}</h3>
            </div>
        </template>

        <div class="judgment__body">
            <slot />
        </div>

        <template #footer>
            <div class="judgment__foot">
                <p v-if="date" class="judgment__date">{{ date }}</p>
                <div class="judgment__seal">
                    <slot name="seal">
                        <StampSeal v-if="verdict" :verdict="verdict" size="sm" />
                    </slot>
                </div>
            </div>
        </template>
    </BaseCard>
</template>

<style scoped>
.judgment__head {
    text-align: center;
}

.judgment__kicker {
    padding-bottom: var(--tt-space-2);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.42em;
    text-indent: 0.42em; /* letter-spacing 이 만든 오른쪽 여백 보정 */
    color: var(--tt-text);
    /* 문서 느낌의 이중선 — 이미지 없이 gradient 로만 */
    border-bottom: 3px double var(--tt-gray-900);
}

.judgment__no {
    margin-top: var(--tt-space-3);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.judgment__title {
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-snug);
}

.judgment__body {
    font-size: var(--tt-fs-body);
    line-height: 1.9;
    color: var(--tt-text);
    text-align: justify;
    /* 원고지 괘선 — 본문 줄에 옅은 밑줄을 깐다 */
    background-image: repeating-linear-gradient(
        to bottom,
        transparent 0,
        transparent calc(1.9em - 1px),
        var(--tt-border) calc(1.9em - 1px),
        var(--tt-border) 1.9em
    );
}

.judgment__foot {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    min-height: 64px;
}

.judgment__date {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.judgment__seal {
    margin-left: auto;
}
</style>
