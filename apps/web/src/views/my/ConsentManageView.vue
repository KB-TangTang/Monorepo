<!--
  용도: 동의 현황 조회·철회 화면 (DS_01_02). 마이페이지 > 동의 관리 진입점.
  언제 쓰는지: router 의 /my/consents. MyPageView 의 "동의 관리" 메뉴가 이 화면으로 보낸다.
  쓰면 안 되는 경우: 최초 가입 동의(ServiceConsentView)·계좌 연동 전 CODEF 동의(FinancialConsentView)를
    대체하지 말 것. 저쪽은 '받는' 화면이고 여기는 '되돌리는' 화면이다 — 두 화면을 건드리지 않는다.
-->
<script setup>
import { onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import { useConsentStore } from '@/stores/consent';
import { canWithdraw, consentStatusText } from '@/utils/consent';

/*
 * 금융정보 수집 동의(FINANCIAL_DATA)를 철회하면 백엔드 ConsentWithdrawnListener 가
 * 연결된 계좌를 전부 해제한다(THIRD_PARTY 도 함께 철회된다, ConsentService.withdraw 참고).
 * 그래서 이 항목만 확인 시트에 별도 경고 문구를 둔다.
 */
const FINANCIAL_DATA = 'FINANCIAL_DATA';

const store = useConsentStore();
const { myConsents, isLoading } = storeToRefs(store);

const errorMessage = ref('');
const sheetOpen = ref(false);
const target = ref(null);
const withdrawing = ref(false);
const actionError = ref('');

async function load() {
    errorMessage.value = '';
    try {
        await store.loadMyConsents();
    } catch (err) {
        errorMessage.value = err.message ?? '동의 정보를 불러오지 못했어요.';
    }
}

onMounted(load);

function askWithdraw(item) {
    if (!canWithdraw(item)) {
        return;
    }
    target.value = item;
    actionError.value = '';
    sheetOpen.value = true;
}

async function confirmWithdraw() {
    if (!target.value) {
        return;
    }
    withdrawing.value = true;
    actionError.value = '';
    try {
        // withdraw() 는 성공 후 store 가 목록을 스스로 다시 불러온다 — 여기서 다시 부르지 않는다.
        await store.withdraw(target.value.type);
        sheetOpen.value = false;
    } catch (err) {
        // 목록은 그대로 두고 시트에만 실패 사유를 보여준다. 토글 하나 실패했다고
        // 이미 불러온 나머지 목록을 지울 이유가 없다.
        actionError.value = err.message ?? '철회하지 못했어요.';
    } finally {
        withdrawing.value = false;
    }
}

function closeSheet() {
    sheetOpen.value = false;
    actionError.value = '';
}
</script>

<template>
    <div class="consent-manage">
        <header class="consent-manage__header">
            <h1 class="consent-manage__title">동의 관리</h1>
            <p class="consent-manage__lead">현재 동의 상태를 확인하고 철회할 수 있어요.</p>
        </header>

        <StateLoading v-if="isLoading" message="동의 정보를 불러오는 중" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="load" />
        <template v-else>
            <article v-for="item in myConsents" :key="item.type" class="consent-manage__card">
                <div class="consent-manage__text">
                    <p class="consent-manage__label">
                        <span :class="item.required ? 'is-required' : 'is-optional'">
                            [{{ item.required ? '필수' : '선택' }}]
                        </span>
                        {{ item.label }}
                    </p>
                    <p class="consent-manage__meta">{{ consentStatusText(item) }}</p>
                </div>
                <button
                    type="button"
                    class="consent-manage__toggle"
                    :class="{ 'is-on': item.agreed }"
                    role="switch"
                    :aria-checked="item.agreed"
                    :aria-label="`${item.label} 철회`"
                    :disabled="!canWithdraw(item)"
                    @click="askWithdraw(item)"
                >
                    <span class="consent-manage__toggle-knob" aria-hidden="true"></span>
                </button>
            </article>

            <p class="consent-manage__notice">
                동의를 철회하면 <strong>추가 수집과 동기화가 즉시 중단</strong>돼요. 기존 데이터는
                고지된 정책에 따라 보관·파기돼요.
            </p>
        </template>

        <BaseBottomSheet
            :model-value="sheetOpen"
            title="동의를 철회할까요?"
            @update:model-value="closeSheet"
        >
            <div class="consent-manage__sheet">
                <p class="consent-manage__sheet-text">
                    {{ target?.label }} 동의를 철회하면 추가 수집과 동기화가 즉시 중단돼요.
                    <strong v-if="target?.type === FINANCIAL_DATA">
                        연결된 계좌가 모두 해제되고, CODEF 제3자 제공 동의도 함께 철회돼요.
                    </strong>
                </p>
                <p v-if="actionError" class="consent-manage__sheet-error">{{ actionError }}</p>
                <BaseButton variant="danger" block :loading="withdrawing" @click="confirmWithdraw">
                    철회하기
                </BaseButton>
                <BaseButton variant="ghost" block :disabled="withdrawing" @click="closeSheet">
                    취소
                </BaseButton>
            </div>
        </BaseBottomSheet>
    </div>
</template>

<style scoped>
.consent-manage {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.consent-manage__title {
    margin: 0;
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.consent-manage__lead {
    margin: var(--tt-space-1) 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.consent-manage__card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: var(--tt-space-4);
    padding: var(--tt-space-4) var(--tt-space-5);
    background: var(--tt-bg);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-1);
}

.consent-manage__label {
    margin: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.is-required {
    color: var(--tt-danger);
}

.is-optional {
    color: var(--tt-text-muted);
}

.consent-manage__meta {
    margin: var(--tt-space-1) 0 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

/* ── 토글 스위치 (공용 컴포넌트 없음 — /dev/ui 확인 후 이 화면 전용으로 만듦) ── */
.consent-manage__toggle {
    position: relative;
    flex-shrink: 0;
    width: 52px;
    height: 30px;
    padding: 3px;
    border: 0;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
    cursor: pointer;
    transition: background-color 0.15s ease;
}

.consent-manage__toggle.is-on {
    background: var(--tt-primary);
}

.consent-manage__toggle:disabled {
    cursor: not-allowed;
    opacity: 0.6;
}

.consent-manage__toggle-knob {
    display: block;
    width: 24px;
    height: 24px;
    background: var(--tt-white);
    border-radius: var(--tt-radius-full);
    box-shadow: var(--tt-elevation-1);
    transition: transform 0.15s ease;
}

.consent-manage__toggle.is-on .consent-manage__toggle-knob {
    transform: translateX(22px);
}

.consent-manage__notice {
    margin: 0;
    padding: var(--tt-space-4);
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-accent-deep);
    background: var(--tt-accent-subtle);
    border-radius: var(--tt-radius-lg);
}

.consent-manage__sheet {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.consent-manage__sheet-text {
    margin: 0;
    font-size: var(--tt-fs-caption);
    line-height: var(--tt-lh-normal);
    color: var(--tt-text-muted);
}

.consent-manage__sheet-text strong {
    color: var(--tt-danger);
}

.consent-manage__sheet-error {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}
</style>
