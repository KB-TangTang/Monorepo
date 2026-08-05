<script setup>
import { ref, onMounted } from 'vue';
import { RouterLink } from 'vue-router';
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import GroupTutorialOverlay from '@/components/challenge/group/GroupTutorialOverlay.vue';
import GroupJoinCodeSheet from '@/components/challenge/group/GroupJoinCodeSheet.vue';
import { hasSeenGroupTutorial, markGroupTutorialSeen } from '@/services/groupTutorialGuide';

const showTutorial = ref(false);
const showJoinSheet = ref(false);

onMounted(() => {
    if (!hasSeenGroupTutorial()) {
        showTutorial.value = true;
    }
});

function onTutorialComplete() {
    markGroupTutorialSeen();
}

function reopenTutorial() {
    showTutorial.value = true;
}
</script>

<template>
    <div class="gc-page">
        <!-- ===== Ink 헤더 ===== -->
        <header class="gc-header">
            <div class="gc-header-inner">
                <div class="gc-title-row">
                    <h1 class="gc-title">재판</h1>
                    <span class="gc-count-badge">진행 중 3</span>
                </div>
                <p class="gc-subtitle">
                    같이 정한 규칙,<br>각자의 소비로 지켜요
                </p>
            </div>
            <button
                type="button"
                class="gc-help-btn"
                aria-label="튜토리얼 다시보기"
                @click="reopenTutorial"
            >
                ?
            </button>
        </header>

        <!-- ===== 본문 ===== -->
        <main class="gc-body">
            <div class="gc-section-top">
                <span class="gc-section-title">진행 중인 재판</span>
                <span class="gc-view-all">전체보기 ›</span>
            </div>

            <!-- 샘플 챌린지 카드 -->
            <div class="gc-card">
                <div class="gc-card-top">
                    <span class="gc-card-badge">일일결산</span>
                    <span class="gc-card-code">2026-재판-0729</span>
                </div>
                <div class="gc-card-name">배달 소비 줄이기</div>
                <div class="gc-progress-track">
                    <div class="gc-progress-fill" style="width: 60%;" />
                </div>
            </div>
        </main>

        <!-- ===== 추가하기 FAB ===== -->
        <div class="gc-fab-area">
            <RouterLink :to="{ name: 'groupChallengeCreate' }" class="gc-fab">
                <span class="gc-fab__icon">+</span>
                <span class="gc-fab__label">추가하기</span>
            </RouterLink>
            <button type="button" class="gc-code-btn" @click="showJoinSheet = true">
                코드 입장
            </button>
        </div>

        <!-- ===== 개인/그룹 세그먼트 (팀 공용) ===== -->
        <ChallengeModeTabBar active-mode="group" />

        <!-- ===== 참여코드 입장 바텀시트 ===== -->
        <GroupJoinCodeSheet v-model="showJoinSheet" />

        <!-- ===== 튜토리얼 오버레이 ===== -->
        <GroupTutorialOverlay
            v-model="showTutorial"
            @complete="onTutorialComplete"
        />
    </div>
</template>

<style scoped>
.gc-page {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    padding-bottom: calc(var(--tt-tabbar-height) + var(--tt-space-10));
}

/* ── Ink 헤더 ─────────────────────────── */
.gc-header {
    background: var(--tt-surface-inverse);
    border-radius: 0 0 var(--tt-radius-2xl) var(--tt-radius-2xl);
    padding: var(--tt-space-12) var(--tt-screen-padding) var(--tt-space-6);
    position: relative;
}

.gc-header-inner {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.gc-title-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.gc-title {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
    letter-spacing: -0.01em;
}

.gc-count-badge {
    background: rgba(245, 185, 33, 0.16);
    color: var(--tt-accent);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.gc-subtitle {
    font-size: 12.5px;
    color: var(--tt-tab-inactive);
    line-height: 1.5;
}

/* 도움말 버튼 */
.gc-help-btn {
    position: absolute;
    top: var(--tt-space-12);
    right: var(--tt-screen-padding);
    width: 28px;
    height: 28px;
    border-radius: 50%;
    border: 1.5px solid rgba(255, 255, 255, 0.3);
    background: rgba(255, 255, 255, 0.08);
    color: var(--tt-text-inverse);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: background 0.15s ease;
}

.gc-help-btn:active {
    background: rgba(255, 255, 255, 0.2);
}

/* ── 본문 ─────────────────────────────── */
.gc-body {
    padding: var(--tt-space-4) var(--tt-screen-padding) 0;
}

.gc-section-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-section-title {
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-view-all {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-muted);
}

/* ── 챌린지 카드 ──────────────────────── */
.gc-card {
    margin-top: var(--tt-space-3);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 18px;
    box-shadow: var(--tt-elevation-2);
    padding: var(--tt-space-4);
}

.gc-card-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-card-badge {
    background: var(--tt-accent-subtle);
    color: var(--tt-accent-deep);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: var(--tt-space-1) var(--tt-space-3);
    border-radius: var(--tt-radius-full);
}

.gc-card-code {
    font-family: var(--tt-font-mono);
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-semibold);
}

.gc-card-name {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    margin-top: var(--tt-space-3);
    color: var(--tt-text);
}

.gc-progress-track {
    margin-top: var(--tt-space-3);
    height: 8px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
    overflow: hidden;
}

.gc-progress-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    background: var(--tt-accent);
}

/* ── 추가하기 FAB ────────────────────── */
.gc-fab-area {
    padding: var(--tt-space-4) var(--tt-screen-padding) 0;
}

.gc-fab {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: var(--tt-space-2);
    width: 100%;
    background: var(--tt-primary-gold);
    color: var(--tt-primary);
    font-weight: var(--tt-fw-black);
    font-size: var(--tt-fs-button);
    padding: 14px;
    border-radius: var(--tt-radius-lg);
    text-decoration: none;
    box-shadow: var(--tt-elevation-2);
    transition: filter 0.15s ease;
}

.gc-fab:active {
    filter: brightness(0.95);
}

.gc-fab__icon {
    font-size: 18px;
    font-weight: var(--tt-fw-black);
    line-height: 1;
}

.gc-code-btn {
    display: block;
    width: 100%;
    margin-top: var(--tt-space-2);
    background: transparent;
    border: 1.5px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    padding: 12px;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-body);
    cursor: pointer;
    font-family: inherit;
    transition: background 0.15s ease;
}

.gc-code-btn:active {
    background: var(--tt-bg-fill);
}

</style>
