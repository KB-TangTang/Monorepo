<script setup>
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';

defineProps({ modelValue: { type: Boolean, required: true } });
defineEmits(['update:modelValue', 'understood']);

const STEPS = [
    {
        title: '평소 하루를 먼저 구해요',
        description: '최근 4주 동안 그 카테고리에 돈을 쓴 날의 하루 평균이에요',
    },
    {
        title: '그날 미션 카테고리만 비교해요',
        description: '미션 밖의 지출은 순 절감액에 들어가지 않아요',
    },
    {
        title: '성공한 날은 덜 쓴 만큼, 실패한 날은 더 쓴 만큼',
        description: '성공과 실패의 차이를 모두 반영해요',
    },
    {
        title: '미션을 못 지킨 날에 덜 쓴 건 세지 않아요',
        description: '실제보다 넉넉하게 잡지 않으려고 성과에서 뺐어요',
    },
];
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        :close-on-overlay="false"
        :close-on-esc="false"
        :drag-close-threshold="10000"
        @update:model-value="$emit('update:modelValue', $event)"
    >
        <div class="guide">
            <h2>이 금액은 이렇게 나왔어요</h2>
            <p class="guide__lead">비교 대상은 남이 아니라 <strong>평소의 나</strong>예요.</p>
            <ol>
                <li v-for="(step, index) in STEPS" :key="step.title">
                    <span>{{ index + 1 }}</span>
                    <div>
                        <strong>{{ step.title }}</strong>
                        <p>{{ step.description }}</p>
                    </div>
                </li>
            </ol>
            <div class="guide__example">
                <p>평소 카페에 하루 <strong>8,200원</strong>을 쓰는데</p>
                <p>카페 미션 <strong>8일</strong> 동안 하루 <strong>3,200원</strong>만 썼어요</p>
                <strong>→ 5,000원 × 8일 = 40,000원</strong>
            </div>
            <button type="button" class="guide__confirm" @click="$emit('understood')">
                이해했어요
            </button>
        </div>
    </BaseBottomSheet>
</template>

<style scoped>
.guide h2 {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
}

.guide__lead {
    margin: var(--tt-space-2) 0 var(--tt-space-5);
    color: var(--tt-text-muted);
}

.guide__lead strong {
    color: var(--tt-text);
}

.guide ol {
    border-top: 1px solid var(--tt-border);
}

.guide li {
    display: flex;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4) 0;
    border-bottom: 1px solid var(--tt-border);
}

.guide li > span {
    display: grid;
    flex: 0 0 28px;
    width: 28px;
    height: 28px;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-accent);
    background: var(--tt-text);
    border-radius: var(--tt-radius-full);
    place-items: center;
}

.guide li strong,
.guide li p {
    display: block;
}

.guide li p {
    margin-top: var(--tt-space-1);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.guide__example {
    margin: var(--tt-space-5) 0;
    padding: var(--tt-space-4);
    background: var(--tt-bg-subtle);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
}

.guide__example > strong {
    display: block;
    margin-top: var(--tt-space-2);
    font-family: var(--tt-font-mono);
    color: var(--tt-success);
}

.guide__confirm {
    width: 100%;
    min-height: 54px;
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
    background: var(--tt-text);
    border: 0;
    border-radius: var(--tt-radius-lg);
    cursor: pointer;
}
</style>
