<script setup>
import BaseInput from '@/components/common/BaseInput.vue';
import BaseButton from '@/components/common/BaseButton.vue';

const props = defineProps({
    evalType: { type: String, required: true },
    groupName: { type: String, required: true },
});

const emit = defineEmits(['update:eval-type', 'update:group-name', 'next']);

const EVAL_TYPES = [
    { value: 'DAILY', label: '일일결산', desc: '매일 각자의 소비를 판정' },
    { value: 'PERIOD', label: '기간결산', desc: '기간 종료 후 각자의 누적 소비를 판정' },
];
</script>

<template>
    <div class="step-basic">
        <div class="step-basic__card">
            <div class="step-basic__section-label">결산 타입</div>

            <button
                v-for="t in EVAL_TYPES"
                :key="t.value"
                type="button"
                class="eval-radio"
                :class="{ 'eval-radio--selected': evalType === t.value }"
                @click="emit('update:eval-type', t.value)"
            >
                <span class="eval-radio__dot">
                    <span v-if="evalType === t.value" class="eval-radio__dot-fill" />
                </span>
                <span class="eval-radio__text">
                    <span class="eval-radio__label">{{ t.label }}</span>
                    <span class="eval-radio__desc">{{ t.desc }}</span>
                </span>
            </button>
        </div>

        <div class="step-basic__card">
            <BaseInput
                :model-value="groupName"
                label="그룹 이름"
                placeholder="예: 배달 소비 줄이기"
                :maxlength="20"
                @update:model-value="emit('update:group-name', $event)"
            />
            <p class="step-basic__hint">
                미참여 친구들도 바로 알아볼 수 있게
                <b>사용자가 직접 입력해요.</b>
            </p>
        </div>

        <div class="step-basic__bottom">
            <BaseButton
                variant="primary"
                size="lg"
                block
                :disabled="!groupName.trim()"
                @click="emit('next')"
            >
                다음
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
.step-basic {
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
    margin-top: -22px;
    position: relative;
    z-index: 2;
}

.step-basic__card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
    padding: var(--tt-space-4);
    margin: 0 var(--tt-screen-padding);
}

.step-basic__card + .step-basic__card {
    margin-top: 14px;
}

.step-basic__card:first-child {
    box-shadow: var(--tt-elevation-3);
}

.step-basic__section-label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
    letter-spacing: 0.02em;
}

/* ── eval type radio cards ───────────────── */
.eval-radio {
    display: flex;
    align-items: flex-start;
    gap: 11px;
    width: 100%;
    margin-top: 12px;
    border: 1.5px solid var(--tt-border);
    border-radius: 15px;
    padding: 13px 14px;
    background: transparent;
    cursor: pointer;
    text-align: left;
    transition: border-color 0.15s ease;
}

.eval-radio:first-of-type {
    margin-top: var(--tt-space-3);
}

.eval-radio--selected {
    border-width: 2px;
    border-color: var(--tt-primary);
}

.eval-radio__dot {
    width: 19px;
    height: 19px;
    border-radius: 50%;
    border: 2px solid #D6D2C8;
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
    margin-top: 1px;
    transition: border-color 0.15s ease;
}

.eval-radio--selected .eval-radio__dot {
    border-color: var(--tt-primary);
}

.eval-radio__dot-fill {
    width: 9px;
    height: 9px;
    border-radius: 50%;
    background: var(--tt-primary);
}

.eval-radio__text {
    display: flex;
    flex-direction: column;
}

.eval-radio__label {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.eval-radio:not(.eval-radio--selected) .eval-radio__label {
    color: var(--tt-text-body);
}

.eval-radio__desc {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    margin-top: 3px;
    line-height: 1.45;
}

/* ── hint ─────────────────────────────────── */
.step-basic__hint {
    font-size: 11.5px;
    color: var(--tt-text-muted);
    margin-top: 9px;
    line-height: 1.5;
}

.step-basic__hint b {
    color: var(--tt-text-body);
}

/* ── bottom button ────────────────────────── */
.step-basic__bottom {
    margin-top: auto;
    padding: var(--tt-space-3) var(--tt-screen-padding) var(--tt-space-5);
}
</style>
