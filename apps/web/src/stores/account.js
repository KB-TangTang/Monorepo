import { computed, ref } from 'vue';
import { defineStore } from 'pinia';
import router from '@/router';
import {
    createConnection,
    disconnectAccount,
    fetchAuthMethods,
    fetchAuthStatus,
    fetchConnectedAccounts,
    fetchInstitutions,
    fetchLinkProgress,
    fetchLinkableAccounts,
    linkAccounts,
    refreshAccounts,
    requestSimpleAuth,
    resyncAccount,
    submitExtraAuth,
} from '@/api/account';
import {
    LINK_STEP_ROUTES,
    calcLinkProgress,
    nextLinkStep,
    prevLinkStep,
    resolveAuthView,
} from '@/utils/account';

/**
 * 계좌 도메인 스토어 (이슈 #12). 도메인당 1개 규칙이라 연결 플로우와 연결 계좌 목록을 함께 둔다.
 *
 * 단계 이동은 여기 goNextStep()/goPrevStep() 으로만 한다. **뷰가 서로를 router.push 로 직접 부르지 않는다.**
 * 그래야 인증 단계가 빠지거나 순서가 바뀔 때 utils/account.js 의 LINK_STEPS 한 줄 수정으로 끝난다.
 *
 * ⚠ 기관 로그인 자격증명(아이디·비밀번호)은 **이 스토어에 절대 저장하지 않는다.**
 * connect() 의 인자로만 흐르고 호출이 끝나면 사라진다.
 */
export const useAccountStore = defineStore('account', () => {
    /* --- 연결 플로우 --- */
    const institutions = ref(null);
    const selectedInstitutions = ref([]);
    const connectionId = ref('');
    const needsExtraAuth = ref(false);
    const extraAuthType = ref(null);
    const linkableGroups = ref([]);
    const selectedAccountIds = ref([]);
    const linkedCount = ref(0);

    /* --- 인증 (수단 무관) --- */
    const authMethods = ref([]);
    const authStatus = ref('IDLE');

    /* --- 조회 진행 --- */
    const progressInstitutions = ref([]);
    const progressDone = ref(false);

    /* --- 연결 계좌 관리 --- */
    const connectedAccounts = ref([]);
    const nextAutoSyncAt = ref(null);

    /* --- 즉시 조회 --- */
    const refreshResult = ref(null);

    /* --- 공통 --- */
    const loading = ref(false);
    const error = ref('');

    const selectedCount = computed(() => selectedInstitutions.value.length);
    const selectedAccountCount = computed(() => selectedAccountIds.value.length);
    const hasConnection = computed(() => connectionId.value !== '');
    const linkableAccountCount = computed(() =>
        linkableGroups.value.reduce((sum, group) => sum + group.accounts.length, 0),
    );
    /**
     * 아직 연결하지 않아 **실제로 고를 수 있는** 계좌 수.
     * 조회된 계좌가 있어도 전부 이미 연결돼 있으면 사용자가 할 수 있는 일이 없다 —
     * 화면이 그 사실을 말하고 다음 행동을 줘야 한다.
     */
    const selectableAccountCount = computed(() =>
        linkableGroups.value.reduce(
            (sum, group) => sum + group.accounts.filter((item) => !item.alreadyLinked).length,
            0,
        ),
    );
    /** 어떤 인증 화면을 그릴지. 서버가 내려준 목록만 보고 정한다. */
    const authView = computed(() => resolveAuthView(authMethods.value));
    const progressPercent = computed(() => calcLinkProgress(progressInstitutions.value));

    /** 단계 이동. 라우트 이름은 utils 의 매핑에서만 나온다. */
    function goStep(step) {
        const name = LINK_STEP_ROUTES[step];
        if (name) {
            router.push({ name });
        }
    }

    function goNextStep(current) {
        const step = nextLinkStep(current);
        if (step) {
            goStep(step);
        }
    }

    /**
     * 플로우를 처음부터 다시 시작한다.
     *
     * ⚠ 계좌 선택에서 "뒤로"는 이전 단계(progress)로 갈 수 없다 —
     * 진행 화면은 진입하자마자 "이미 완료"를 확인하고 계좌 선택으로 되돌려보내
     * 왕복만 하고 제자리가 된다. 인증·조회를 다시 해야 하므로 1단계로 보내는 게 맞다.
     */
    function restartFlow() {
        resetFlow();
        goStep('institutions');
    }

    function goPrevStep(current) {
        const step = prevLinkStep(current);
        if (step) {
            goStep(step);
        } else {
            router.back();
        }
    }

    async function run(task) {
        loading.value = true;
        error.value = '';
        try {
            return await task();
        } catch (err) {
            error.value = err.message;
            throw err;
        } finally {
            loading.value = false;
        }
    }

    /* --- 1단계: 기관 선택 --- */

    async function loadInstitutions() {
        return run(async () => {
            institutions.value = await fetchInstitutions();
            return institutions.value;
        });
    }

    function toggleInstitution(code) {
        const index = selectedInstitutions.value.indexOf(code);
        if (index === -1) {
            selectedInstitutions.value.push(code);
        } else {
            selectedInstitutions.value.splice(index, 1);
        }
    }

    function isInstitutionSelected(code) {
        return selectedInstitutions.value.includes(code);
    }

    /* --- 2단계: 인증 (간편인증 / 기관 로그인) --- */

    async function loadAuthMethods() {
        return run(async () => {
            const result = await fetchAuthMethods();
            authMethods.value = result.methods;
            return authMethods.value;
        });
    }

    /**
     * 간편인증 요청.
     *
     * ⚠ **개인정보는 서버가 필요하다고 할 때만 보낸다.**
     * `auth-methods` 응답의 `requiresIdentity` 가 그 판단을 한다 —
     * 목 모드에는 전달할 인증기관이 없어 false 이고, 그때 개인정보는 화면 밖으로 나가지 않는다.
     * 프론트가 목/실을 판단하지 않는다는 원칙은 여기서도 같다. (DECISIONS.md 2026-08-05)
     *
     * @param {string} provider 인증 수단 코드
     * @param {{userName, birthDate, carrier, phoneNo}} [identity] 화면이 들고 있는 본인 정보
     */
    async function requestAuth(provider, identity) {
        return run(async () => {
            const method = authMethods.value.find((item) => item.type === 'SIMPLE_AUTH');
            const result = await requestSimpleAuth({
                provider,
                organizations: selectedInstitutions.value,
                ...(method?.requiresIdentity && identity ? identity : {}),
            });
            connectionId.value = result.connectionId;
            authStatus.value = result.status;
            return result;
        });
    }

    /**
     * 폴링 1회.
     * ⚠ 타이머는 **뷰가 소유한다.** 스토어에 setInterval 핸들을 두면 화면을 떠나도 살아남아 누수가 된다.
     * loading 을 건드리지 않는 것도 의도적이다 — 1초마다 스피너가 깜빡이면 화면이 떨린다.
     */
    async function checkAuthStatus() {
        const result = await fetchAuthStatus(connectionId.value);
        authStatus.value = result.status;
        return result.status;
    }

    async function checkProgress() {
        const result = await fetchLinkProgress(connectionId.value);
        progressInstitutions.value = result.institutions;
        progressDone.value = result.done;
        return result;
    }

    /**
     * @param {Array<{organization, loginType, id, password}>} credentials
     * 자격증명은 여기서 인자로만 쓰이고 스토어에 남지 않는다.
     */
    async function connect(credentials) {
        return run(async () => {
            const result = await createConnection(credentials);
            connectionId.value = result.connectionId;
            needsExtraAuth.value = result.needsExtraAuth;
            extraAuthType.value = result.extraAuthType;
            return result;
        });
    }

    async function verifyExtraAuth(payload) {
        return run(async () => {
            const result = await submitExtraAuth(connectionId.value, payload);
            needsExtraAuth.value = result.needsExtraAuth;
            extraAuthType.value = result.extraAuthType;
            return result;
        });
    }

    /* --- 3단계: 계좌 선택 --- */

    async function loadLinkableAccounts() {
        return run(async () => {
            linkableGroups.value = await fetchLinkableAccounts(connectionId.value);
            /* 이미 연결된 계좌는 기본 선택에서 뺀다 — 중복 등록을 막는다(AC_01_03). */
            selectedAccountIds.value = linkableGroups.value
                .flatMap((group) => group.accounts)
                .filter((account) => !account.alreadyLinked)
                .map((account) => account.accountId);
            return linkableGroups.value;
        });
    }

    function toggleAccount(accountId) {
        const index = selectedAccountIds.value.indexOf(accountId);
        if (index === -1) {
            selectedAccountIds.value.push(accountId);
        } else {
            selectedAccountIds.value.splice(index, 1);
        }
    }

    function isAccountSelected(accountId) {
        return selectedAccountIds.value.includes(accountId);
    }

    async function submitLink() {
        return run(async () => {
            const result = await linkAccounts(connectionId.value, selectedAccountIds.value);
            linkedCount.value = result.linkedCount;
            return result;
        });
    }

    /* --- 연결 계좌 관리 --- */

    async function loadConnectedAccounts() {
        return run(async () => {
            const result = await fetchConnectedAccounts();
            connectedAccounts.value = result.accounts;
            nextAutoSyncAt.value = result.nextAutoSyncAt;
            return result;
        });
    }

    async function disconnect(accountId) {
        return run(async () => {
            await disconnectAccount(accountId);
            connectedAccounts.value = connectedAccounts.value.filter(
                (account) => account.accountId !== accountId,
            );
        });
    }

    async function resync(accountId) {
        return run(async () => {
            const result = await resyncAccount(accountId);
            const target = connectedAccounts.value.find(
                (account) => account.accountId === accountId,
            );
            if (target) {
                target.syncStatus = result.syncStatus;
                target.lastSyncAt = result.lastSyncAt;
                target.syncFailReason = null;
            }
            return result;
        });
    }

    /* --- 즉시 조회 --- */

    async function refresh() {
        return run(async () => {
            refreshResult.value = await refreshAccounts();
            return refreshResult.value;
        });
    }

    /** 즉시 조회 화면을 다시 열 때. 직전 결과가 새 결과인 것처럼 보이지 않게 한다. */
    function clearRefreshResult() {
        refreshResult.value = null;
    }

    /** 플로우를 처음부터 다시 시작할 때. 완료 화면을 벗어나거나 중간 진입을 막을 때 쓴다. */
    function resetFlow() {
        selectedInstitutions.value = [];
        connectionId.value = '';
        needsExtraAuth.value = false;
        extraAuthType.value = null;
        linkableGroups.value = [];
        selectedAccountIds.value = [];
        linkedCount.value = 0;
        authMethods.value = [];
        authStatus.value = 'IDLE';
        progressInstitutions.value = [];
        progressDone.value = false;
        error.value = '';
    }

    return {
        institutions,
        selectedInstitutions,
        connectionId,
        needsExtraAuth,
        extraAuthType,
        linkableGroups,
        selectedAccountIds,
        linkedCount,
        authMethods,
        authStatus,
        progressInstitutions,
        progressDone,
        connectedAccounts,
        nextAutoSyncAt,
        refreshResult,
        loading,
        error,
        selectedCount,
        selectedAccountCount,
        hasConnection,
        linkableAccountCount,
        selectableAccountCount,
        authView,
        progressPercent,
        goStep,
        goNextStep,
        goPrevStep,
        restartFlow,
        loadInstitutions,
        toggleInstitution,
        isInstitutionSelected,
        loadAuthMethods,
        requestAuth,
        checkAuthStatus,
        checkProgress,
        connect,
        verifyExtraAuth,
        loadLinkableAccounts,
        toggleAccount,
        isAccountSelected,
        submitLink,
        loadConnectedAccounts,
        disconnect,
        resync,
        refresh,
        clearRefreshResult,
        resetFlow,
    };
});
