<!--
  용도: 자산 홈에서 "입출금 계좌" 항목을 눌렀을 때 진입하는 상세 화면. 입출금 합계 + 계좌별 잔액을 보여준다.
  언제 쓰는지: router/index.js 의 /asset/checking 라우트. 백엔드 연동 전까지 api/asset.js 가 목업 데이터를 반환한다.
-->
<script setup>
import { onMounted, ref } from 'vue';
import { fetchCheckingAccountDetail } from '@/api/asset';
import AssetDetailHeader from '@/components/asset/AssetDetailHeader.vue';
import AssetTotalCard from '@/components/asset/AssetTotalCard.vue';
import AssetAccountRow from '@/components/asset/AssetAccountRow.vue';
import StateLoading from '@/components/common/StateLoading.vue';
import StateError from '@/components/common/StateError.vue';

const detail = ref(null);
const loading = ref(false);
const errorMessage = ref('');

async function load() {
    loading.value = true;
    errorMessage.value = '';
    try {
        detail.value = await fetchCheckingAccountDetail();
    } catch (err) {
        errorMessage.value = err.message ?? '입출금 계좌 정보를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

onMounted(load);
</script>

<template>
    <div class="asset-checking">
        <AssetDetailHeader title="입출금 계좌" />

        <StateLoading v-if="loading" message="입출금 계좌를 불러오는 중" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="load" />
        <template v-else-if="detail">
            <AssetTotalCard label="입출금 합계" :amount="detail.total">
                <template #meta>{{ detail.syncedLabel }}</template>
            </AssetTotalCard>

            <section class="asset-checking__list">
                <h2 class="asset-checking__title">계좌 목록</h2>
                <ul class="asset-checking__items">
                    <AssetAccountRow
                        v-for="account in detail.accounts"
                        :key="account.code"
                        :badge="account.badge"
                        :label="account.label"
                        :meta="account.meta"
                        :amount="account.amount"
                        :tone="account.tone"
                    />
                </ul>
            </section>
        </template>
    </div>
</template>

<style scoped>
.asset-checking {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.asset-checking__title {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.asset-checking__items {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}
</style>
