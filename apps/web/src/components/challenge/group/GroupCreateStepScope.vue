<script setup>
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';

defineProps({
    categoryId: { type: Number, default: null },
    rules: { type: String, required: true },
});

const emit = defineEmits(['update:category-id', 'update:rules', 'next']);

// API 연동 시 실제 DB 값으로 교체 예정
const CATEGORIES = [
    { id: null, label: '총 소비' },
    { id: 1, label: '식비' },
    { id: 2, label: '카페' },
    { id: 3, label: '편의점' },
    { id: 4, label: '택시' },
    { id: 5, label: '쇼핑' },
];
</script>

<template>
    <div class="step-scope">
        <div class="step-scope__card step-scope__card--raised">
            <p class="step-scope__desc">서비스 대금 카테고리 중 하나를 선택할 수 있어요.</p>
            <div class="step-scope__chips">
                <button
                    v-for="cat in CATEGORIES"
                    :key="cat.id ?? 'all'"
                    type="button"
                    class="scope-chip"
                    :class="{ 'scope-chip--active': categoryId === cat.id }"
                    @click="emit('update:category-id', cat.id)"
                >
                    {{ cat.label }}
                </button>
            </div>
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

/* ── category chips ──────────────────────── */
.step-scope__chips {
    margin-top: var(--tt-space-3);
    display: flex;
    flex-wrap: wrap;
    gap: var(--tt-space-2);
}

.scope-chip {
    background: var(--tt-bg-fill);
    color: var(--tt-text-body);
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    padding: 8px 15px;
    border-radius: var(--tt-radius-full);
    border: none;
    cursor: pointer;
    transition: background-color 0.15s ease, color 0.15s ease;
}

.scope-chip--active {
    background: var(--tt-primary);
    color: var(--tt-text-inverse);
    font-weight: var(--tt-fw-black);
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
