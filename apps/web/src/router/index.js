import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
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
        path: '/',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '홈' },
    },
    ...personalMissionChallengeRoutes,
    {
        path: '/asset',
        name: 'asset',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '자산' },
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
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '마이' },
    },
    // ↓ 팀원(#10 챌린지 리포트) 라우트. 지우지 말 것.
    {
        path: '/reports/challenge',
        name: 'challengeReport',
        component: () => import('@/views/challenge/report/ChallengeReportView.vue'),
        meta: { title: '챌린지 리포트' },
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

    return true;
});

export default router;
