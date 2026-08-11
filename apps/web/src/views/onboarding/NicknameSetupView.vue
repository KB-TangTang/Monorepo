<!--
  용도: 온보딩 마지막 단계. 서비스에서 쓸 닉네임을 정한다 (DECISIONS.md 2026-08-11 「닉네임 온보딩 신설」).
  언제 쓰는지: 계좌 연동을 마치고 홈에 들어가기 직전. 닉네임이 비어 있으면 라우터 가드가 여기로 보낸다.
  쓰면 안 되는 경우: 닉네임 '수정'. 그건 마이페이지의 바텀시트(MyPageView)가 한다.

  ⚠ **건너뛰기·뒤로가기가 없는 강제 입력 화면이다.** 표시명 규칙이 `nickname ?? socialName` 이라
  닉네임이 비면 서비스 전역에서 이름이 흔들린다. 나가는 길은 저장 성공 하나뿐이다.
-->
<script setup>
import { computed, ref, watch } from 'vue';
import { useRouter } from 'vue-router';
import BaseButton from '@/components/common/BaseButton.vue';
import BaseInput from '@/components/common/BaseInput.vue';
import { updateMyNickname } from '@/api/user';
import { useAuthStore } from '@/stores/auth';
import { NICKNAME_MAX_LENGTH, validateNickname } from '@/utils/user';

const router = useRouter();
const auth = useAuthStore();

/*
 * 구글 계정 이름(socialName)으로 미리 채운다 — 그대로 확인만 하고 넘어갈 수 있어야 한다.
 * 실명(name)이 아니라 socialName 인 이유: 표시명 규칙이 `nickname ?? socialName` 이라
 * 손대지 않고 저장하면 지금까지 보이던 이름이 그대로 유지된다.
 */
const nickname = ref(auth.user?.socialName ?? '');
const errorMessage = ref('');
const isSaving = ref(false);
/* 처음 들어왔을 때부터 빨간 문구가 떠 있으면 아직 아무것도 안 한 사용자를 나무라는 꼴이 된다. */
const touched = ref(false);

const validation = computed(() => validateNickname(nickname.value));
const fieldError = computed(() => (touched.value ? validation.value.error : ''));

watch(nickname, () => {
    touched.value = true;
    /* 값을 고치는 순간 이전 저장 실패 문구는 사실이 아니게 된다. */
    errorMessage.value = '';
});

async function onSubmit() {
    if (!validation.value.valid || isSaving.value) {
        return;
    }
    isSaving.value = true;
    errorMessage.value = '';
    try {
        /* 응답이 갱신된 사용자 정보다. 스토어에 반영해야 가드가 다시 이 화면으로 되돌리지 않는다. */
        auth.mergeUser(await updateMyNickname(validation.value.value));
        /* replace 로 나간다 — push 면 홈에서 뒤로가기로 이 화면에 돌아오게 된다. */
        router.replace({ name: 'home' });
    } catch (err) {
        errorMessage.value = err.message ?? '닉네임을 저장하지 못했어요.';
    } finally {
        isSaving.value = false;
    }
}
</script>

<template>
    <div class="nickname-setup">
        <div class="nickname-setup__body">
            <p class="nickname-setup__step">마지막 단계</p>
            <h1 class="nickname-setup__title">어떻게 불러드릴까요?</h1>
            <p class="nickname-setup__lead">
                재판정과 랭킹에 보여줄 이름이에요.<br />
                나중에 마이페이지에서 바꿀 수 있어요.
            </p>

            <BaseInput
                v-model="nickname"
                class="nickname-setup__field"
                label="닉네임"
                placeholder="닉네임을 입력해주세요"
                :maxlength="NICKNAME_MAX_LENGTH"
                :error="fieldError"
                :disabled="isSaving"
                required
            />

            <p v-if="errorMessage" class="nickname-setup__error" role="alert">{{ errorMessage }}</p>
        </div>

        <div class="nickname-setup__cta">
            <BaseButton
                block
                size="lg"
                :disabled="!validation.valid"
                :loading="isSaving"
                @click="onSubmit"
            >
                이 이름으로 시작하기
            </BaseButton>
        </div>
    </div>
</template>

<style scoped>
.nickname-setup {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    padding: var(--tt-space-8) var(--tt-space-5);
    background: var(--tt-bg);
}

.nickname-setup__body {
    flex: 1;
}

.nickname-setup__step {
    margin: 0;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
}

.nickname-setup__title {
    margin: var(--tt-space-2) 0 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.nickname-setup__lead {
    margin: var(--tt-space-3) 0 0;
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.nickname-setup__field {
    margin-top: var(--tt-space-8);
}

.nickname-setup__error {
    margin: var(--tt-space-4) 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

.nickname-setup__cta {
    padding-top: var(--tt-space-6);
}
</style>
