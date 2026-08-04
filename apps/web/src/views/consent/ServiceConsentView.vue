<!--
  용도: 로그인 직후 필수/선택 약관 동의를 받는 화면 (기능명세 AU_01_03).
  언제 쓰는지: needsConsent 가 true 인 사용자. 라우터 가드가 자동으로 여기로 보낸다.
  쓰면 안 되는 경우: 계좌 연동 직전 CODEF 동의(그건 FinancialConsentView).

  ‹ 뒤로가기는 로그아웃이다. 로그인 직후라 돌아갈 화면이 로그인밖에 없고,
  명세의 "필수 동의 거부 시 가입 중단"에 대응한다. 계정 행은 남으므로
  구글 재로그인만 하면 이 화면부터 이어서 할 수 있다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useConsentStore } from '@/stores/consent';
import { useAuthStore } from '@/stores/auth';
import { logout } from '@/api/auth';
import {
    buildAgreementState,
    buildAgreementStateFromConsents,
    canSubmit,
    isAllChecked,
    toAgreements,
    toggleAll,
} from '@/utils/consent';
import ConsentAllCheck from '@/components/consent/ConsentAllCheck.vue';
import ConsentCheckRow from '@/components/consent/ConsentCheckRow.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';

const router = useRouter();
const auth = useAuthStore();
const consent = useConsentStore();
const { catalog, isLoading } = storeToRefs(consent);

const agreementState = ref({});
const errorMessage = ref('');
const isSaving = ref(false);

const items = computed(() => catalog.value?.items ?? []);
const allChecked = computed(() => items.value.length > 0 && isAllChecked(items.value, agreementState.value));
const submittable = computed(() => items.value.length > 0 && canSubmit(items.value, agreementState.value));

async function loadConsentCatalog() {
    // 재시도 시 이전 실패 메시지가 남아있으면 성공해도 화면에 계속 보인다.
    errorMessage.value = '';
    try {
        await consent.loadCatalog('SIGNUP');
    } catch (err) {
        errorMessage.value = err.message;
        return;
    }

    // 재동의 진입 시 기존 동의 상태로 체크박스를 시드한다 — 그래야 사용자가
    // 건드리지 않은 선택 항목(AI_USAGE 등)이 재제출 때 미동의로 덮어써지지 않는다.
    // 이 조회가 실패해도 화면은 막지 않는다: 최초 사용자는 원래 행이 없고,
    // 이 화면은 라우터 가드가 강제하는 게이트라 우회할 수 없기 때문이다.
    try {
        await consent.loadMyConsents();
        agreementState.value = buildAgreementStateFromConsents(items.value, consent.myConsents);
    } catch {
        agreementState.value = buildAgreementState(items.value);
    }
}

onMounted(loadConsentCatalog);

function onToggleAll(checked) {
    agreementState.value = toggleAll(items.value, checked);
}

async function onSubmit() {
    if (!submittable.value || isSaving.value) {
        return;
    }
    isSaving.value = true;
    errorMessage.value = '';
    try {
        await consent.save('SIGNUP', toAgreements(agreementState.value));
        router.replace({ name: 'home' });
    } catch (err) {
        errorMessage.value = err.message;
    } finally {
        isSaving.value = false;
    }
}

async function onBack() {
    // 동의하지 않고 나가는 것은 가입 중단이다. 세션을 정리하고 로그인으로 되돌린다.
    try {
        await logout();
    } finally {
        auth.clear();
        router.replace({ name: 'login' });
    }
}
</script>

<template>
    <div class="consent-view">
        <header class="consent-view__header">
            <button class="consent-view__back" type="button" aria-label="동의 취소하고 로그아웃" @click="onBack">‹</button>
            <h1 class="consent-view__title">서비스 동의</h1>
        </header>

        <StateLoading v-if="isLoading" message="약관을 불러오는 중" />

        <template v-else-if="items.length > 0">
            <p class="consent-view__lead">시작 전, 몇 가지<br />동의가 필요해요</p>

            <ConsentAllCheck :model-value="allChecked" @update:model-value="onToggleAll" />

            <div class="consent-view__list">
                <ConsentCheckRow
                    v-for="item in items"
                    :key="item.type"
                    v-model="agreementState[item.type]"
                    :type="item.type"
                    :label="item.label"
                    :required="item.required"
                    :terms-url="item.termsUrl"
                />
            </div>

            <p class="consent-view__notice">
                금융정보·AI 활용 동의는 <strong>언제든 마이페이지에서 철회</strong>할 수 있어요.
            </p>

            <p v-if="errorMessage" class="consent-view__error">{{ errorMessage }}</p>

            <div class="consent-view__cta">
                <BaseButton block size="lg" :disabled="!submittable" :loading="isSaving" @click="onSubmit">
                    동의하고 시작하기
                </BaseButton>
            </div>
        </template>

        <StateError
            v-else
            :message="errorMessage || '약관을 불러오지 못했습니다.'"
            @retry="loadConsentCatalog"
        />
    </div>
</template>

<style scoped>
.consent-view {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    min-height: 100vh;
    padding: var(--tt-space-5);
    background: var(--tt-bg);
}

.consent-view__header {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
}

.consent-view__back {
    border: 0;
    background: none;
    font-size: var(--tt-fs-title);
    color: var(--tt-text);
    cursor: pointer;
    padding: 0;
    line-height: 1;
}

.consent-view__title {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    margin: 0;
}

.consent-view__lead {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    line-height: var(--tt-lh-tight);
    color: var(--tt-text);
    margin: 0;
}

.consent-view__list {
    display: flex;
    flex-direction: column;
}

.consent-view__notice {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    background: var(--tt-bg-subtle);
    border-radius: var(--tt-radius-md);
    padding: var(--tt-space-4);
    margin: 0;
}

.consent-view__notice strong {
    color: var(--tt-primary);
}

.consent-view__error {
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
    margin: 0;
}

.consent-view__cta {
    margin-top: auto;
    padding-top: var(--tt-space-6);
}
</style>
