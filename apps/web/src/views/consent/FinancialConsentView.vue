<!--
  용도: 계좌 연동 직전 CODEF 제3자 제공 동의 (기능명세 DS_01_01).
  언제 쓰는지: 계좌 연동 흐름의 첫 화면. SIGNUP 동의를 마친 사용자만 도달한다.
  쓰면 안 되는 경우: 가입 직후 포괄 동의(그건 ServiceConsentView).

  화면 1의 FINANCIAL_DATA 는 "서비스가 내 금융데이터를 분석하는 것"에 대한 포괄 동의이고,
  여기 THIRD_PARTY 는 "CODEF 라는 제3자에게 계좌 정보를 제공하는 것"에 대한 개별 동의다.
-->
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useConsentStore } from '@/stores/consent';
import BaseButton from '@/components/common/BaseButton.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';

const router = useRouter();
const consent = useConsentStore();
const { catalog, isLoading } = storeToRefs(consent);

const agreed = ref(false);
const errorMessage = ref('');
const isSaving = ref(false);

const item = computed(() => catalog.value?.items?.[0] ?? null);
const termsVersion = computed(() => catalog.value?.termsVersion ?? '');

async function loadFinancialConsent() {
    // 재시도 시 이전 실패 메시지가 남아있으면 성공해도 화면에 계속 보인다.
    errorMessage.value = '';
    try {
        await consent.loadCatalog('FINANCIAL');
    } catch (err) {
        errorMessage.value = err.message;
    }
}

onMounted(loadFinancialConsent);

async function onSubmit() {
    if (!agreed.value || isSaving.value) {
        return;
    }
    isSaving.value = true;
    errorMessage.value = '';
    try {
        await consent.save('FINANCIAL', [{ type: 'THIRD_PARTY', agreed: true }]);
        // 이 동의는 계좌 연동의 앞단이라, 동의 직후 바로 기관 선택으로 이어간다(이슈 #12).
        router.replace({ name: 'accountLinkInstitutions' });
    } catch (err) {
        errorMessage.value = err.message;
    } finally {
        isSaving.value = false;
    }
}
</script>

<template>
    <div class="financial-consent">
        <header class="financial-consent__header">
            <button class="financial-consent__back" type="button" aria-label="뒤로 가기" @click="router.back()">
                ‹
            </button>
            <h1 class="financial-consent__title">금융데이터 수집 동의</h1>
        </header>

        <StateLoading v-if="isLoading" message="동의서를 불러오는 중" />

        <template v-else-if="item">
            <section class="financial-consent__card">
                <p class="financial-consent__card-title">CONSENT · 정보 제공 동의서</p>
                <dl class="financial-consent__table">
                    <div class="financial-consent__row">
                        <dt>수집 목적</dt>
                        <dd>자산·소비 분석 및 재판 챌린지 판정 제공</dd>
                    </div>
                    <div class="financial-consent__row">
                        <dt>수집 항목</dt>
                        <dd>계좌 잔액, 거래내역, 가맹점·업종 정보</dd>
                    </div>
                    <div class="financial-consent__row">
                        <dt>보유 기간</dt>
                        <dd>동의의 철회 시까지 (철회 후 정책에 따라 파기)</dd>
                    </div>
                </dl>
                <a
                    v-if="item.termsUrl"
                    class="financial-consent__link"
                    :href="item.termsUrl"
                    target="_blank"
                    rel="noopener noreferrer"
                >
                    전문 보기 ›
                </a>
            </section>

            <label class="financial-consent__agree">
                <input v-model="agreed" type="checkbox" class="financial-consent__check" />
                <span class="financial-consent__agree-label">위 내용에 모두 동의합니다</span>
                <span class="financial-consent__badge">[필수]</span>
            </label>

            <p class="financial-consent__warning">
                동의하지 않으면 계좌 연동과 데이터 수집을 진행할 수 없어요.
            </p>

            <p v-if="errorMessage" class="financial-consent__error">{{ errorMessage }}</p>

            <div class="financial-consent__cta">
                <BaseButton block size="lg" :disabled="!agreed" :loading="isSaving" @click="onSubmit">
                    동의하고 계좌 연결
                </BaseButton>
                <p class="financial-consent__version">약관 버전 {{ termsVersion }}</p>
            </div>
        </template>

        <StateError
            v-else
            :message="errorMessage || '동의서를 불러오지 못했습니다.'"
            @retry="loadFinancialConsent"
        />
    </div>
</template>

<style scoped>
.financial-consent {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    min-height: 100vh;
    padding: var(--tt-space-5);
    background: var(--tt-bg);
}

.financial-consent__header {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
}

.financial-consent__back {
    border: 0;
    background: none;
    font-size: var(--tt-fs-title);
    color: var(--tt-text);
    cursor: pointer;
    padding: 0;
    line-height: 1;
}

.financial-consent__title {
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
    margin: 0;
}

.financial-consent__card {
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
    overflow: hidden;
}

.financial-consent__card-title {
    margin: 0;
    padding: var(--tt-space-4);
    background: var(--tt-surface-inverse);
    color: var(--tt-accent);
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    text-align: center;
    letter-spacing: 0.1em;
}

.financial-consent__table {
    margin: 0;
    padding: var(--tt-space-4);
}

.financial-consent__row {
    display: flex;
    gap: var(--tt-space-4);
    padding: var(--tt-space-3) 0;
    border-bottom: 1px solid var(--tt-border);
}

.financial-consent__row:last-child {
    border-bottom: 0;
}

.financial-consent__row dt {
    flex-shrink: 0;
    width: 72px;
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-primary);
}

.financial-consent__row dd {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text);
    line-height: var(--tt-lh-normal);
}

.financial-consent__link {
    display: block;
    padding: 0 var(--tt-space-4) var(--tt-space-4);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-decoration: none;
}

.financial-consent__agree {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4) var(--tt-space-5);
    background: var(--tt-surface-inverse);
    border-radius: var(--tt-radius-lg);
    cursor: pointer;
}

.financial-consent__check {
    width: 22px;
    height: 22px;
    accent-color: var(--tt-accent);
    flex-shrink: 0;
}

.financial-consent__agree-label {
    flex: 1;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-inverse);
}

.financial-consent__badge {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-accent);
}

.financial-consent__warning {
    margin: 0;
    padding: var(--tt-space-4);
    background: var(--tt-danger-subtle);
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

.financial-consent__error {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

.financial-consent__cta {
    margin-top: auto;
    padding-top: var(--tt-space-6);
}

.financial-consent__version {
    margin: var(--tt-space-3) 0 0;
    text-align: center;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}
</style>
