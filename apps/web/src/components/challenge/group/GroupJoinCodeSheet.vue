<script setup>
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import GroupCodeInput from '@/components/challenge/group/GroupCodeInput.vue';
import GroupExpiredModal from '@/components/challenge/group/GroupExpiredModal.vue';
import GroupNotFoundModal from '@/components/challenge/group/GroupNotFoundModal.vue';
import { previewInviteCode } from '@/api/groupChallenge';

defineProps({
    modelValue: { type: Boolean, required: true },
});

const emit = defineEmits(['update:model-value']);

const router = useRouter();
const sheetRef = ref(null);
const code = ref('');
const isLoading = ref(false);
const showExpired = ref(false);
const blockedGroup = ref(null);
const blockedReason = ref('');
const showNotFound = ref(false);
const notFoundCode = ref('');
const errorText = ref('');

function closeSheet() {
    emit('update:model-value', false);
    code.value = '';
    errorText.value = '';
}

/*
 * 시트 안에서 다른 화면으로 나갈 때는 시트가 쌓아둔 히스토리 항목을 넘겨받은 뒤
 * push 가 아닌 replace 로 옮긴다. 그러지 않으면 시트가 닫히며 실행하는 history.back() 이
 * 방금 한 이동을 되감아 「입장하기」를 눌러도 아무 일도 안 난 것처럼 보인다.
 * (useOverlay 의 releaseHistory 주석 참고)
 */
function leaveSheet(location) {
    sheetRef.value?.releaseHistory?.();
    closeSheet();
    router.replace(location);
}

async function handleEnter() {
    if (code.value.length < 5 || isLoading.value) return;

    /* closeSheet() 가 입력값을 비우므로 넘길 코드를 먼저 붙잡아 둔다.
     * 예전에 closeSheet() 뒤에 code.value 를 읽어 빈 코드로 이동했고,
     * 참여 화면은 코드가 없다고 판단해 홈으로 되돌렸다 — 눌러도 아무 일도 안 나는 것처럼 보였다. */
    const entered = code.value;

    isLoading.value = true;
    errorText.value = '';
    try {
        const result = await previewInviteCode(entered);

        /* 이미 참여 중이면 「참여할까요?」를 다시 물을 이유가 없다. 목록으로 보낸다. */
        if (result.reason === 'ALREADY_JOINED') {
            leaveSheet({ name: 'groupChallengeList' });
            return;
        }
        if (!result.joinable) {
            blockedGroup.value = result.group;
            blockedReason.value = result.reason;
            showExpired.value = true;
            return;
        }
        leaveSheet({
            name: 'groupChallengeJoin',
            params: { code: entered },
        });
    } catch (e) {
        /* 실패를 한 덩어리로 묶으면 코드 오타인지 로그인 만료인지 서버 오류인지 구분할 수 없다.
         * 서버가 「코드 없음」이라고 답한 경우만 전용 모달이고, 나머지는 실제 메시지를 보여준다. */
        if (e.code === 'GROUP_INVITE_CODE_NOT_FOUND') {
            notFoundCode.value = entered;
            showNotFound.value = true;
        } else {
            errorText.value = e.message ?? '초대 코드를 확인하지 못했습니다.';
        }
    } finally {
        isLoading.value = false;
    }
}

function handleExpiredConfirm() {
    showExpired.value = false;
    code.value = '';
    blockedGroup.value = null;
    blockedReason.value = '';
}

function handleNotFoundConfirm() {
    showNotFound.value = false;
    code.value = '';
    notFoundCode.value = '';
}
</script>

<template>
    <BaseBottomSheet
        ref="sheetRef"
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

            <p v-if="errorText" class="gjcs-error">{{ errorText }}</p>

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

    <!-- 참여 불가 모달 (만료 · 정원 초과 · 종료) -->
    <GroupExpiredModal
        v-model="showExpired"
        :reason="blockedReason"
        :group-name="blockedGroup?.groupName ?? ''"
        :group-code="blockedGroup?.inviteCode ?? ''"
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

/* ── 실패 안내 ─────────────────────────── */
.gjcs-error {
    margin-top: 14px;
    padding: 10px 14px;
    background: var(--tt-danger-subtle);
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger-deep);
    text-align: center;
    line-height: 1.5;
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
