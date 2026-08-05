<!--
  용도: 조회된 계좌 중 연결 대상을 고르는 체크 행 (AC_01_03).
  언제 쓰는지: 연결할 계좌 선택 화면.
  쓰면 안 되는 경우: 이미 연결된 계좌 관리 목록(ConnectedAccountRow).

  이미 연결된 계좌는 alreadyLinked=true 로 오며 체크가 잠기고 `이미 연결됨` 으로 표시된다
  (명세 AC_01_03: 중복 계좌는 재등록하지 않는다). 계좌번호는 서버에서 마스킹된 값이 온다.
-->
<script setup>
import { formatAmount } from '@/utils/account';

const props = defineProps({
    account: { type: Object, required: true },
    selected: { type: Boolean, default: false },
});

const emit = defineEmits(['toggle']);

function onClick() {
    if (props.account.alreadyLinked) {
        return;
    }
    emit('toggle', props.account.accountId);
}
</script>

<template>
    <button
        class="account-row"
        :class="{ 'account-row--locked': account.alreadyLinked }"
        type="button"
        :disabled="account.alreadyLinked"
        :aria-pressed="account.alreadyLinked ? undefined : selected"
        @click="onClick"
    >
        <span
            class="account-row__check"
            :class="{ 'account-row__check--on': selected || account.alreadyLinked }"
            aria-hidden="true"
        >
            ✓
        </span>

        <span class="account-row__body">
            <span class="account-row__name">{{ account.accountName }}</span>
            <span class="account-row__sub">
                {{ account.alreadyLinked ? '이미 연결됨' : account.accountNoMasked }}
            </span>
        </span>

        <span class="account-row__amount">{{ formatAmount(account.balance) }}</span>
    </button>
</template>

<style scoped>
.account-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    width: 100%;
    padding: var(--tt-space-4);
    border: 0;
    border-bottom: 1px solid var(--tt-border);
    background: var(--tt-bg);
    text-align: left;
    cursor: pointer;
}

.account-row:last-child {
    border-bottom: 0;
}

.account-row--locked {
    background: var(--tt-bg-subtle);
    cursor: not-allowed;
}

.account-row__check {
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 24px;
    height: 24px;
    border: 1px solid var(--tt-border-strong);
    border-radius: var(--tt-radius-sm);
    background: var(--tt-bg);
    color: transparent;
    font-size: var(--tt-fs-caption);
}

.account-row__check--on {
    border-color: var(--tt-primary);
    background: var(--tt-primary);
    color: var(--tt-text-inverse);
}

/* 원시 팔레트(--tt-gray-*)는 컴포넌트에서 쓰지 않는다 (DESIGN_SYSTEM.md 절대 규칙 2). */
.account-row--locked .account-row__check--on {
    border-color: var(--tt-border-strong);
    background: var(--tt-border-strong);
}

.account-row__body {
    display: flex;
    flex-direction: column;
    gap: 2px;
    flex: 1;
    min-width: 0;
}

/* 상품명이 길어도 행 높이가 흔들리지 않게 한 줄 + 말줄임 (320px 폭 기준). */
.account-row__name {
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.account-row--locked .account-row__name {
    color: var(--tt-text-subtle);
}

/* 계좌번호는 코드성 문자열이라 mono-chip 조합을 쓴다 (DESIGN_SYSTEM.md 타이포 표). */
.account-row__sub {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

/*
 * 참고화면 0-5: 금액은 모노 숫자에 단위 없음. 자릿수가 세로로 맞아 비교하기 쉽다.
 * 모노는 글자폭이 넓어 section(18px)보다 한 단계 작게 잡아야 행이 넘치지 않는다.
 */
.account-row__amount {
    flex-shrink: 0;
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-bold);
    letter-spacing: 0.02em;
    color: var(--tt-text);
}

.account-row--locked .account-row__amount {
    color: var(--tt-text-subtle);
}
</style>
