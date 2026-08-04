import { ref } from 'vue';
import { defineStore } from 'pinia';
import {
    fetchConsentCatalog,
    fetchMyConsents,
    submitConsents,
    withdrawConsent,
} from '@/api/consent';
import { useAuthStore } from '@/stores/auth';

/**
 * 동의 도메인 상태.
 *
 * 게이트 플래그(needsConsent)는 여기 두지 않는다 — 로그인·재발급 응답에 실려 오므로
 * auth 스토어가 계속 소유한다. 저장·철회 응답으로 그 값을 갱신해준다.
 */
export const useConsentStore = defineStore('consent', () => {
    const catalog = ref(null);
    const myConsents = ref([]);
    const isLoading = ref(false);

    async function loadCatalog(scope) {
        isLoading.value = true;
        try {
            catalog.value = await fetchConsentCatalog(scope);
        } finally {
            isLoading.value = false;
        }
    }

    async function save(scope, agreements) {
        const result = await submitConsents(scope, agreements);
        useAuthStore().needsConsent = result.needsConsent;
        return result;
    }

    async function loadMyConsents() {
        isLoading.value = true;
        try {
            myConsents.value = (await fetchMyConsents()).items;
        } finally {
            isLoading.value = false;
        }
    }

    async function withdraw(type) {
        const result = await withdrawConsent(type);
        useAuthStore().needsConsent = result.needsConsent;
        await loadMyConsents();
        return result;
    }

    return { catalog, myConsents, isLoading, loadCatalog, save, loadMyConsents, withdraw };
});
