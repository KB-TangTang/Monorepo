<!--
  용도: 목숨 게이지. 남은 목숨 N/M 을 성한 판사봉 ↔ 부러진 판사봉으로 보여준다.
  언제 쓰는지: 챌린지 진행 중 남은 기회를 표시할 때. 다른 카드 안에 넣을 때는 bare 를 켠다.
  쓰면 안 되는 경우: 퍼센트 진행률(그건 게이지가 아니라 프로그레스 바다). 목숨은 셀 수 있는 개수여야 한다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import gavelAlive from '@/assets/images/challenge_live/gavel-alive.png';
import gavelDepleted from '@/assets/images/challenge_live/gavel-depleted.png';

const props = defineProps({
    remaining: { type: Number, required: true },
    total: { type: Number, default: 3 },
    label: { type: String, default: '남은 목숨' },
    /* 다른 카드 안에 넣을 때 true — 카드 셸 없이 내용만 그린다(카드 중첩 방지) */
    bare: { type: Boolean, default: false },
});

const lives = computed(() => Array.from({ length: props.total }, (_, i) => i < props.remaining));
const isDanger = computed(() => props.remaining <= 1);
</script>

<template>
    <component :is="bare ? 'div' : BaseCard" :padding="bare ? undefined : 'md'">
        <div class="life">
            <div class="life__head">
                <span class="life__label">{{ label }}</span>
                <span class="life__count" :class="{ 'life__count--danger': isDanger }">
                    {{ remaining }}<span class="life__total">/{{ total }}</span>
                </span>
            </div>

            <ul class="life__gavels" :aria-label="`${total}개 중 ${remaining}개 남음`">
                <li v-for="(lit, i) in lives" :key="i" class="life__gavel">
                    <img
                        :src="lit ? gavelAlive : gavelDepleted"
                        alt=""
                        aria-hidden="true"
                        width="126"
                        height="132"
                        decoding="async"
                    />
                </li>
            </ul>
        </div>
    </component>
</template>

<style scoped>
.life {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.life__head {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    gap: var(--tt-space-2);
}

.life__label {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.life__count {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.life__count--danger {
    color: var(--tt-danger);
}

.life__total {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-regular);
    color: var(--tt-text-muted);
}

.life__gavels {
    display: flex;
    gap: var(--tt-space-3);
}

/* 판사봉 일러스트는 그 자체가 오브젝트다 — 칩 박스로 가두지 않는다.
 * 점등/소등은 CSS 가 아니라 이미지가 구분한다(성한 판사봉 ↔ 부러진 판사봉). */
.life__gavel {
    display: flex;
}

.life__gavel img {
    display: block;
    width: auto;
    height: 44px;
}
</style>
