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
 * 게이트 플래그(needsConsent · needsFinancialConsent · needsAccountLink)는 여기 두지 않는다 —
 * 로그인·재발급 응답에 실려 오므로 auth 스토어가 계속 소유한다. 저장·철회 응답으로 그 값을 갱신해준다.
 * 응답에 실려 온 플래그만 반영된다(auth 스토어의 applyOnboardingFlags 참고).
 */
export const useConsentStore = defineStore('consent', () => {
    const catalog = ref(null);
    const myConsents = ref([]);
    const isLoading = ref(false);

    async function loadCatalog(scope) {
        // 실패 시 다른 scope 의 낡은 카탈로그가 남아 잘못된 약관이 렌더링되는 것을 막는다.
        catalog.value = null;
        isLoading.value = true;
        try {
            catalog.value = await fetchConsentCatalog(scope);
        } finally {
            isLoading.value = false;
        }
    }

    /**
     * scope 단위 동의 저장.
     *
     * 응답의 게이트 플래그를 **전부** 반영한다. 예전에는 needsConsent 만 반영해서,
     * FINANCIAL 저장 직후에도 needsFinancialConsent 가 true 로 남아 라우터 가드가
     * 방금 동의한 화면으로 다시 돌려보냈다.
     */
    async function save(scope, agreements) {
        const result = await submitConsents(scope, agreements);
        useAuthStore().applyOnboardingFlags(result);
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

    /**
     * 철회한 항목을 다시 켠다. 전용 엔드포인트 없이 scope 단위 저장(POST /consents)을 쓴다.
     * agreements 는 같은 scope 전량이어야 한다 — buildAgreeAgainPayload 가 만들어 준다.
     */
    async function agreeAgain(scope, agreements) {
        const result = await save(scope, agreements);
        try {
            await loadMyConsents();
        } catch {
            // 서버 저장은 이미 성공했다. 목록 갱신 실패로 작업 전체를 실패로 보고하지 않는다.
        }
        return result;
    }

    async function withdraw(type) {
        const result = await withdrawConsent(type);
        useAuthStore().applyOnboardingFlags(result);
        try {
            await loadMyConsents();
        } catch {
            // 서버 철회는 이미 성공했으므로 목록 갱신 실패로 작업 전체를 실패로 보고하지 않는다. 다음 조회 때 최신 상태가 반영된다.
        }
        return result;
    }

    return {
        catalog,
        myConsents,
        isLoading,
        loadCatalog,
        save,
        loadMyConsents,
        agreeAgain,
        withdraw,
    };
});
