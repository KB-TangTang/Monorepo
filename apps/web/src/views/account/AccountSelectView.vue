<!--
  용도: 인증한 기관에서 조회된 계좌 중 연결 대상을 고른다 (AC_01_03). 연결 플로우 3단계.
  언제 쓰는지: 기관 로그인 인증 성공 직후.
  쓰면 안 되는 경우: 이미 연결된 계좌 관리(ConnectedAccountView).

  레이아웃은 개발참고화면 `0-5[계좌연결]연결계좌선택.png` 기준이다 —
  기관별 그룹 헤더 · 체크 행 · 마스킹된 계좌번호 · 하단 고정 `선택한 계좌 N개` + CTA.
-->
<script setup>
import { computed, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import BaseButton from '@/components/common/BaseButton.vue';
import StateEmpty from '@/components/common/StateEmpty.vue';
import StateError from '@/components/common/StateError.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import AccountSelectRow from '@/components/account/AccountSelectRow.vue';
import InstitutionLogo from '@/components/account/InstitutionLogo.vue';
import LinkStepHeader from '@/components/account/LinkStepHeader.vue';
import { useAccountStore } from '@/stores/account';
import { linkStepPosition } from '@/utils/account';

const store = useAccountStore();
const {
    linkableGroups,
    loading,
    error,
    selectedAccountCount,
    linkableAccountCount,
    selectableAccountCount,
} = storeToRefs(store);

const position = linkStepPosition('select');

/*
 * 은행 없이 대출·페이머니만 고르면 체크할 계좌가 하나도 없다(#334) — 그래도 완료 버튼은 눌러야
 * 한다. selectedAccountCount 만 보고 막으면 아무것도 못 하는 화면이 된다.
 */
const hasAutoIncludedGroups = computed(() =>
    linkableGroups.value.some((group) => group.autoIncluded),
);

onMounted(() => {
    store.loadLinkableAccounts().catch(() => {});
});

async function onSubmit() {
    if (selectedAccountCount.value === 0 && !hasAutoIncludedGroups.value) {
        return;
    }
    try {
        await store.submitLink();
        store.goNextStep('select');
    } catch {
        /* 실패 메시지는 store.error 로 화면에 이미 노출된다. */
    }
}
</script>

<template>
    <div class="account-select">
        <LinkStepHeader
            title="연결할 계좌 선택"
            description="조회된 계좌 중 연결할 계좌를 선택하세요. 계좌번호는 마스킹돼요."
            :step="position.current"
            :total-steps="position.total"
            @back="store.restartFlow()"
        />

        <StateLoading v-if="loading && !linkableGroups.length" message="계좌를 조회하는 중" />
        <StateError
            v-else-if="error && !linkableGroups.length"
            :message="error"
            @retry="store.loadLinkableAccounts"
        />
        <StateEmpty
            v-else-if="!linkableAccountCount"
            title="가져올 계좌가 없어요"
            description="선택한 기관에 연결할 수 있는 계좌가 없어요. 이미 연결한 기관은 목록에서 빠져요."
        >
            <template #action>
                <BaseButton variant="dark" @click="store.restartFlow()">
                    기관 다시 선택
                </BaseButton>
            </template>
        </StateEmpty>

        <div v-else class="account-select__body">
            <section
                v-for="group in linkableGroups"
                :key="group.bankCode"
                class="account-select__group"
            >
                <h2 class="account-select__group-title">
                    <InstitutionLogo
                        :code="group.bankCode"
                        :short-label="group.shortLabel"
                        size="sm"
                    />
                    {{ group.bankName }}
                </h2>
                <!-- 대출·페이머니는 기관당 1건이라 고를 게 없다 — 최초 동기화가 자동으로 연동한다. -->
                <p v-if="group.autoIncluded" class="account-select__auto-notice">
                    선택한 기관이라 자동으로 연동돼요. 계좌를 따로 고를 필요는 없어요.
                </p>
                <div class="account-select__card">
                    <AccountSelectRow
                        v-for="account in group.accounts"
                        :key="account.accountId"
                        :account="account"
                        :selected="store.isAccountSelected(account.accountId)"
                        :auto-included="group.autoIncluded"
                        @toggle="store.toggleAccount"
                    />
                </div>
            </section>
        </div>

        <div v-if="linkableAccountCount" class="account-select__cta">
            <!--
              저장 실패는 여기서 말한다.
              위쪽 StateError 는 목록을 못 불러온 경우만 그리므로(`!linkableGroups.length`)
              저장 실패 시점에는 절대 뜨지 않는다 — 그대로 두면 버튼이 고장 난 것처럼 보인다.
            -->
            <p v-if="error" class="account-select__error" role="alert">{{ error }}</p>

            <!--
              조회는 됐는데 전부 이미 연결된 계좌인 경우.
              비활성 버튼만 남으면 사용자가 할 수 있는 일이 없다 — 다음 행동을 준다.
            -->
            <template v-if="!selectableAccountCount">
                <p class="account-select__notice">
                    이 기관의 계좌는 모두 연결돼 있어요. 다른 기관을 골라주세요.
                </p>
                <BaseButton variant="dark" block size="lg" @click="store.restartFlow()">
                    다른 기관 선택
                </BaseButton>
            </template>

            <template v-else>
                <!-- 참고화면 0-5: 라벨은 왼쪽 회색, 개수는 오른쪽 파란 강조 -->
                <p class="account-select__summary">
                    <span>선택한 계좌</span>
                    <strong>{{ selectedAccountCount }}개</strong>
                </p>
                <BaseButton
                    variant="dark"
                    block
                    size="lg"
                    :disabled="selectedAccountCount === 0 && !hasAutoIncludedGroups"
                    :loading="loading"
                    @click="onSubmit"
                >
                    선택한 계좌 연결
                </BaseButton>
            </template>
        </div>
    </div>
</template>

<style scoped>
.account-select {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-5);
    min-height: 100vh;
    padding: var(--tt-space-5);
    padding-bottom: var(--tt-space-12);
    background: var(--tt-bg-subtle);
}

.account-select__body {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-5);
    flex: 1;
}

.account-select__group-title {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
    margin: 0 0 var(--tt-space-3);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.account-select__card {
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
    overflow: hidden;
}

.account-select__cta {
    position: sticky;
    bottom: 0;
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
    margin-top: auto;
    padding-top: var(--tt-space-4);
    background: var(--tt-bg-subtle);
}

/* 참고화면 0-5: 라벨(왼쪽 회색) — 개수(오른쪽 파란 강조) 양끝 배치 */
.account-select__error {
    margin: 0 0 var(--tt-space-2);
    font-size: var(--tt-fs-caption);
    color: var(--tt-danger);
}

.account-select__notice {
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    text-align: center;
}

/* 대출·페이머니 미리보기 그룹의 안내 문구(#334). 그룹 제목과 카드 사이에 낀다. */
.account-select__auto-notice {
    margin: 0 0 var(--tt-space-3);
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.account-select__summary {
    display: flex;
    align-items: baseline;
    justify-content: space-between;
    margin: 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.account-select__summary strong {
    color: var(--tt-primary);
}
</style>
