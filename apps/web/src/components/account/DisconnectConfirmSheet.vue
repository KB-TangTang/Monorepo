<!--
  용도: 계좌 연결 해제 전 영향 범위를 알리는 확인 바텀시트 (AC_01_04).
  언제 쓰는지: 연결 계좌 관리에서 `연결 해제` 를 눌렀을 때.
  쓰면 안 되는 경우: 단순 안내. 여기는 파괴적 액션 확인 전용이다.

  본문 4줄은 FIX_C_계좌추가연결_홈정리.md 의 확정 카피 그대로다 —
  명세 AC_01_04 의 "해제 전 영향 범위를 안내하고 확인 후 수집을 중단한다" 요건을 채우는 핵심이라 임의로 줄이지 않는다.
  파괴적 액션(연결 해제)이 primary 가 되지 않게 색·비중을 배치한다.
-->
<script setup>
import { computed } from 'vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';

const props = defineProps({
    modelValue: { type: Boolean, required: true },
    account: { type: Object, default: null },
    loading: { type: Boolean, default: false },
});

const emit = defineEmits(['update:modelValue', 'confirm']);

const IMPACTS = [
    '이 계좌의 거래 수집이 즉시 멈춰요',
    '진행 중인 챌린지 판정에서 이 계좌 소비가 빠져요',
    '이미 수집된 거래·리포트는 보관돼요',
    '다시 연결하면 본인 인증을 새로 해야 해요',
];

const title = computed(() =>
    props.account ? `${props.account.bankName} 연결을 해제할까요?` : '연결을 해제할까요?',
);

function keep() {
    emit('update:modelValue', false);
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        @update:model-value="emit('update:modelValue', $event)"
    >
        <template #header>
            <p class="disconnect-sheet__eyebrow">NOTICE · 해제 전 확인</p>
            <h2 class="disconnect-sheet__title">{{ title }}</h2>
        </template>

        <ul class="disconnect-sheet__impacts">
            <li v-for="impact in IMPACTS" :key="impact">{{ impact }}</li>
        </ul>

        <!-- BaseBottomSheet 의 footer 는 flex 행이다. 두 버튼을 같은 너비로 나눠 갖는다. -->
        <template #footer>
            <div class="disconnect-sheet__destructive">
                <BaseButton
                    variant="ghost"
                    block
                    :loading="loading"
                    @click="emit('confirm', account)"
                >
                    연결 해제
                </BaseButton>
            </div>
            <BaseButton
                class="disconnect-sheet__keep"
                variant="primary"
                block
                :disabled="loading"
                @click="keep"
            >
                유지하기
            </BaseButton>
        </template>
    </BaseBottomSheet>
</template>

<style scoped>
.disconnect-sheet__eyebrow {
    margin: 0 0 var(--tt-space-2);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    letter-spacing: 0.1em;
    color: var(--tt-text-muted);
}

.disconnect-sheet__title {
    margin: 0;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.disconnect-sheet__impacts {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
    margin: 0;
    padding: 0 0 0 var(--tt-space-4);
    list-style: disc;
}

.disconnect-sheet__impacts li {
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

/* footer 가 flex 행이라 명시하지 않으면 콘텐츠 폭으로 줄어든다. */
.disconnect-sheet__destructive,
.disconnect-sheet__keep {
    flex: 1;
}

/* 파괴적 액션은 저채도 레드 아웃라인. 채움(primary)은 '유지하기' 가 가져간다.
 * BaseButton 의 ghost variant 를 덮어쓰는 것이라 자손 선택자로 특이도를 확보한다. */
.disconnect-sheet__destructive :deep(.tt-btn) {
    border-color: var(--tt-danger);
    color: var(--tt-danger);
}

.disconnect-sheet__destructive :deep(.tt-btn:not(:disabled):hover) {
    background: var(--tt-danger-subtle);
    border-color: var(--tt-danger);
}
</style>
