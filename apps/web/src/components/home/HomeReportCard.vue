<!--
  용도: 홈의 「N월 소비습관 변화」 카드. 확정된 월간 리포트의 절감액을 요약하고 리포트 화면으로 보낸다.
  #450 수정 — 원래 홈에서 유일하게 남색 배경이라 아래쪽에 홀로 떠 있었다. 「나의 기록」 자리로 올리면서
  다른 카드와 같은 흰 BaseCard 로 맞췄다. 강조는 배경이 아니라 금액 한 줄로 준다.
-->
<script setup>
import { computed } from 'vue';
import BaseCard from '@/components/common/BaseCard.vue';
import { formatHomeAmount, getHomeReportEmptyCopy } from '@/utils/home';

const props = defineProps({
    /* toHomeReportSummary() 의 결과. 확정된 리포트가 없으면 null. */
    summary: { type: Object, default: null },
    /* 'ready' | 'empty' | 'not-agreed' | 'preparing' | 'error' — summary 가 null 일 때 문구를 가른다. */
    status: { type: String, default: 'empty' },
});

defineEmits(['open']);

const emptyCopy = computed(() => getHomeReportEmptyCopy(props.status));
</script>

<template>
    <BaseCard class="report" clickable padding="none" @click="$emit('open')">
        <p class="report__label">
            {{ summary ? `${summary.month}월 소비습관 변화` : '소비습관 변화 리포트' }}
        </p>

        <span class="report__chevron" aria-hidden="true">›</span>

        <template v-if="summary">
            <p class="report__headline">
                <b>{{ formatHomeAmount(summary.savedAmount) }}원</b> 아꼈어요
            </p>
            <p class="report__caption">
                {{
                    summary.topCategoryName
                        ? `${summary.topCategoryName}에서 가장 큰 변화가 있었어요`
                        : '확정된 챌린지 결과를 확인해 보세요'
                }}
            </p>
        </template>

        <template v-else>
            <p class="report__headline report__headline--empty">{{ emptyCopy.title }}</p>
            <p class="report__caption">{{ emptyCopy.description }}</p>
        </template>
    </BaseCard>
</template>

<style scoped>
.report {
    position: relative;
    padding: var(--tt-space-4);
    border-radius: var(--tt-radius-xl);
}

.report__label {
    padding-right: var(--tt-space-5);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.report__chevron {
    position: absolute;
    top: var(--tt-space-4);
    right: var(--tt-space-4);
    color: var(--tt-text-hint);
    font-size: var(--tt-fs-label);
    line-height: 1;
}

.report__headline {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
}

/* 절감액만 골드로 띄운다 — 남색 배경을 걷어낸 자리의 강조를 여기로 옮겼다. */
.report__headline b {
    color: var(--tt-accent-deep);
    font-weight: var(--tt-fw-black);
}

.report__headline--empty {
    font-size: var(--tt-fs-body);
    line-height: var(--tt-lh-snug);
}

.report__caption {
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}
</style>
