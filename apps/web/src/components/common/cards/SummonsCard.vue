<!--
  용도: 소환장(티켓) 카드. 그룹명 · 기간 · 참여 코드(모노) · 참여 CTA 를 티켓 모양으로 보여준다.
  언제 쓰는지: 커스텀 그룹챌린지 초대·참여 화면, 공유 링크로 들어온 사용자에게 보여줄 때.
  쓰면 안 되는 경우: 이미 참여한 챌린지의 진행 상황(RecordCard).
-->
<script setup>
import BaseCard from '@/components/common/BaseCard.vue';
import BaseButton from '@/components/common/BaseButton.vue';

defineProps({
    groupName: { type: String, required: true },
    period: { type: String, default: '' },
    memberText: { type: String, default: '' },
    joinCode: { type: String, default: '' },
    ctaLabel: { type: String, default: '참여하기' },
    disabled: { type: Boolean, default: false },
});

defineEmits(['join']);
</script>

<template>
    <BaseCard class="summons" padding="none">
        <div class="summons__body">
            <p class="summons__kicker">SUMMONS · 소환장</p>
            <h3 class="summons__name">{{ groupName }}</h3>
            <p v-if="period || memberText" class="summons__meta">
                {{ [period, memberText].filter(Boolean).join(' · ') }}
            </p>
            <slot />
        </div>

        <!-- 펀치홀 절취선 — 이미지 없이 gradient 로만 그린다 -->
        <div class="summons__perf" aria-hidden="true"></div>

        <div class="summons__stub">
            <div v-if="joinCode" class="summons__code">
                <span class="summons__code-label">참여 코드</span>
                <span class="summons__code-value">{{ joinCode }}</span>
            </div>
            <slot name="action">
                <BaseButton block :disabled="disabled" @click="$emit('join')">
                    {{ ctaLabel }}
                </BaseButton>
            </slot>
        </div>
    </BaseCard>
</template>

<style scoped>
.summons {
    overflow: hidden;
    background: var(--tt-kraft); /* 종이 질감 — 소환장은 종이 계열 허용 대상 */
    border-color: var(--tt-wood);
}

.summons__body {
    padding: var(--tt-space-5) var(--tt-space-5) var(--tt-space-4);
}

.summons__kicker {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.16em;
    color: var(--tt-wood);
}

.summons__name {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.summons__meta {
    margin-top: var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

/* 절취선: 가로 점선 + 양끝 펀치홀.
 * 노치는 카드가 페이지 배경(--tt-bg-subtle) 위에 놓인다고 가정한다. */
.summons__perf {
    position: relative;
    height: 1px;
    margin: 0 var(--tt-space-4);
    background: repeating-linear-gradient(to right, var(--tt-wood) 0 5px, transparent 5px 10px);
}

.summons__perf::before,
.summons__perf::after {
    position: absolute;
    top: -10px;
    width: 20px;
    height: 20px;
    content: '';
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-full);
}

.summons__perf::before {
    left: calc(var(--tt-space-4) * -1 - 10px);
}

.summons__perf::after {
    right: calc(var(--tt-space-4) * -1 - 10px);
}

.summons__stub {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4) var(--tt-space-5) var(--tt-space-5);
}

.summons__code {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-3);
    padding: var(--tt-space-2) var(--tt-space-3);
    background: var(--tt-bg);
    border: 1px dashed var(--tt-wood);
    border-radius: var(--tt-radius-sm);
}

.summons__code-label {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.summons__code-value {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.16em;
    color: var(--tt-text);
}
</style>
