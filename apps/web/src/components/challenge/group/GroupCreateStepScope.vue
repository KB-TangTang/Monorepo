<script setup>
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import GroupCategoryPicker from '@/components/challenge/group/GroupCategoryPicker.vue';

defineProps({
    categoryId: { type: Number, default: null },
    categoryName: { type: String, default: '총 소비' },
    rules: { type: String, required: true },
});

/*
 * 선택한 카테고리는 **id 와 이름을 함께** 올려보낸다. 최종 확인 화면이 「대상」 줄에 이름을
 * 보여줘야 하는데, 거기서 목록을 한 번 더 부르면 같은 이름을 두 곳에서 따로 풀게 된다.
 * 선택지 목록을 부르고 그리는 일은 GroupCategoryPicker 가 한다 — 이 단계는 그대로 흘려보낸다.
 */
const emit = defineEmits(['update:category-id', 'update:category-name', 'update:rules', 'next']);
</script>

<template>
    <div class="step-scope">
        <div class="step-scope__card step-scope__card--raised">
            <p class="step-scope__desc">지켜볼 소비 카테고리 하나를 선택할 수 있어요.</p>

            <GroupCategoryPicker
                :category-id="categoryId"
                :category-name="categoryName"
                @update:category-id="emit('update:category-id', $event)"
                @update:category-name="emit('update:category-name', $event)"
            />

            <div class="step-scope__info-box">
                기타 소비를 목록에 넣고 싶다면 카테고리 대신 <b>'총 소비'</b>를 선택해요.
            </div>
        </div>

        <div class="step-scope__card">
            <BaseInput
                :model-value="rules"
                label="우리 그룹 규칙"
                hint="친구들과 상황 내용을 자유롭게 작성해요."
                placeholder="야식 배달은 오후 9시 이후 금지"
                :maxlength="200"
                :rows="4"
                multiline
                @update:model-value="emit('update:rules', $event)"
            />
        </div>

        <div class="step-scope__bottom">
            <BaseButton variant="primary" size="lg" block @click="emit('next')">
                기간과 금액 설정
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
.step-scope {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    margin-top: -22px;
    position: relative;
    z-index: 2;
}

.step-scope__card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
    padding: var(--tt-space-4);
    margin: 0 var(--tt-screen-padding);
}

.step-scope__card--raised {
    box-shadow: var(--tt-elevation-3);
}

.step-scope__card + .step-scope__card {
    margin-top: 14px;
}

.step-scope__desc {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    line-height: 1.5;
}

/* ── info box (warm yellow) ──────────────── */
.step-scope__info-box {
    margin-top: 13px;
    background: #FFF6E2;
    border: 1px solid #F0E0B8;
    border-radius: 13px;
    padding: 11px 13px;
    font-size: 11.5px;
    color: #8A6A16;
    line-height: 1.5;
}

.step-scope__info-box b {
    font-weight: var(--tt-fw-black);
}

/* ── bottom button ────────────────────────── */
.step-scope__bottom {
    margin-top: auto;
    padding: var(--tt-space-3) var(--tt-screen-padding) var(--tt-space-5);
}
</style>
