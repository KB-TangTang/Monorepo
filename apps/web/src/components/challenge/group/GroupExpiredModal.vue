<script setup>
import BaseModal from '@/components/common/BaseModal.vue';
import worriedImg from '@/assets/images/emotions/16_worried.png';

defineProps({
    modelValue: { type: Boolean, required: true },
    groupName: { type: String, default: '' },
    groupCode: { type: String, default: '' },
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
        <div class="gem-content">
            <div class="gem-mascot-wrap">
                <img
                    :src="worriedImg"
                    alt="초대 만료 경고"
                    class="gem-mascot"
                />
            </div>

            <h3 class="gem-title">초대 가능한 시간이<br>지났어요</h3>
            <p class="gem-desc">
                이 그룹은 첫날 23:59에<br>초대가 마감됐어요.
            </p>

            <div v-if="groupName || groupCode" class="gem-info-banner">
                {{ groupName }}<template v-if="groupCode"> · {{ groupCode }}</template>
            </div>

            <button type="button" class="gem-confirm-btn" @click="handleConfirm">
                확인
            </button>
        </div>
    </BaseModal>
</template>

<style scoped>
.gem-content {
    text-align: center;
    padding: 4px 0;
}

.gem-mascot-wrap {
    width: 104px;
    height: 104px;
    margin: 0 auto;
    border-radius: 50%;
    background: rgba(224, 102, 75, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
}

.gem-mascot {
    width: 80px;
    height: 80px;
    object-fit: contain;
}

.gem-title {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    margin-top: 14px;
    letter-spacing: -0.01em;
    line-height: 1.3;
    color: var(--tt-text);
}

.gem-desc {
    font-size: 12.5px;
    color: var(--tt-text-muted);
    margin-top: 9px;
    line-height: 1.55;
}

.gem-info-banner {
    margin-top: 16px;
    background: #FFF6E2;
    border: 1px solid #F0E0B8;
    border-radius: 12px;
    padding: 11px 14px;
    font-size: 12.5px;
    font-weight: var(--tt-fw-black);
    color: #8A6A16;
}

.gem-confirm-btn {
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

.gem-confirm-btn:active {
    filter: brightness(1.15);
}
</style>
