<!--
  용도: 목숨 게이지. 남은 목숨 N/M 을 판사봉 점등·소등으로 보여준다.
  언제 쓰는지: 챌린지 진행 중 남은 기회를 표시할 때. 다른 카드 안에 넣을 때는 bare 를 켠다.
  쓰면 안 되는 경우: 퍼센트 진행률(그건 게이지가 아니라 프로그레스 바다). 목숨은 셀 수 있는 개수여야 한다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';

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
                <li
                    v-for="(lit, i) in lives"
                    :key="i"
                    class="life__gavel"
                    :class="{ 'life__gavel--lit': lit }"
                >
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        stroke-width="1.75"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        aria-hidden="true"
                    >
                        <path
                            d="M13.2 2.6a1.5 1.5 0 0 1 2.1 0l6.1 6.1a1.5 1.5 0 0 1 0 2.1l-1.4 1.4a1.5 1.5 0 0 1-2.1 0l-6.1-6.1a1.5 1.5 0 0 1 0-2.1z"
                        />
                        <path d="M11.6 8.4 4.9 15a1.9 1.9 0 0 0 2.7 2.7l6.6-6.6" />
                        <path d="M3.5 21h11" />
                    </svg>
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
    gap: var(--tt-space-2);
}

/* 소등 — 남아 있지 않은 목숨 */
.life__gavel {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 40px;
    height: 40px;
    color: var(--tt-gray-300);
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-sm);
}

/* 점등 — 판사봉은 골드·목재 계열을 쓰는 자리 */
.life__gavel--lit {
    color: var(--tt-accent-700);
    background: var(--tt-accent-subtle);
    border-color: var(--tt-accent);
}

.life__gavel svg {
    width: 24px;
    height: 24px;
}
</style>
