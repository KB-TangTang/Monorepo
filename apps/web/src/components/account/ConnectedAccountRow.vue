<!--
  용도: 연결 계좌 관리 목록의 행 하나 (AC_01_04). 상태 · 잔액 · 액션 진입점을 담는다.
  언제 쓰는지: /asset/accounts 화면.
  쓰면 안 되는 경우: 연결 전 계좌 선택(AccountSelectRow).

  Figma 확정본 구조 — 로고 · 이름/부제 · (금액 + 상태) · `⋮` 더보기 4열.
  재동기화·연결 해제를 행에 늘어놓지 않고 더보기 안으로 접는다. 목록의 주인공은 잔액과 상태다.
  재연동이 필요한 행은 카드 배경으로 먼저 알린다 — 배지만으로는 목록에서 놓친다.

  AC_02_04(동기화 상태)·AC_02_06(재연동)을 별도 화면으로 만들지 않고 이 행에 흡수했다 —
  sync_status 가 계좌 단위 컬럼이라 목록 행이 제자리다. (DECISIONS.md 2026-08-04 계좌 연동)
-->
<script setup>
import { computed } from 'vue';
import AccountSyncBadge from '@/components/account/AccountSyncBadge.vue';
import InstitutionLogo from '@/components/account/InstitutionLogo.vue';
import { connectedRowFigure, consentExpiryLabel, needsReconnect } from '@/utils/account';

const props = defineProps({
    account: { type: Object, required: true },
});

defineEmits(['more']);

const expiryLabel = computed(() => consentExpiryLabel(props.account.expiresAt));
const alert = computed(() => needsReconnect(props.account.syncStatus));
</script>

<template>
    <div class="connected-row" :class="{ 'connected-row--alert': alert }">
        <InstitutionLogo :code="account.bankCode" :short-label="account.shortLabel" />

        <div class="connected-row__title">
            <p class="connected-row__bank">{{ account.bankName }}</p>
            <p class="connected-row__account">{{ account.accountName }}</p>
        </div>

        <div class="connected-row__figures">
            <!-- 카드는 잔액이 아니라 종류를 쓴다(#467) — 자산이 아니라 거래 출처라서다. -->
            <p class="connected-row__amount">{{ connectedRowFigure(account) }}</p>
            <AccountSyncBadge :sync-status="account.syncStatus" />
            <span v-if="expiryLabel" class="connected-row__expiry">{{ expiryLabel }}</span>
        </div>

        <button
            class="connected-row__more"
            type="button"
            :aria-label="`${account.bankName} ${account.accountType === 'CARD' ? '카드' : '계좌'} 관리`"
            @click="$emit('more', account)"
        >
            ⋮
        </button>
    </div>
</template>

<style scoped>
/* 확정본: 흰 카드 + 은은한 그림자, 한 줄 4열. */
.connected-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4);
    border-radius: var(--tt-radius-lg);
    background: var(--tt-bg);
    box-shadow: var(--tt-elevation-1);
}

/* 재연동이 필요한 계좌는 카드째로 물들여 목록에서 먼저 눈에 띄게 한다. */
.connected-row--alert {
    background: var(--tt-danger-subtle);
}

.connected-row__title {
    flex: 1;
    min-width: 0;
}

/*
 * 상품명은 길다("KB Star 입출금통장"). 좁은 폭에서 줄바꿈되면 로고·금액과 높이가 어긋나
 * 행이 무너지므로 한 줄로 고정하고 넘치면 말줄임한다. 전체 이름은 계좌 선택 화면에서 볼 수 있다.
 */
.connected-row__bank,
.connected-row__account {
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
}

.connected-row__bank {
    margin: 0;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.connected-row__account {
    margin: 2px 0 0;
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
}

.connected-row__figures {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: var(--tt-space-1);
    flex-shrink: 0;
}

/* 확정본: 금액은 모노 숫자에 단위 없음. */
.connected-row__amount {
    margin: 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.02em;
    color: var(--tt-text);
}

.connected-row__expiry {
    padding: 2px var(--tt-space-2);
    border-radius: var(--tt-radius-full);
    background: var(--tt-accent-subtle);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-accent-deep);
}

.connected-row__more {
    flex-shrink: 0;
    width: 28px;
    height: 28px;
    border: 0;
    border-radius: var(--tt-radius-full);
    background: none;
    font-size: var(--tt-fs-section);
    line-height: 1;
    color: var(--tt-text-muted);
    cursor: pointer;
}

.connected-row__more:hover {
    background: var(--tt-bg-subtle);
    color: var(--tt-text);
}
</style>
