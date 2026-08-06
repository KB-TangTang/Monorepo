import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useAccountStore } from '@/stores/account';
import { canEnterLinkStep } from '@/utils/account';
import personalMissionChallengeRoutes from './personalMissionChallengeRoutes';

/*
 * 하단 5탭 구조: 재판 · 자산 · 홈 · 자료실 · 마이 (TheTabBar.vue 의 TABS 와 짝을 이룬다).
 * meta.public   — 로그인 없이 접근 가능
 * meta.hideTabBar — 하단 탭바를 숨긴다 (App.vue 가 읽는다)
 */
const routes = [
    {
        path: '/login',
        name: 'login',
        component: () => import('@/views/auth/LoginView.vue'),
        meta: { title: '로그인', public: true, hideTabBar: true },
    },
    {
        path: '/auth/callback',
        name: 'authCallback',
        component: () => import('@/views/auth/AuthCallbackView.vue'),
        meta: { title: '로그인 처리 중', public: true, hideTabBar: true },
    },
    {
        path: '/consent',
        name: 'consent',
        component: () => import('@/views/consent/ServiceConsentView.vue'),
        meta: { title: '서비스 동의', hideTabBar: true },
    },
    {
        path: '/consent/financial',
        name: 'financialConsent',
        component: () => import('@/views/consent/FinancialConsentView.vue'),
        meta: { title: '금융데이터 수집 동의', hideTabBar: true },
    },
    {
        path: '/',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '홈' },
    },
    ...personalMissionChallengeRoutes,
    {
        path: '/asset',
        name: 'asset',
        component: () => import('@/views/AssetHomeView.vue'),
        meta: { title: '자산' },
    },
    {
        path: '/asset/checking',
        name: 'assetChecking',
        component: () => import('@/views/AssetCheckingView.vue'),
        meta: { title: '입출금 계좌' },
    },
    {
        path: '/asset/savings',
        name: 'assetSavings',
        component: () => import('@/views/AssetSavingsView.vue'),
        meta: { title: '예적금' },
    },
    {
        path: '/asset/investment',
        name: 'assetInvestment',
        component: () => import('@/views/AssetInvestmentView.vue'),
        meta: { title: '투자증권' },
    },
    {
        path: '/asset/loan',
        name: 'assetLoan',
        component: () => import('@/views/AssetLoanView.vue'),
        meta: { title: '대출' },
    },
    {
        path: '/asset/trend',
        name: 'assetNetWorthTrend',
        component: () => import('@/views/NetWorthTrendView.vue'),
        meta: { title: '순자산 추이' },
    },
    {
        path: '/ledger',
        name: 'ledger',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '장부' },
    },
    {
        path: '/my',
        name: 'my',
        component: () => import('@/views/MyPageView.vue'),
        meta: { title: '마이' },
    },
    // ↓ 그룹 챌린지 (#36). personalMissionChallengeRoutes 의 목업을 대체한다. 지우지 말 것.
    {
        path: '/group-challenges',
        name: 'groupChallenge',
        component: () => import('@/views/challenge/group/GroupChallengeHomeView.vue'),
        meta: { title: '그룹 챌린지' },
    },
    {
        path: '/group-challenges/create',
        name: 'groupChallengeCreate',
        component: () => import('@/views/challenge/group/GroupCreateView.vue'),
        meta: { title: '그룹 만들기', hideTabBar: true },
    },
    {
        path: '/group-challenges/invite/:groupId',
        name: 'groupChallengeInvite',
        component: () => import('@/views/challenge/group/GroupInviteView.vue'),
        meta: { title: '친구 초대', hideTabBar: true },
    },
    {
        path: '/group-challenges/join/:code?',
        name: 'groupChallengeJoin',
        component: () => import('@/views/challenge/group/GroupJoinView.vue'),
        meta: { title: '그룹 참여', hideTabBar: true },
    },
    // ↓ 팀원(#10 챌린지 리포트) 라우트. 지우지 말 것.
    {
        path: '/reports/challenge',
        name: 'challengeReport',
        component: () => import('@/views/challenge/report/ChallengeReportView.vue'),
        meta: { title: '챌린지 리포트' },
    },
    {
        path: '/reports/monthly',
        name: 'monthlyConsumptionReport',
        component: () => import('@/views/report/MonthlyConsumptionReportView.vue'),
        meta: { title: '월간 판결문' },
    },
    {
        path: '/reports/challenge/savings',
        name: 'challengeNetSavings',
        component: () => import('@/views/challenge/report/ChallengeNetSavingsView.vue'),
        meta: { title: '카테고리별 순 절감액' },
    },
    {
        path: '/asset/fixed-expenses/savings',
        name: 'fixedExpenseSavings',
        component: () => import('@/views/fixed-expense/FixedExpenseSavingsView.vue'),
        meta: { title: '절약 감정서' },
    },
    {
        path: '/asset/fixed-expenses',
        name: 'fixedExpenseManagement',
        component: () => import('@/views/fixed-expense/FixedExpenseManagementView.vue'),
        meta: { title: '고정지출 관리' },
    },
    {
        path: '/asset/fixed-expenses/candidates/:candidateId',
        name: 'fixedExpenseCandidate',
        component: () => import('@/views/fixed-expense/FixedExpenseCandidateView.vue'),
        meta: { title: '탐지 후보 확인' },
    },
    {
        path: '/asset/fixed-expenses/:expenseId',
        name: 'fixedExpenseDetail',
        component: () => import('@/views/fixed-expense/FixedExpenseDetailView.vue'),
        meta: { title: '고정지출 상세' },
    },
    // ↓ 계좌 연동(이슈 #12). 단계 순서는 stores/account.js 가 utils/account.js 의 LINK_STEPS 로 결정한다.
    {
        path: '/accounts/link/institutions',
        name: 'accountLinkInstitutions',
        component: () => import('@/views/account/InstitutionSelectView.vue'),
        meta: { title: '금융기관 연결', hideTabBar: true },
    },
    {
        path: '/accounts/link/auth',
        name: 'accountLinkAuth',
        component: () => import('@/views/account/AuthStepView.vue'),
        meta: { title: '본인 인증', hideTabBar: true, linkStep: 'auth' },
    },
    {
        path: '/accounts/link/progress',
        name: 'accountLinkProgress',
        component: () => import('@/views/account/LinkProgressView.vue'),
        meta: { title: '자산 연결 중', hideTabBar: true, linkStep: 'progress' },
    },
    {
        path: '/accounts/link/select',
        name: 'accountLinkSelect',
        component: () => import('@/views/account/AccountSelectView.vue'),
        meta: { title: '연결할 계좌 선택', hideTabBar: true, linkStep: 'select' },
    },
    {
        path: '/accounts/link/done',
        name: 'accountLinkDone',
        component: () => import('@/views/account/LinkDoneView.vue'),
        meta: { title: '계좌 연결 완료', hideTabBar: true, linkStep: 'done' },
    },
    {
        path: '/asset/accounts',
        name: 'connectedAccounts',
        component: () => import('@/views/account/ConnectedAccountView.vue'),
        meta: { title: '연결 계좌 관리' },
    },
    {
        path: '/asset/accounts/:accountId/reconnect',
        name: 'accountReconnect',
        component: () => import('@/views/account/AccountReconnectView.vue'),
        meta: { title: '계좌 재연동', hideTabBar: true },
    },
    {
        path: '/asset/accounts/refresh',
        name: 'accountRefresh',
        component: () => import('@/views/account/AccountRefreshView.vue'),
        meta: { title: '계좌 즉시 조회' },
    },
];

/* 개발용 컴포넌트 카탈로그. import.meta.env.DEV 가 false 인 프로덕션 빌드에서는
 * 이 블록째로 제거돼 라우트도 청크도 생기지 않는다.
 * 사용자 데이터를 다루지 않으므로 로그인 없이 연다. */
if (import.meta.env.DEV) {
    routes.push({
        path: '/dev/ui',
        name: 'devUi',
        component: () => import('@/views/dev/UiCatalogView.vue'),
        meta: { title: '컴포넌트 카탈로그', public: true, hideTabBar: true },
    });
}

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
});

/*
 * 로그인 가드. meta.public 이 아닌 모든 화면은 로그인이 필요하다.
 * 5탭은 전부 개인 금융 데이터라 예외를 두지 않는다.
 * 개발용 인증 우회 플래그는 만들지 않는다 — 인증 버그를 가리고 운영 설정에 새어나간다.
 */
router.beforeEach((to) => {
    const auth = useAuthStore();

    if (to.meta.public) {
        // 이미 로그인한 사용자가 로그인 화면으로 오면 홈으로 보낸다
        if (auth.isLoggedIn && to.name === 'login') {
            return { name: 'home' };
        }
        return true;
    }

    if (!auth.isLoggedIn) {
        return { name: 'login', query: { redirect: to.fullPath } };
    }

    /*
     * 필수 동의를 마치지 않은 사용자는 동의 화면에 묶어둔다.
     * 계좌 연동용 CODEF 동의(/consent/financial)는 이 게이트 밖이다 —
     * SIGNUP 동의를 이미 마친 사용자만 도달하는 화면이라 여기서 막으면 순환한다.
     */
    if (auth.needsConsent && to.name !== 'consent') {
        return { name: 'consent' };
    }

    /*
     * 계좌 연결 플로우 중간 단계 직접 진입 차단 (이슈 #12).
     * 북마크·새로고침으로 /accounts/link/select 에 바로 들어오면 이전 단계 결과가 없어 빈 화면이 된다.
     * meta.linkStep 이 붙은 라우트만 검사하므로 다른 화면에는 영향이 없다.
     */
    if (to.meta.linkStep) {
        const account = useAccountStore();
        const canEnter = canEnterLinkStep(to.meta.linkStep, {
            selectedCount: account.selectedCount,
            hasConnection: account.hasConnection,
            linkedCount: account.linkedCount,
            progressDone: account.progressDone,
        });
        if (!canEnter) {
            return { name: 'accountLinkInstitutions' };
        }
    }

    return true;
});

export default router;
