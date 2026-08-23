<!--
  용도: 홈 최상단 「오늘 쓴 돈」 카드. 오늘 지출 · 이번 달 누계 · 어제 대비 증감을 한 덩어리로 보여주고
        거래내역 · 고정지출로 가는 입구를 겸한다.
  데이터는 전부 props 로 받는다 — 이 컴포넌트는 API 를 모른다.
  amounts 가 null 이면 아직 집계 전이라는 뜻이라 금액 자리를 「—」로 둔다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { formatHomeAmount, getHomeSpendingChange } from '@/utils/home';

const props = defineProps({
    todayAmount: { type: Number, default: null },
    monthAmount: { type: Number, default: null },
    /* 어제 대비 증감률(%). 음수면 덜 쓴 것이다. 비교할 어제 데이터가 없으면 null. */
    changeRate: { type: Number, default: null },
});

defineEmits(['open-ledger', 'open-fixed-expense']);

const todayText = computed(() =>
    props.todayAmount === null ? '—' : `${formatHomeAmount(props.todayAmount)}원`,
);
const monthText = computed(() =>
    props.monthAmount === null
        ? '이번 달 집계 중'
        : `이번 달 ${formatHomeAmount(props.monthAmount)}원`,
);
const change = computed(() => getHomeSpendingChange(props.changeRate));
</script>

<template>
    <BaseCard class="spending" padding="none">
        <p class="spending__label">
            <span class="spending__icon" aria-hidden="true">₩</span>
            오늘 쓴 돈
        </p>

        <span class="spending__chip">{{ monthText }}</span>

        <strong class="spending__amount">{{ todayText }}</strong>

        <p v-if="change" class="spending__change" :class="`spending__change--${change.tone}`">
            {{ change.text }}
        </p>

        <div class="spending__links">
            <button type="button" class="spending__link" @click="$emit('open-ledger')">
                거래내역
            </button>
            <button type="button" class="spending__link" @click="$emit('open-fixed-expense')">
                고정지출
            </button>
        </div>
    </BaseCard>
</template>

<style scoped>
.spending {
    position: relative;
    padding: var(--tt-space-4) var(--tt-space-4) 0;
    border-radius: var(--tt-radius-xl);
}

.spending__label {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.spending__icon {
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    width: 16px;
    height: 16px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-info);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
}

.spending__amount {
    display: block;
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-stat);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.02em;
}

/*
  이번 달 누계는 오늘 금액의 「곁다리」다. 원래 같은 줄에 진한 파랑 알약으로 놔서 오늘 금액과
  무게를 다퉜다 — 재판 카드 뱃지와 같은 옅은 파랑 + 살짝 둥근 모서리로 낮추고 우측 상단으로 뺐다.
*/
.spending__chip {
    position: absolute;
    top: var(--tt-space-4);
    right: var(--tt-space-4);
    padding: var(--tt-space-1) var(--tt-space-3);
    border-radius: var(--tt-radius-xs);
    background: var(--tt-info-subtle);
    color: var(--tt-info);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-normal);
}

.spending__change {
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
}

/* 덜 쓴 쪽을 정보색(파랑)으로 강조한다 — 시안 기준. 초록은 「무죄 판결」에만 쓴다. */
.spending__change--down {
    color: var(--tt-info);
}

.spending__change--up {
    color: var(--tt-danger);
}

.spending__change--same {
    color: var(--tt-text-muted);
}

.spending__links {
    display: flex;
    margin-top: var(--tt-space-4);
    border-top: 1px solid var(--tt-border);
}

.spending__link {
    flex: 1;
    padding: var(--tt-space-3) 0;
    border: 0;
    background: none;
    color: var(--tt-text);
    font-family: inherit;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    cursor: pointer;
}

/* 두 입구를 가르는 세로선. 위의 border-top 과 같은 굵기·색으로 맞춘다. */
.spending__link + .spending__link {
    border-left: 1px solid var(--tt-border);
}

.spending__link:active {
    color: var(--tt-primary);
}
</style>
