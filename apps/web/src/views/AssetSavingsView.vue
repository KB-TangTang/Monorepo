<!--
  용도: 자산 홈에서 "예금·적금" 항목을 눌렀을 때 진입하는 상세 화면. 예적금 합계 + 상품별 만기·잔액을 보여준다.
  언제 쓰는지: router/index.js 의 /asset/savings 라우트. 백엔드 연동 전까지 api/asset.js 가 목업 데이터를 반환한다.
-->
<script setup>
import { onMounted, ref } from 'vue';
import { fetchSavingsAccountDetail } from '@/api/asset';
import BaseBackHeader from '@/components/common/BaseBackHeader.vue';
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
        detail.value = await fetchSavingsAccountDetail();
    } catch (err) {
        errorMessage.value = err.message ?? '예적금 정보를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

onMounted(load);
</script>

<template>
    <div class="asset-savings">
        <BaseBackHeader title="예적금" back-label="자산 홈으로 돌아가기" />

        <StateLoading v-if="loading" message="예적금 정보를 불러오는 중" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="load" />
        <template v-else-if="detail">
            <AssetTotalCard label="예적금 합계" :amount="detail.total">
                <template #meta>{{ detail.syncedLabel }}</template>
            </AssetTotalCard>

            <section class="asset-savings__list">
                <h2 class="asset-savings__title">상품 목록</h2>
                <ul class="asset-savings__items">
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
.asset-savings {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    min-height: calc(100vh - var(--tt-tabbar-height));
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.asset-savings__title {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.asset-savings__items {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}
</style>
