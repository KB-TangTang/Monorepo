<script setup>
import BaseModal from '@/components/common/BaseModal.vue';
import confusedImg from '@/assets/images/emotions/20_confused.png';

defineProps({
    modelValue: { type: Boolean, required: true },
    inputCode: { type: String, default: '' },
});

const emit = defineEmits(['update:model-value', 'confirm']);

function handleConfirm() {
    emit('confirm');
    emit('update:model-value', false);
}
</script>

<template>
    <BaseModal
        :model-value="modelValue"
        :show-close="false"
        :close-on-overlay="false"
        @update:model-value="emit('update:model-value', $event)"
    >
        <div class="gnf-content">
            <div class="gnf-mascot-wrap">
                <img
                    :src="confusedImg"
                    alt="존재하지 않는 방"
                    class="gnf-mascot"
                />
            </div>

            <h3 class="gnf-title">존재하지 않는<br>방이에요</h3>
            <p class="gnf-desc">
                입력한 코드와 일치하는 그룹을<br>찾을 수 없어요. 코드를 다시 확인해주세요.
            </p>

            <div v-if="inputCode" class="gnf-info-banner">
                입력한 코드 · {{ inputCode }}
            </div>

            <button type="button" class="gnf-confirm-btn" @click="handleConfirm">
                확인
            </button>
        </div>
    </BaseModal>
</template>

<style scoped>
.gnf-content {
    text-align: center;
    padding: 4px 0;
}

.gnf-mascot-wrap {
    width: 104px;
    height: 104px;
    margin: 0 auto;
    border-radius: 50%;
    background: rgba(62, 99, 214, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
}

.gnf-mascot {
    width: 80px;
    height: 80px;
    object-fit: contain;
}

.gnf-title {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    margin-top: 14px;
    letter-spacing: -0.01em;
    line-height: 1.3;
    color: var(--tt-text);
}

.gnf-desc {
    font-size: 12.5px;
    color: var(--tt-text-muted);
    margin-top: 9px;
    line-height: 1.55;
}

.gnf-info-banner {
    margin-top: 16px;
    background: var(--tt-bg-fill);
    border: 1px solid var(--tt-border);
    border-radius: 12px;
    padding: 11px 14px;
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-muted);
}

.gnf-confirm-btn {
    width: 100%;
    margin-top: 18px;
    background: var(--tt-surface-inverse);
    color: var(--tt-text-inverse);
    font-weight: var(--tt-fw-black);
    font-size: 15px;
    padding: 15px;
    border-radius: 14px;
    border: none;
    cursor: pointer;
    font-family: inherit;
    transition: filter 0.15s ease;
}

.gnf-confirm-btn:active {
    filter: brightness(1.15);
}
</style>
