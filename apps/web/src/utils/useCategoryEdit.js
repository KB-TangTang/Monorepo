import { ref } from 'vue';
import { fetchCategories } from '@/api/category';
import { updateTransactionCategory } from '@/api/ledger';
import { resolveCategoryId } from '@/utils/category';

/**
 * 거래 카테고리 수동 변경 composable. LedgerView·LedgerMonthTransactionsView 두 화면이 공유한다.
 *
 * @param {import('vue').Ref<Array>} transactionsRef — 화면이 들고 있는 거래 목록 ref.
 *   성공 시 이 배열 안에서 해당 거래 하나만 찾아 category를 갱신한다(가맹점 일괄 적용을
 *   켜도 다른 거래는 그대로 둔다 — 백엔드가 이후 거래에만 적용하고 기존 거래는 소급
 *   재분류하지 않기 때문).
 * @returns {{
 *   categories: import('vue').Ref<Array>,
 *   isApplyingCategory: import('vue').Ref<boolean>,
 *   categoryError: import('vue').Ref<string>,
 *   loadCategories: () => Promise<void>,
 *   applyCategory: (payload: {transactionId: number, categoryName: string, applyToMerchant: boolean}) => Promise<boolean>
 * }}
 */
export function useCategoryEdit(transactionsRef) {
    const categories = ref([]);
    const isApplyingCategory = ref(false);
    const categoryError = ref('');

    async function loadCategories() {
        try {
            categories.value = await fetchCategories();
        } catch {
            categories.value = [];
        }
    }

    async function applyCategory({ transactionId, categoryName, applyToMerchant }) {
        const categoryId = resolveCategoryId(categories.value, categoryName);
        if (categoryId === null) {
            categoryError.value = '알 수 없는 카테고리입니다.';
            return false;
        }
        isApplyingCategory.value = true;
        categoryError.value = '';
        try {
            await updateTransactionCategory(transactionId, { categoryId, applyToMerchant });
            const tx = transactionsRef.value.find((item) => item.id === transactionId);
            if (tx) {
                tx.category = categoryName;
            }
            return true;
        } catch (err) {
            categoryError.value = err.message ?? '카테고리 변경에 실패했습니다.';
            return false;
        } finally {
            isApplyingCategory.value = false;
        }
    }

    return { categories, isApplyingCategory, categoryError, loadCategories, applyCategory };
}
