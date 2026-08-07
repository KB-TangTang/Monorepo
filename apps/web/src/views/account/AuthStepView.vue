<!--
  용도: 연결 플로우 2단계(인증). 어떤 인증 화면을 그릴지 정하고 승인 여부를 폴링한다.
  언제 쓰는지: 기관 선택 직후. 계좌 목록 조회가 connectionId 를 요구하므로 계좌 선택보다 반드시 앞이다.

  ⚠ **이 화면은 자기가 목 모드인지 CODEF 모드인지 모른다.**
    백엔드 auth-methods 응답만 보고 패널을 고른다. 여기에 빌드 플래그 분기가 들어가면
    "설정 교체만으로 실 CODEF 전환" 이라는 전제가 깨진다. (DECISIONS.md 2026-08-05)

  폴링 타이머를 스토어가 아니라 이 뷰가 소유한다 — 스토어에 두면 화면을 떠나도 살아남아 누수가 된다.
-->
<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import LinkStepHeader from '@/components/account/LinkStepHeader.vue';
import SimpleAuthPanel from '@/components/account/SimpleAuthPanel.vue';
import InstitutionLoginPanel from '@/components/account/InstitutionLoginPanel.vue';
import { useAccountStore } from '@/stores/account';
import { AUTH_METHOD, isPollExpired, linkStepPosition } from '@/utils/account';

const store = useAccountStore();
const { authMethods, authView, error } = storeToRefs(store);

const position = linkStepPosition('auth');

/** 수단이 여러 개일 때 사용자가 고른 값. 하나뿐이면 쓰이지 않는다. */
const picked = ref(null);
const loginPanel = ref(null);

let timer = null;
let startedAt = null;

/** 실제로 그릴 수단. 서버가 하나만 내려주면 그것, 여러 개면 사용자가 고른 것. */
const method = computed(() => (authView.value.needsPicker ? picked.value : authView.value.method));

const activeMethod = computed(() => authMethods.value.find((item) => item.type === method.value));

const description = computed(() =>
    method.value === AUTH_METHOD.INSTITUTION_LOGIN
        ? '금융기관에 등록한 아이디와 비밀번호로 본인을 확인해요.'
        : '선택한 금융기관을 함께 연결해요.',
);

/* 다른 단계 화면과 같은 2단 헤더. `|` 가 줄바꿈이다. */
const headline = computed(() =>
    method.value === AUTH_METHOD.INSTITUTION_LOGIN
        ? '기관에 로그인해|본인을 확인해요'
        : '한 번만 인증하면|모두 연결돼요',
);

function methodLabel(type) {
    return type === AUTH_METHOD.INSTITUTION_LOGIN ? '금융기관 로그인' : '간편인증';
}

function stopPolling() {
    if (timer) {
        clearInterval(timer);
        timer = null;
    }
}

/**
 * 간편인증 승인 대기. 승인되면 다음 단계로 넘어간다.
 *
 * ⚠ 제한 시간은 **서버가 준 유효시간**을 쓴다. 예전에는 60초로 고정돼 있어서,
 *   화면은 5분을 카운트다운하는데 폴링은 1분에 멈췄다 —
 *   사용자가 앱에서 승인해도 아무 일도 일어나지 않는 정지 상태가 됐다.
 */
function startPolling(expiresInSeconds) {
    stopPolling();
    startedAt = new Date();
    const limit = Number(expiresInSeconds) > 0 ? Number(expiresInSeconds) : undefined;
    timer = setInterval(async () => {
        if (isPollExpired(startedAt, new Date(), limit)) {
            stopPolling();
            error.value = '인증 시간이 지났어요. 다시 시도해 주세요.';
            return;
        }
        try {
            const status = await store.checkAuthStatus();
            if (status === 'APPROVED') {
                stopPolling();
                store.goNextStep('auth');
            } else if (status === 'FAILED' || status === 'EXPIRED') {
                stopPolling();
                error.value = '인증에 실패했어요. 다시 시도해 주세요.';
            }
        } catch (err) {
            stopPolling();
            error.value = err.message;
        }
    }, 1000);
}

function onBack() {
    stopPolling();
    /* 기관 로그인은 기관이 여러 개면 이전 기관으로 돌아간다. 판단은 패널이 한다. */
    if (method.value === AUTH_METHOD.INSTITUTION_LOGIN && loginPanel.value) {
        loginPanel.value.goBack();
        return;
    }
    if (authView.value.needsPicker && picked.value) {
        picked.value = null;
        return;
    }
    store.goPrevStep('auth');
}

onMounted(() => store.loadAuthMethods());
onUnmounted(stopPolling);
</script>

<template>
    <div class="auth-step">
        <LinkStepHeader
            title="본인 인증"
            :headline="headline"
            :description="description"
            :step="position.current"
            :total-steps="position.total"
            @back="onBack"
        />

        <!-- 수단이 둘 이상일 때만 고르게 한다. 하나뿐이면 선택 화면은 헛걸음이다. -->
        <div v-if="authView.needsPicker && !picked" class="auth-step__picker">
            <button
                v-for="item in authMethods"
                :key="item.type"
                type="button"
                class="auth-step__method"
                @click="picked = item.type"
            >
                {{ methodLabel(item.type) }}
            </button>
        </div>

        <SimpleAuthPanel
            v-else-if="method === AUTH_METHOD.SIMPLE_AUTH"
            :providers="activeMethod?.providers ?? []"
            @requested="startPolling"
            @exit="store.goPrevStep('auth')"
        />

        <InstitutionLoginPanel
            v-else-if="method === AUTH_METHOD.INSTITUTION_LOGIN"
            ref="loginPanel"
            @done="store.goNextStep('auth')"
            @exit="store.goPrevStep('auth')"
        />

        <p v-else class="auth-step__loading">인증 수단을 불러오는 중이에요…</p>

        <!--
          인증 실패·만료를 여기서 말한다.
          예전에는 error 를 store 에만 쓰고 화면에 그리는 곳이 없어서,
          승인 대기 화면이 이유 없이 멈춘 것처럼 보였다.
        -->
        <p v-if="error" class="auth-step__error" role="alert">{{ error }}</p>
    </div>
</template>

<style scoped>
.auth-step__error {
    margin: var(--tt-space-4) 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
    text-align: center;
}

.auth-step {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-5);
    min-height: 100vh;
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.auth-step__picker {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.auth-step__method {
    padding: var(--tt-space-4);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    text-align: left;
    cursor: pointer;
}

.auth-step__loading {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}
</style>
