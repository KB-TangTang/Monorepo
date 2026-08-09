<!--
  용도: 앱 하단 고정 5탭(재판 · 자산 · 홈 · 자료실 · 마이). 라우트 이동과 활성 표시를 담당한다.
  언제 쓰는지: 앱 셸(App.vue)에서 한 번만 렌더한다. 화면마다 따로 두지 않는다.
  쓰면 안 되는 경우: 로그인 · 온보딩 등 탭 이동이 없는 화면. 그런 라우트에서는 App.vue 에서 숨긴다.
-->
<script setup>
import { useRoute } from 'vue-router';

/* 탭 정의는 이 배열 하나뿐이다. 라우트를 바꾸면 router/index.js 와 함께 고친다. */
const TABS = [
    {
        name: 'personalMissionChallenge',
        label: '재판',
        to: '/mission/personal',
        paths: ['M12 4v16', 'M7.5 20h9', 'M4.5 8h15', 'M4.5 8 2 14h5z', 'M19.5 8 17 14h5z'],
    },
    {
        name: 'asset',
        label: '자산',
        to: '/asset',
        paths: [
            'M3.5 8.5A2.5 2.5 0 0 1 6 6h11.5A1.5 1.5 0 0 1 19 7.5V9',
            'M3.5 8.5v8A2.5 2.5 0 0 0 6 19h12a1.5 1.5 0 0 0 1.5-1.5v-7A1.5 1.5 0 0 0 18 9H6',
            'M20 12.5h-3a1.5 1.5 0 0 0 0 3h3',
        ],
    },
    {
        name: 'home',
        label: '홈',
        to: '/',
        paths: ['M3.5 11 12 4l8.5 7', 'M5.8 9.6V19a1 1 0 0 0 1 1h10.4a1 1 0 0 0 1-1V9.6'],
    },
    {
        name: 'ledger',
        label: '자료실',
        to: '/reports/monthly',
        paths: [
            'M6 3.5h11a1 1 0 0 1 1 1v15a1 1 0 0 1-1 1H6a2 2 0 0 1-2-2v-13a2 2 0 0 1 2-2z',
            'M8 8.5h6',
            'M8 12h6',
            'M8 15.5h4',
        ],
    },
    {
        name: 'my',
        label: '마이',
        to: '/my',
        paths: ['M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8z', 'M4.5 20a7.5 7.5 0 0 1 15 0'],
    },
];

const route = useRoute();

/*
 * 홈(/)만 정확 매칭. 나머지는 하위 경로에서도 활성으로 본다.
 * /ledger 는 자산 홈의 토글로만 들어가는 화면이라 "자산" 탭을 활성으로 유지한다.
 */
function isActive(tab) {
    if (tab.name === 'ledger' && route.path.startsWith('/reports')) {
        return true;
    }
    if (tab.name === 'asset' && route.path.startsWith('/ledger')) {
        return true;
    }
    return tab.to === '/' ? route.path === '/' : route.path.startsWith(tab.to);
}
</script>

<template>
    <nav class="tt-tabbar" aria-label="주요 메뉴">
        <RouterLink
            v-for="tab in TABS"
            :key="tab.name"
            class="tt-tabbar__item"
            :class="{ 'tt-tabbar__item--active': isActive(tab) }"
            :to="tab.to"
            :aria-current="isActive(tab) ? 'page' : undefined"
        >
            <svg
                class="tt-tabbar__icon"
                viewBox="0 0 24 24"
                fill="none"
                stroke="currentColor"
                stroke-width="1.75"
                stroke-linecap="round"
                stroke-linejoin="round"
                aria-hidden="true"
            >
                <path v-for="(d, i) in tab.paths" :key="i" :d="d" />
            </svg>
            <span class="tt-tabbar__label">{{ tab.label }}</span>
        </RouterLink>
    </nav>
</template>

<style scoped>
.tt-tabbar {
    position: fixed;
    right: 0;
    bottom: 0;
    left: 0;
    z-index: var(--tt-z-tabbar);
    display: flex;
    width: 100%;
    max-width: var(--tt-content-max);
    margin: 0 auto;
    padding-bottom: env(safe-area-inset-bottom);
    background: var(--tt-bg);
    border-top: 1px solid var(--tt-border);
}

.tt-tabbar__item {
    display: flex;
    flex: 1;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    gap: 3px;
    height: var(--tt-tabbar-height);
    font-family: var(--tt-font-sans);
    font-size: var(--tt-fs-mono-chip);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-tab-inactive);
    text-decoration: none;
    transition: color 0.15s ease;
}

.tt-tabbar__icon {
    width: 24px;
    height: 24px;
}

/* 활성 탭 — 활성 판정은 스크립트의 isActive() 한 곳에서만 한다 */
.tt-tabbar__item--active {
    color: var(--tt-tab-active);
}

.tt-tabbar__item--active .tt-tabbar__icon {
    stroke-width: 2.1;
}

.tt-tabbar__label {
    line-height: 1;
}
</style>
