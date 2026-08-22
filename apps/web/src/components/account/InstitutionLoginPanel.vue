<!--
  용도: 선택한 기관에 로그인해 CODEF connectedId 를 발급받는다 (AC_01_02). 인증 단계의 CODEF 경로.
  언제 쓰는지: 백엔드 auth-methods 가 INSTITUTION_LOGIN 을 내려줄 때만. 어느 경로인지는 AuthStepView 가 정한다.
  쓰면 안 되는 경우: 목 모드. 목서버에는 인증 API 가 없어 흉내낼 근거가 없다 → SimpleAuthPanel 을 쓴다.

  ⚠ 보안 원칙 — 여기서 받는 값은 실제 금융기관 자격증명이다.
    · 스토어·로컬스토리지에 저장하지 않는다. 이 컴포넌트의 지역 상태로만 들고 있다가 제출 후 즉시 비운다.
    · 브라우저 자동완성·비밀번호 저장을 끈다.
    · 화면을 벗어나면 남은 입력을 지운다.
    · 암호화(RSA)와 전송은 백엔드 몫이다. 프론트는 평문을 HTTPS 로 백엔드에만 넘긴다.

  단계 이동은 하지 않는다. 끝나면 done 을 emit 하고 부모(AuthStepView)가 옮긴다.
-->
<script setup>
import { computed, onBeforeUnmount, ref } from 'vue';
import { storeToRefs } from 'pinia';
import BaseButton from '@/components/common/BaseButton.vue';
import { useAccountStore } from '@/stores/account';
import InstitutionLogo from '@/components/account/InstitutionLogo.vue';
import { INSTITUTION_GROUPS } from '@/utils/account';

const emit = defineEmits(['done', 'exit']);

const store = useAccountStore();
const { institutions, selectedInstitutions, loading, error, needsExtraAuth, extraAuthType } =
    storeToRefs(store);

/* 선택한 기관을 순서대로 편다. 새로고침으로 선택이 비면 가드가 1단계로 되돌린다. */
const targets = computed(() => {
    const all = INSTITUTION_GROUPS.flatMap((group) => institutions.value?.[group.key] ?? []);
    return selectedInstitutions.value
        .map((code) => all.find((item) => item.code === code))
        .filter(Boolean);
});

const index = ref(0);
const current = computed(() => targets.value[index.value] ?? null);
const isLast = computed(() => index.value === targets.value.length - 1);

/* 자격증명은 여기에만 있다. 스토어로 올리지 않는다. */
const credentials = ref([]);
const loginId = ref('');
const password = ref('');
const extraAuthCode = ref('');
const fieldError = ref('');

/** 화면에 떠 있는 입력만 비운다. 누적된 자격증명은 건드리지 않는다. */
function clearInputs() {
    loginId.value = '';
    password.value = '';
    extraAuthCode.value = '';
}

/** 누적분까지 전부 버린다. 제출이 끝났거나 화면을 떠날 때만 부른다. */
function clearSecrets() {
    credentials.value = [];
    clearInputs();
}

onBeforeUnmount(clearSecrets);

async function onSubmit() {
    if (!loginId.value || !password.value) {
        fieldError.value = '아이디와 비밀번호를 모두 입력해 주세요.';
        return;
    }
    fieldError.value = '';

    credentials.value.push({
        organization: current.value.code,
        loginType: 'ID_PASSWORD',
        id: loginId.value,
        password: password.value,
    });
    loginId.value = '';
    password.value = '';

    if (!isLast.value) {
        index.value += 1;
        return;
    }

    try {
        const result = await store.connect(credentials.value);
        /* 제출이 끝나면 즉시 비운다. 성공·실패 무관. */
        const pending = result.needsExtraAuth;
        clearSecrets();
        if (!pending) {
            emit('done');
        }
    } catch {
        /* 실패하면 처음 기관부터 다시 받는다. 어느 기관이 틀렸는지 서버가 알려주지 않는다. */
        clearSecrets();
        index.value = 0;
    }
}

async function onSubmitExtraAuth() {
    if (!extraAuthCode.value) {
        fieldError.value = '인증번호를 입력해 주세요.';
        return;
    }
    fieldError.value = '';
    try {
        const result = await store.verifyExtraAuth({ authCode: extraAuthCode.value });
        extraAuthCode.value = '';
        /* 기관이 2차 추가인증을 또 요구할 수 있다. 끝났을 때만 다음 단계로 넘긴다. */
        if (!result?.needsExtraAuth) {
            emit('done');
        }
    } catch {
        extraAuthCode.value = '';
    }
}

/**
 * 헤더의 뒤로가기가 부른다. 기관이 여러 개면 이전 기관으로, 처음이면 단계를 벗어난다.
 *
 * ⚠ 여기서 누적분(`credentials`)을 비우면 안 된다. 예전에는 비웠고,
 *   그 결과 3곳 중 2곳을 입력한 뒤 뒤로 → 다시 진행하면 **1번 기관이 빠진 채**
 *   인증이 성공한 것처럼 다음 단계로 넘어갔다. 되돌아간 기관의 입력만 되돌린다.
 */
function goBack() {
    clearInputs();
    if (index.value > 0) {
        index.value -= 1;
        credentials.value = credentials.value.slice(0, index.value);
        return;
    }
    credentials.value = [];
    emit('exit');
}

defineExpose({ goBack });
</script>

<template>
    <div class="institution-login">
        <!-- 추가인증(2-way). 실 CODEF 에서 기관에 따라 요구한다. -->
        <form
            v-if="needsExtraAuth"
            class="institution-login__form"
            @submit.prevent="onSubmitExtraAuth"
        >
            <p class="institution-login__notice">
                {{ extraAuthType === 'SMS' ? '문자로 받은 인증번호' : '추가 인증번호' }}를 입력해
                주세요.
            </p>
            <label class="institution-login__field">
                <span class="institution-login__label">인증번호</span>
                <input
                    v-model="extraAuthCode"
                    class="institution-login__input"
                    type="text"
                    inputmode="numeric"
                    autocomplete="one-time-code"
                    placeholder="인증번호 6자리"
                />
            </label>

            <p v-if="fieldError || error" class="institution-login__error">
                {{ fieldError || error }}
            </p>

            <BaseButton variant="dark" type="submit" block size="lg" :loading="loading"
                >인증 완료</BaseButton
            >
        </form>

        <form v-else-if="current" class="institution-login__form" @submit.prevent="onSubmit">
            <div class="institution-login__target">
                <InstitutionLogo :code="current.code" :short-label="current.shortLabel" />
                <div>
                    <p class="institution-login__bank">{{ current.name }}</p>
                    <p class="institution-login__count">
                        {{ index + 1 }} / {{ targets.length }}번째 기관
                    </p>
                </div>
            </div>

            <label class="institution-login__field">
                <span class="institution-login__label">아이디</span>
                <input
                    v-model="loginId"
                    class="institution-login__input"
                    type="text"
                    autocomplete="off"
                    autocapitalize="off"
                    spellcheck="false"
                    placeholder="금융기관 아이디"
                />
            </label>

            <label class="institution-login__field">
                <span class="institution-login__label">비밀번호</span>
                <input
                    v-model="password"
                    class="institution-login__input"
                    type="password"
                    autocomplete="new-password"
                    placeholder="금융기관 비밀번호"
                />
            </label>

            <p v-if="fieldError || error" class="institution-login__error">
                {{ fieldError || error }}
            </p>

            <p class="institution-login__guard">
                입력하신 정보는 저장하지 않아요. 금융기관 인증에만 쓰이고 즉시 폐기돼요.
            </p>

            <BaseButton variant="dark" type="submit" block size="lg" :loading="loading">
                {{ isLast ? '인증하고 계좌 조회' : '다음 기관' }}
            </BaseButton>
        </form>
    </div>
</template>

<style scoped>
.institution-login {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-5);
}

.institution-login__form {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
}

.institution-login__target {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg-subtle);
}

.institution-login__bank {
    margin: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.institution-login__count {
    margin: 2px 0 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.institution-login__field {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.institution-login__label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.institution-login__input {
    height: 48px;
    padding: 0 var(--tt-space-4);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    background: var(--tt-bg);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
}

.institution-login__input:focus {
    outline: 2px solid var(--tt-primary);
    outline-offset: -1px;
}

.institution-login__notice {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.institution-login__error {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

.institution-login__guard {
    margin: 0;
    padding: var(--tt-space-3);
    border-radius: var(--tt-radius-md);
    background: var(--tt-primary-subtle);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}
</style>
