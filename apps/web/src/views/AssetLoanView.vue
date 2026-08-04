<!--
  용도: 자산 홈에서 "대출" 항목을 눌렀을 때 진입하는 상세 화면. 대출 합계 + 상품별 잔액·금리·만기를 보여준다.
  언제 쓰는지: router/index.js 의 /asset/loan 라우트. 백엔드 연동 전까지 api/asset.js 가 목업 데이터를 반환한다.
-->
<script setup>
import { onMounted, ref } from 'vue';
import { fetchLoanAccountDetail } from '@/api/asset';
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
        detail.value = await fetchLoanAccountDetail();
    } catch (err) {
        errorMessage.value = err.message ?? '대출 정보를 불러오지 못했습니다.';
    } finally {
        loading.value = false;
    }
}

onMounted(load);
</script>

<template>
    <div class="asset-loan">
        <AssetDetailHeader title="대출" />

        <StateLoading v-if="loading" message="대출 정보를 불러오는 중" />
        <StateError v-else-if="errorMessage" :message="errorMessage" @retry="load" />
        <template v-else-if="detail">
            <AssetTotalCard label="대출 합계" :amount="detail.total" />

            <section class="asset-loan__list">
                <h2 class="asset-loan__title">대출 목록</h2>
                <ul class="asset-loan__items">
                    <AssetAccountRow
                        v-for="loan in detail.loans"
                        :key="loan.code"
                        :badge="loan.badge"
                        :label="loan.label"
                        :meta="loan.meta"
                        :amount="-loan.amount"
                        :tone="loan.tone"
                    />
                </ul>
            </section>
        </template>
    </div>
</template>

<style scoped>
.asset-loan {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-4);
    padding: var(--tt-space-5);
    background: var(--tt-bg-subtle);
}

.asset-loan__title {
    margin-bottom: var(--tt-space-3);
    font-size: var(--tt-fs-section);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text);
}

.asset-loan__items {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}
</style>
