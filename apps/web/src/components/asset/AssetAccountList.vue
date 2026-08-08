<!--
  용도: 자산 홈 화면의 계좌 유형별 목록. 이니셜 배지 · 이름 · 개수 · 금액을 한 행에 보여준다.
  언제 쓰는지: AssetHomeView 하단 한 곳. 대출처럼 부채인 계좌는 amount 가 음수로 들어와 빨간색으로 표시된다.
-->
<script setup>
import { useRouter } from 'vue-router';
import { formatAssetHomeWon, toneColor } from '@/utils/asset';

defineProps({
    accounts: { type: Array, required: true },
});

const router = useRouter();

const DETAIL_ROUTE_NAMES = {
    checking: 'assetChecking',
    savings: 'assetSavings',
    investment: 'assetInvestment',
    loan: 'assetLoan',
};

function goToDetail(account) {
    const routeName = DETAIL_ROUTE_NAMES[account.code];
    if (routeName) {
        router.push({ name: routeName });
    }
}
</script>

<template>
    <section class="asset-accounts">
        <h2 class="asset-accounts__title">자산 목록</h2>
        <ul class="asset-accounts__list">
            <li v-for="account in accounts" :key="account.code">
                <button type="button" class="asset-accounts__item" @click="goToDetail(account)">
                    <span
                        class="asset-accounts__avatar"
                        :style="{ background: toneColor(account.tone) }"
                        aria-hidden="true"
                    >
                        {{ account.badge }}
                    </span>
                    <div class="asset-accounts__info">
                        <p class="asset-accounts__name">{{ account.label }}</p>
                        <p class="asset-accounts__count">{{ account.count }}개</p>
                    </div>
                    <p
                        class="asset-accounts__amount"
                        :class="{ 'asset-accounts__amount--negative': account.amount < 0 }"
                    >
                        {{ formatAssetHomeWon(account.amount) }}
                    </p>
                </button>
            </li>
        </ul>
    </section>
</template>

<style scoped>
.asset-accounts__title {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.asset-accounts__list {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.asset-accounts__item {
    display: flex;
    align-items: center;
    width: 100%;
    gap: var(--tt-space-3);
    padding: var(--tt-space-4);
    font: inherit;
    text-align: left;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-md);
    cursor: pointer;
    transition:
        box-shadow 0.15s ease,
        border-color 0.15s ease;
}

.asset-accounts__item:hover {
    border-color: var(--tt-border-strong);
    box-shadow: var(--tt-elevation-2);
}

.asset-accounts__avatar {
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-inverse);
    border-radius: var(--tt-radius-full);
}

.asset-accounts__info {
    flex: 1;
}

.asset-accounts__name {
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.asset-accounts__count {
    margin-top: 2px;
    font-size: var(--tt-fs-mono-chip);
    color: var(--tt-text-muted);
}

.asset-accounts__amount {
    font-family: var(--tt-font-mono);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.asset-accounts__amount--negative {
    color: var(--tt-danger);
}
</style>
