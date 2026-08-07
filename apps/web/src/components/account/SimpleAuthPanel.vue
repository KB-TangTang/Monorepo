<!--
  용도: 마이데이터형 간편인증으로 선택한 기관을 한 번에 연결한다. 인증 단계의 목 모드 경로.
  언제 쓰는지: 백엔드 auth-methods 가 SIMPLE_AUTH 를 내려줄 때. 어느 경로인지는 AuthStepView 가 정한다.
  쓰면 안 되는 경우: 실 CODEF 기관 로그인. 그쪽은 InstitutionLoginPanel 이다.

  ⚠ 개인정보 취급 — 생년월일·통신사·휴대폰은 실제 마이데이터에서 "누구에게 인증 푸시를 보낼지"를
    정하는 값이다. 목 모드에는 보낼 인증기관이 없으므로
    · 서버로 보내지 않는다 (requestSimpleAuth 페이로드는 provider·organizations 뿐).
    · 스토어·로컬스토리지에 저장하지 않는다. 이 컴포넌트 지역 상태로만 들고 있다가 제출 즉시 비운다.
    · 형식만 검증한다 (utils/account.js validateSimpleAuthForm).
    (DECISIONS.md 2026-08-05 계좌 연동 항목)

  폴링은 하지 않는다. 요청이 나가면 requested 를 emit 하고 부모(AuthStepView)가 승인 여부를 확인한다.
-->
<script setup>
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue';
import BaseButton from '@/components/common/BaseButton.vue';
import { useAccountStore } from '@/stores/account';
import { useAuthStore } from '@/stores/auth';
import {
    formatBirthDate,
    formatCooldown,
    formatPhoneNumber,
    validateSimpleAuthForm,
} from '@/utils/account';

const props = defineProps({
    providers: { type: Array, default: () => [] },
});
const emit = defineEmits(['requested', 'exit']);

const account = useAccountStore();
const auth = useAuthStore();

/**
 * 통신사 목록.
 *
 * 알뜰폰을 한 줄로 두지 않고 **어느 망을 쓰는지까지** 고른다 — 인증기관이 망 단위로 구분해
 * 확인하기 때문이다. 실제 간편인증 화면들도 같은 방식으로 묻는다.
 * 여기 `code` 는 우리 서비스의 값이고, 공급자별 코드로 바꾸는 일은 백엔드 어댑터가 한다.
 */
const CARRIERS = [
    { code: 'SKT', label: 'SKT' },
    { code: 'KT', label: 'KT' },
    { code: 'LGU', label: 'LG U+' },
    { code: 'SKT_MVNO', label: 'SKT 알뜰폰' },
    { code: 'KT_MVNO', label: 'KT 알뜰폰' },
    { code: 'LGU_MVNO', label: 'LG U+ 알뜰폰' },
];

/*
 * 인증 수단 로고. 파일명이 공급자 코드다.
 * 서버가 새 수단을 내려줘도 화면이 깨지지 않게, 로고가 없으면 이름만 그린다.
 */
const PROVIDER_LOGOS = import.meta.glob('@/assets/images/auth/*.png', {
    eager: true,
    query: '?url',
    import: 'default',
});

function logoOf(code) {
    const found = Object.entries(PROVIDER_LOGOS).find(([path]) => path.endsWith(`/${code}.png`));
    return found ? found[1] : null;
}

const provider = ref(props.providers[0]?.code ?? '');
/* ⚠ 개인정보는 이 객체 밖으로 나가지 않는다. */
const form = reactive({ birthDate: '', carrier: '', phone: '' });
const errors = ref({});
const submitted = ref(false);
const waiting = ref(false);
const remaining = ref(0);
let countdown = null;

/*
 * 한 번 제출한 뒤에는 입력이 바뀔 때마다 다시 검증한다.
 * 고쳤는데도 빨간 글씨가 남아 있으면 사용자는 무엇이 잘못됐는지 계속 모른다.
 */
watch(form, () => {
    if (submitted.value) {
        errors.value = validateSimpleAuthForm(form).errors;
    }
});

const userName = computed(() => auth.user?.nickname ?? '');
const providerName = computed(
    () => props.providers.find((item) => item.code === provider.value)?.name ?? '인증 앱',
);

/*
 * 입력을 받는 즉시 화면이 보여준 형식으로 맞춘다.
 * v-model 대신 :value + @input 을 쓰는 이유는, v-model 은 사용자가 친 값을 그대로 넣은 뒤
 * 우리가 고치면 커서가 튀기 때문이다. 여기서 값을 정한 뒤 한 번만 반영한다.
 */
function onBirthDate(event) {
    form.birthDate = formatBirthDate(event.target.value);
    event.target.value = form.birthDate;
}

function onPhone(event) {
    form.phone = formatPhoneNumber(event.target.value);
    event.target.value = form.phone;
}

function clearPrivateInput() {
    form.birthDate = '';
    form.phone = '';
    form.carrier = '';
}

function stopCountdown() {
    if (countdown) {
        clearInterval(countdown);
        countdown = null;
    }
}

onBeforeUnmount(() => {
    stopCountdown();
    clearPrivateInput();
});

async function onSubmit() {
    submitted.value = true;
    const result = validateSimpleAuthForm(form);
    errors.value = result.errors;
    if (!result.valid) {
        return;
    }
    try {
        /*
         * 값을 넘기되 **실제로 전송할지는 스토어가 정한다**(서버의 requiresIdentity 기준).
         * 목 모드에서는 여기서 넘긴 값이 요청에 실리지 않고 아래에서 즉시 지워진다.
         */
        const response = await account.requestAuth(provider.value, {
            userName: userName.value,
            birthDate: form.birthDate,
            carrier: form.carrier,
            phoneNo: form.phone,
        });
        /* 검증이 끝나면 즉시 지운다. 화면에 남겨둘 이유가 없다.
         * 지우는 순간 watch 가 다시 검증해 오류가 되살아나므로 submitted 를 먼저 내린다. */
        submitted.value = false;
        errors.value = {};
        clearPrivateInput();
        waiting.value = true;
        remaining.value = response.expiresInSeconds;
        countdown = setInterval(() => {
            remaining.value = Math.max(0, remaining.value - 1);
            if (remaining.value === 0) {
                stopCountdown();
            }
        }, 1000);
        /* 폴링을 얼마나 돌릴지는 서버가 준 유효시간이 정한다. 화면 카운트다운과 같은 값이다. */
        emit('requested', response.expiresInSeconds);
    } catch {
        /* 오류 메시지는 스토어가 들고 있다. 여기서는 대기 화면으로 넘어가지 않기만 하면 된다. */
        waiting.value = false;
    }
}

function onCancel() {
    stopCountdown();
    waiting.value = false;
    emit('exit');
}
</script>

<template>
    <!-- 인증 요청 후: 승인 대기. 여백을 크게 두고 파동 하나만 남긴다. -->
    <div v-if="waiting" class="simple-auth simple-auth--waiting">
        <span class="simple-auth__pulse" aria-hidden="true">
            <span class="simple-auth__pulse-core"></span>
        </span>
        <p class="simple-auth__waiting-title">{{ providerName }}에서<br />인증을 완료해주세요</p>
        <p class="simple-auth__waiting-desc">인증을 마치면 자동으로 다음 단계로 넘어가요.</p>
        <p class="simple-auth__remaining">{{ formatCooldown(remaining) }}</p>
        <button class="simple-auth__cancel" type="button" @click="onCancel">취소</button>
    </div>

    <!-- 인증 요청 전: 수단 선택 + 본인 정보 -->
    <form v-else class="simple-auth" @submit.prevent="onSubmit">
        <p class="simple-auth__label">인증 수단</p>
        <div class="simple-auth__providers">
            <button
                v-for="item in providers"
                :key="item.code"
                type="button"
                class="simple-auth__provider"
                :class="{ 'simple-auth__provider--on': provider === item.code }"
                :aria-pressed="provider === item.code"
                @click="provider = item.code"
            >
                <img
                    v-if="logoOf(item.code)"
                    class="simple-auth__provider-logo"
                    :src="logoOf(item.code)"
                    alt=""
                    aria-hidden="true"
                />
                <span class="simple-auth__provider-name">{{ item.name }}</span>
                <span
                    v-if="provider === item.code"
                    class="simple-auth__provider-check"
                    aria-hidden="true"
                    >✓</span
                >
            </button>
        </div>

        <div class="simple-auth__form">
            <div class="simple-auth__field">
                <span class="simple-auth__field-label">이름</span>
                <input class="simple-auth__input" type="text" :value="userName" readonly />
            </div>

            <label class="simple-auth__field">
                <span class="simple-auth__field-label">생년월일</span>
                <input
                    :value="form.birthDate"
                    class="simple-auth__input"
                    type="text"
                    inputmode="numeric"
                    maxlength="6"
                    autocomplete="off"
                    placeholder="생년월일 6자리"
                    @input="onBirthDate"
                />
                <span v-if="errors.birthDate" class="simple-auth__error">{{
                    errors.birthDate
                }}</span>
            </label>

            <label class="simple-auth__field">
                <span class="simple-auth__field-label">통신사</span>
                <select v-model="form.carrier" class="simple-auth__input">
                    <option value="">선택해주세요</option>
                    <option v-for="carrier in CARRIERS" :key="carrier.code" :value="carrier.code">
                        {{ carrier.label }}
                    </option>
                </select>
                <span v-if="errors.carrier" class="simple-auth__error">{{ errors.carrier }}</span>
            </label>

            <label class="simple-auth__field">
                <span class="simple-auth__field-label">휴대폰번호</span>
                <input
                    :value="form.phone"
                    class="simple-auth__input"
                    type="tel"
                    inputmode="tel"
                    maxlength="13"
                    autocomplete="off"
                    placeholder="010-0000-0000"
                    @input="onPhone"
                />
                <span v-if="errors.phone" class="simple-auth__error">{{ errors.phone }}</span>
            </label>
        </div>

        <p v-if="account.error" class="simple-auth__error">{{ account.error }}</p>

        <p class="simple-auth__guard">
            입력하신 정보는 저장하지 않아요. 본인 확인에만 쓰이고 즉시 폐기돼요.
        </p>

        <BaseButton variant="dark" type="submit" block size="lg" :loading="account.loading">
            인증 요청
        </BaseButton>
    </form>
</template>

<style scoped>
.simple-auth {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
}

/* ── 승인 대기 ─────────────────────────────────────── */

.simple-auth--waiting {
    align-items: center;
    text-align: center;
    gap: var(--tt-space-3);
    padding-top: var(--tt-space-10);
}

/*
 * 파동 하나. 대기 화면에서 유일하게 움직이는 요소다 —
 * "요청은 갔고 상대의 응답을 기다린다"를 이모지나 스피너 없이 말한다.
 */
.simple-auth__pulse {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 96px;
    height: 96px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary-subtle);
}

.simple-auth__pulse::after {
    content: '';
    position: absolute;
    inset: 0;
    border-radius: var(--tt-radius-full);
    border: 2px solid var(--tt-primary);
    animation: simple-auth-ping 1.8s ease-out infinite;
}

.simple-auth__pulse-core {
    width: 28px;
    height: 28px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary);
}

@keyframes simple-auth-ping {
    0% {
        opacity: 0.6;
        transform: scale(1);
    }
    100% {
        opacity: 0;
        transform: scale(1.5);
    }
}

@media (prefers-reduced-motion: reduce) {
    .simple-auth__pulse::after {
        animation: none;
        opacity: 0.3;
    }
}

.simple-auth__waiting-title {
    margin: var(--tt-space-4) 0 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
}

.simple-auth__waiting-desc {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

/* 남은 시간은 대기 화면의 유일한 수치다. 모노로 크게 세운다. */
.simple-auth__remaining {
    margin: var(--tt-space-2) 0 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-numeric);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-primary);
}

.simple-auth__cancel {
    margin-top: var(--tt-space-4);
    padding: var(--tt-space-2) var(--tt-space-4);
    border: 0;
    background: none;
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
    cursor: pointer;
}

/* ── 수단 선택 · 입력 ──────────────────────────────── */

.simple-auth__label {
    margin: 0;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

.simple-auth__providers {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: var(--tt-space-2);
}

/* 기관 카드와 같은 문법 — 흰 카드, 선택 시 파란 테두리 + 우상단 체크. */
.simple-auth__provider {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--tt-space-2);
    padding: var(--tt-space-4) var(--tt-space-2);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    font-family: var(--tt-font-sans);
    cursor: pointer;
}

.simple-auth__provider--on {
    border-color: var(--tt-primary);
    border-width: 2px;
    padding: calc(var(--tt-space-4) - 1px) calc(var(--tt-space-2) - 1px);
}

/*
 * 로고 타일. PASS 는 흰 배경이라 카드 위에서 경계가 사라진다 — 옅은 테두리로 형태를 잡는다.
 * 세 로고의 크기·라운드를 맞춰 한 줄로 읽히게 한다.
 */
.simple-auth__provider-logo {
    width: 36px;
    height: 36px;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    object-fit: cover;
}

.simple-auth__provider-name {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.simple-auth__provider--on /*
 * 로고 타일. PASS 는 흰 배경이라 카드 위에서 경계가 사라진다 — 옅은 테두리로 형태를 잡는다.
 * 세 로고의 크기·라운드를 맞춰 한 줄로 읽히게 한다.
 */
.simple-auth__provider-logo {
    width: 36px;
    height: 36px;
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    object-fit: cover;
}

.simple-auth__provider-name {
    color: var(--tt-primary);
}

.simple-auth__provider-check {
    position: absolute;
    top: var(--tt-space-2);
    right: var(--tt-space-2);
    display: flex;
    align-items: center;
    justify-content: center;
    width: 18px;
    height: 18px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-primary);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-mono-chip);
}

/* 입력은 흰 카드 하나로 묶는다 — 낱개 필드가 배경 위에 떠 있으면 산만하다. */
.simple-auth__form {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    padding: var(--tt-space-5);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
}

.simple-auth__field {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-2);
}

.simple-auth__field-label {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.simple-auth__input {
    height: 48px;
    padding: 0 var(--tt-space-4);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    background: var(--tt-bg);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-body);
    color: var(--tt-text);
}

.simple-auth__input:read-only {
    background: var(--tt-bg-subtle);
    color: var(--tt-text-muted);
}

.simple-auth__input:focus {
    outline: 2px solid var(--tt-primary);
    outline-offset: -1px;
}

.simple-auth__error {
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

/* 고지는 「동기화 상태」의 안내 박스와 같은 문법. */
.simple-auth__guard {
    margin: 0;
    padding: var(--tt-space-4);
    border-radius: var(--tt-radius-md);
    background: var(--tt-primary-subtle);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}
</style>
