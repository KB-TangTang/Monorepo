import { createRouter, createWebHistory } from 'vue-router';

/*
 * 하단 5탭 구조: 재판 · 자산 · 홈 · 자료실 · 마이 (TheTabBar.vue 의 TABS 와 짝을 이룬다).
 * 각 화면 담당자는 아래 component 한 줄을 자기 뷰로 바꾸면 된다.
 *   component: () => import('@/views/trial/TrialHomeView.vue')
 */
const routes = [
    {
        path: '/',
        name: 'home',
        component: () => import('@/views/HomeView.vue'),
        meta: { title: '홈' },
    },
    {
        path: '/trial',
        name: 'trial',
        component: () => import('@/views/PlaceholderView.vue'),
        meta: { title: '재판' },
    },
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
];

/* 개발용 컴포넌트 카탈로그. import.meta.env.DEV 가 false 인 프로덕션 빌드에서는
 * 이 블록째로 제거돼 라우트도 청크도 생기지 않는다. */
if (import.meta.env.DEV) {
    routes.push({
        path: '/dev/ui',
        name: 'devUi',
        component: () => import('@/views/dev/UiCatalogView.vue'),
        meta: { title: '컴포넌트 카탈로그' },
    });
}

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
});

export default router;
