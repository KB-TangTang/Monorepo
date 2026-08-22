<!--
  용도: 동의 현황 조회·철회·재동의 화면 (DS_01_02). 마이페이지 > 동의 관리 진입점.
  언제 쓰는지: router 의 /my/consents. MyPageView 의 "동의 관리" 메뉴가 이 화면으로 보낸다.
  쓰면 안 되는 경우: 최초 가입 동의(ServiceConsentView)·계좌 연동 전 CODEF 동의(FinancialConsentView)를
    대체하지 말 것. 저쪽은 '받는' 화면이고 여기는 '되돌리는' 화면이다 — 두 화면을 건드리지 않는다.
-->
<script setup>
import { onMounted, ref } from 'vue';
import { storeToRefs } from 'pinia';
import BaseBackButton from '@/components/common/BaseBackButton.vue';
import BaseBottomSheet from '@/components/common/BaseBottomSheet.vue';
import BaseButton from '@/components/common/BaseButton.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import { useConsentStore } from '@/stores/consent';
import {
    buildAgreeAgainPayload,
    canAgreeAgain,
    canWithdraw,
    consentStatusText,
} from '@/utils/consent';

/*
 * 금융정보 수집 동의(FINANCIAL_DATA)를 철회하면 백엔드 ConsentWithdrawnListener 가
 * 연결된 계좌를 전부 해제한다(THIRD_PARTY 도 함께 철회된다, ConsentService.withdraw 참고).
 * 이 항목은 ConsentScope.SIGNUP 의 필수 항목이기도 해서, 철회하면 서버가
 * needsConsent:true 를 돌려주고(ConsentService.needsConsent) auth 스토어가 그 값을 반영,
 * router 가드가 다음 이동부터 전부 /consent 로 돌려보낸다(router/index.js beforeEach 참고).
 * 즉 이 화면에 남아 있는 동안만 잠잠하고, 탭을 하나라도 누르면 서비스 동의 화면에 갇힌다.
 * 그래서 이 항목만 확인 시트에 별도 경고 문구를 둔다.
 */
const FINANCIAL_DATA = 'FINANCIAL_DATA';
const CHALLENGE = 'CHALLENGE';

const store = useConsentStore();
const { myConsents } = storeToRefs(store);

// 스토어의 isLoading 은 철회 후 백그라운드 재조회에도 켜진다(store.withdraw 내부 loadMyConsents).
// 그걸 그대로 쓰면 철회 직후 카드 목록이 통째로 StateLoading 으로 바뀐다 — 최초 진입 로딩만 이 화면 전용으로 갖는다.
const loading = ref(false);
const errorMessage = ref('');
const sheetOpen = ref(false);
const target = ref(null);
/* 철회·재동의가 같은 시트를 쓰므로 진행 플래그도 하나만 둔다 */
const submitting = ref(false);
const actionError = ref('');

async function load() {
    loading.value = true;
    errorMessage.value = '';
    try {
        await store.loadMyConsents();
    } catch (err) {
        errorMessage.value = err.message ?? '동의 정보를 불러오지 못했어요.';
    } finally {
        loading.value = false;
    }
}

onMounted(load);

/* 토글은 양방향이다. 켜져 있으면 철회를, 꺼져 있으면 재동의를 확인받는다.
 * 되돌릴 수단이 없으면 선택 항목(AI_USAGE·MARKETING)은 한 번 끄면 영영 못 켠다. */
function onToggle(item) {
    if (!canWithdraw(item) && !canAgreeAgain(item)) {
        return;
    }
    target.value = item;
    actionError.value = '';
    sheetOpen.value = true;
}

async function confirmAgreeAgain() {
    if (!target.value) {
        return;
    }
    submitting.value = true;
    actionError.value = '';
    try {
        await store.agreeAgain(
            target.value.scope,
            buildAgreeAgainPayload(myConsents.value, target.value),
        );
        // 철회와 같은 이유로 낙관적 반영을 함께 둔다 — 재조회가 실패해도 토글이 거짓말하지 않게.
        target.value.agreed = true;
        sheetOpen.value = false;
    } catch (err) {
        actionError.value = err.message ?? '다시 동의하지 못했어요.';
    } finally {
        submitting.value = false;
    }
}

async function confirmWithdraw() {
    if (!target.value) {
        return;
    }
    submitting.value = true;
    actionError.value = '';
    try {
        // withdraw() 는 성공 후 store 가 목록을 스스로 다시 불러온다 — 여기서 다시 부르지 않는다.
        await store.withdraw(target.value.type);
        // 재조회(store 내부)가 실패해도 서버 철회 자체는 이미 끝났다 — 토글이 계속 켜진 채로 보이면
        // 사용자가 다시 눌렀을 때 "철회할 동의 내역이 없습니다" 를 받는다. target 은 store.myConsents
        // 배열 안의 그 객체 참조라 여기서만 관리하는 별도 상태가 아니다. 재조회가 성공하면
        // loadMyConsents 가 배열 자체를 새로 받아온 데이터로 교체하므로 같은 값으로 수렴한다.
        target.value.agreed = false;
        sheetOpen.value = false;
    } catch (err) {
        // 목록은 그대로 두고 시트에만 실패 사유를 보여준다. 토글 하나 실패했다고
        // 이미 불러온 나머지 목록을 지울 이유가 없다.
        actionError.value = err.message ?? '철회하지 못했어요.';
    } finally {
        submitting.value = false;
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
            <!-- to 를 주지 않는다 — router.back() 이라야 들어온 자리로 돌아간다 -->
            <div class="consent-manage__head-row">
                <BaseBackButton label="이전 화면으로 돌아가기" />
                <h1 class="consent-manage__title">동의 관리</h1>
            </div>
            <p class="consent-manage__lead">
                현재 동의 상태를 확인하고 철회하거나 다시 동의할 수 있어요.
            </p>
        </header>

        <StateLoading v-if="loading" message="동의 정보를 불러오는 중" />
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
                    :aria-label="`${item.label} ${item.agreed ? '철회' : '다시 동의'}`"
                    :disabled="!canWithdraw(item) && !canAgreeAgain(item)"
                    @click="onToggle(item)"
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
            :title="target?.agreed ? '동의를 철회할까요?' : '다시 동의할까요?'"
            @update:model-value="closeSheet"
        >
            <div class="consent-manage__sheet">
                <p v-if="target?.agreed" class="consent-manage__sheet-text">
                    <template v-if="target?.type === CHALLENGE">
                        챌린지 참여 동의를 철회하면 다음 개인 미션 자동 배정이 중단돼요. 이미 배정된
                        미션과 상대형 미션 자격은 유지돼요.
                    </template>
                    <template v-else>
                        {{ target?.label }} 동의를 철회하면 추가 수집과 동기화가 즉시 중단돼요.
                    </template>
                    <strong v-if="target?.type === FINANCIAL_DATA">
                        연결된 계좌가 모두 해제되고, CODEF 제3자 제공 동의도 함께 철회돼요. 이
                        동의는 필수 항목이라 다시 동의하기 전까지 서비스를 이용할 수 없어요.
                    </strong>
                </p>
                <p v-else class="consent-manage__sheet-text">
                    <template v-if="target?.type === CHALLENGE">
                        챌린지 참여에 다시 동의하면 오늘 개인 미션이 없을 때 자동으로 배정돼요.
                    </template>
                    <template v-else>
                        {{ target?.label }} 동의를 다시 켜면 오늘부터 수집과 동기화가 재개돼요. 약관
                        전문은 동의 화면에서 확인할 수 있어요.
                    </template>
                </p>
                <p v-if="actionError" class="consent-manage__sheet-error">{{ actionError }}</p>
                <BaseButton
                    v-if="target?.agreed"
                    variant="danger"
                    block
                    :loading="submitting"
                    @click="confirmWithdraw"
                >
                    철회하기
                </BaseButton>
                <BaseButton
                    v-else
                    variant="primary"
                    block
                    :loading="submitting"
                    @click="confirmAgreeAgain"
                >
                    다시 동의하기
                </BaseButton>
                <BaseButton variant="ghost" block :disabled="submitting" @click="closeSheet">
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

/* 뒤로가기와 제목을 한 줄에. 알림 목록(.noti-list__header)과 같은 배치를 쓴다 */
.consent-manage__head-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
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
