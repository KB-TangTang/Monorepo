<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import GroupCodeInput from '@/components/challenge/group/GroupCodeInput.vue';
import GroupExpiredModal from '@/components/challenge/group/GroupExpiredModal.vue';
import GroupNotFoundModal from '@/components/challenge/group/GroupNotFoundModal.vue';
import { validateInviteCode } from '@/api/groupChallenge';

defineProps({
    modelValue: { type: Boolean, required: true },
});

const emit = defineEmits(['update:model-value']);

const router = useRouter();
const code = ref('');
const isLoading = ref(false);
const showExpired = ref(false);
const expiredGroup = ref(null);
const showNotFound = ref(false);
const notFoundCode = ref('');

function closeSheet() {
    emit('update:model-value', false);
    code.value = '';
}

async function handleEnter() {
    if (code.value.length < 5 || isLoading.value) return;

    isLoading.value = true;
    try {
        const result = await validateInviteCode(code.value);
        if (result.expired) {
            expiredGroup.value = result.group;
            showExpired.value = true;
            return;
        }
        closeSheet();
        router.push({
            name: 'challengeGroupJoin',
            params: { code: code.value },
        });
    } catch {
        notFoundCode.value = code.value;
        showNotFound.value = true;
    } finally {
        isLoading.value = false;
    }
}

function handleExpiredConfirm() {
    showExpired.value = false;
    code.value = '';
    expiredGroup.value = null;
}

function handleNotFoundConfirm() {
    showNotFound.value = false;
    code.value = '';
    notFoundCode.value = '';
}
</script>

<template>
    <BaseBottomSheet
        :model-value="modelValue"
        @update:model-value="emit('update:model-value', $event)"
        @close="closeSheet"
    >
        <div class="gjcs-content">
            <div class="gjcs-title-row">
                <span class="gjcs-title">참여코드 입장</span>
            </div>

            <p class="gjcs-desc">
                초대장 참여코드 5자리를 입력하면 그 그룹 법정에 배심원으로 입장해요.
            </p>

            <!-- SUMMONS 미니 배너 -->
            <div class="gjcs-banner">
                <span class="gjcs-banner__label">SUMMONS · 소환장</span>
                <span class="gjcs-banner__hint">영문+숫자 5자리</span>
            </div>

            <!-- OTP 코드 입력 -->
            <div class="gjcs-input-area">
                <GroupCodeInput
                    v-model="code"
                    :length="5"
                    :disabled="isLoading"
                />
            </div>

            <!-- 입장하기 버튼 -->
            <button
                type="button"
                class="gjcs-enter-btn"
                :class="{ 'gjcs-enter-btn--disabled': code.length < 5 || isLoading }"
                :disabled="code.length < 5 || isLoading"
                @click="handleEnter"
            >
                입장하기
            </button>

            <div class="gjcs-link">
                초대 링크가 있나요? ›
            </div>
        </div>
    </BaseBottomSheet>

    <!-- 만료 모달 -->
    <GroupExpiredModal
        v-model="showExpired"
        :group-name="expiredGroup?.groupName ?? ''"
        :group-code="expiredGroup?.inviteCode ?? ''"
        @confirm="handleExpiredConfirm"
    />

    <!-- 존재하지 않는 방 모달 -->
    <GroupNotFoundModal
        v-model="showNotFound"
        :input-code="notFoundCode"
        @confirm="handleNotFoundConfirm"
    />
</template>

<style scoped>
.gjcs-content {
    padding: 0 0 6px;
}

.gjcs-title-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gjcs-title {
    font-size: 17px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gjcs-desc {
    font-size: 12px;
    color: var(--tt-text-muted);
    margin-top: 8px;
    line-height: 1.55;
}

/* ── SUMMONS 배너 ──────────────────────── */
.gjcs-banner {
    margin-top: 14px;
    background: var(--tt-info-subtle);
    border: 1px solid #C9D6F5;
    border-radius: 12px;
    padding: 9px 13px;
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gjcs-banner__label {
    font-size: 11.5px;
    font-weight: var(--tt-fw-black);
    letter-spacing: 0.16em;
    color: var(--tt-info);
}

.gjcs-banner__hint {
    font-size: 10.5px;
    font-weight: var(--tt-fw-bold);
    color: #7E93D6;
}

/* ── 입력 영역 ─────────────────────────── */
.gjcs-input-area {
    margin-top: 16px;
}

/* ── 입장하기 버튼 ─────────────────────── */
.gjcs-enter-btn {
    width: 100%;
    margin-top: 18px;
    background: var(--tt-info);
    color: var(--tt-text-inverse);
    font-weight: var(--tt-fw-black);
    font-size: 15.5px;
    padding: 16px;
    border-radius: 15px;
    border: none;
    text-align: center;
    cursor: pointer;
    font-family: inherit;
    transition: filter 0.15s ease, opacity 0.15s ease;
}

.gjcs-enter-btn:active {
    filter: brightness(0.92);
}

.gjcs-enter-btn--disabled {
    opacity: 0.45;
    cursor: not-allowed;
}

/* ── 링크 ──────────────────────────────── */
.gjcs-link {
    text-align: center;
    margin-top: 13px;
    font-size: 13px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-body);
    cursor: pointer;
}
</style>
