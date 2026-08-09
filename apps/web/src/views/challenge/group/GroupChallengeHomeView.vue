<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import GroupTutorialOverlay from '@/components/challenge/group/GroupTutorialOverlay.vue';
import GroupJoinCodeSheet from '@/components/challenge/group/GroupJoinCodeSheet.vue';
import GroupTrialEventCard from '@/components/challenge/group/GroupTrialEventCard.vue';
import GroupMascotScene from '@/components/challenge/group/GroupMascotScene.vue';
import GroupPeacefulCard from '@/components/challenge/group/GroupPeacefulCard.vue';
import { hasSeenGroupTutorial, markGroupTutorialSeen } from '@/services/groupTutorialGuide';
import {
    MOCK_TRIAL_SUMMARY,
    MOCK_INDICTMENT_SUMMARY,
    MOCK_ACTIVE_CHALLENGES,
} from '@/fixtures/groupChallenge';
import mascotSkeptical from '@/assets/images/emotions/25_skeptical.png';
import mascotJudging from '@/assets/images/emotions/48_judging.png';
import mascotSmile from '@/assets/images/emotions/03_gentle_smile.png';

const router = useRouter();
const showTutorial = ref(false);
const showJoinSheet = ref(false);

/* ── 목데이터 (추후 API 교체) ──────────── */
const trialSummary = ref(MOCK_TRIAL_SUMMARY);
const indictmentSummary = ref(MOCK_INDICTMENT_SUMMARY);
const activeChallenges = ref(MOCK_ACTIVE_CHALLENGES);

/* ── computed ──────────────────────────── */
const trialCount = computed(() => trialSummary.value?.count ?? 0);
const indictmentCount = computed(() => indictmentSummary.value?.count ?? 0);
const totalAlerts = computed(() => trialCount.value + indictmentCount.value);
const hasTrials = computed(() => trialCount.value > 0);
const hasIndictments = computed(() => indictmentCount.value > 0);
const hasBothTypes = computed(() => hasTrials.value && hasIndictments.value);
const hasAnyEvent = computed(() => hasTrials.value || hasIndictments.value);

/** 한쪽만 있을 때 보여줄 마스코트 씬 */
const mascotScene = computed(() => {
    if (hasBothTypes.value || !hasAnyEvent.value) return 'peaceful';
    if (hasIndictments.value) return 'indictment';
    return 'vote';
});

/** 헤더 상태 뱃지 */
const headerBadge = computed(() => {
    if (!hasAnyEvent.value) return { text: '평온', cls: 'gc-badge--green' };
    if (hasBothTypes.value) return { text: `알림 ${totalAlerts.value}`, cls: 'gc-badge--red' };
    if (hasIndictments.value) return { text: `기소 ${indictmentCount.value}`, cls: 'gc-badge--red' };
    return { text: `판결 ${trialCount.value}`, cls: 'gc-badge--gold' };
});

/** 헤더 서브타이틀 */
const headerSubtitle = computed(() => {
    if (!hasAnyEvent.value) return '오늘은 조용한 하루예요';
    if (hasBothTypes.value) return `처리할 일이 ${totalAlerts.value}건 있어요`;
    if (hasIndictments.value) return '기소에 응답할 차례예요';
    return '판결이 기다리고 있어요';
});

/** 헤더 마스코트 이미지 */
const headerMascot = computed(() => {
    if (!hasAnyEvent.value) return mascotSmile;
    if (hasIndictments.value) return mascotSkeptical;
    return mascotJudging;
});

const isDev = import.meta.env.DEV;

/* ── DEV: 상태 전환 테스트 ────────────── */
const STATE_PRESETS = [
    { label: '평온', indictment: 0, trial: 0 },
    { label: '기소만', indictment: 1, trial: 0 },
    { label: '투표만', indictment: 0, trial: 2 },
    { label: '기소+투표', indictment: 1, trial: 2 },
];
const devStateIndex = ref(3); // 기본: 기소+투표
const devStateLabel = computed(() => STATE_PRESETS[devStateIndex.value].label);

function cycleDevState() {
    devStateIndex.value = (devStateIndex.value + 1) % STATE_PRESETS.length;
    const preset = STATE_PRESETS[devStateIndex.value];
    indictmentSummary.value = { ...MOCK_INDICTMENT_SUMMARY, count: preset.indictment };
    trialSummary.value = { ...MOCK_TRIAL_SUMMARY, count: preset.trial };
}

/* ── 이벤트 핸들러 ─────────────────────── */
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

function onEventAction(type) {
    if (activeChallenges.value.length) {
        router.push({
            name: 'groupChallengeDetail',
            params: { id: activeChallenges.value[0].id },
        });
    }
}

function goToAllChallenges() {
    router.push({ name: 'groupChallengeList' });
}

function progressPercent(challenge) {
    if (!challenge.totalDays) return 0;
    return Math.round((challenge.currentDay / challenge.totalDays) * 100);
}

function livesColor(challenge) {
    const ratio = challenge.livesCount / challenge.maxLives;
    if (ratio >= 0.8) return 'var(--tt-green)';
    if (ratio >= 0.4) return 'var(--tt-gold-deep)';
    return 'var(--tt-red-deep)';
}
</script>

<template>
    <div class="gc-page">
        <!-- ===== Ink 헤더 ===== -->
        <header class="gc-header">
            <div class="gc-header-content">
                <div class="gc-header-text">
                    <div class="gc-title-row">
                        <h1 class="gc-title">그룹 챌린지</h1>
                        <span :class="['gc-badge', headerBadge.cls]">{{ headerBadge.text }}</span>
                    </div>
                    <p class="gc-subtitle">{{ headerSubtitle }}</p>
                </div>
                <div class="gc-header-mascot">
                    <img :src="headerMascot" alt="탕이" class="gc-header-mascot__img" />
                </div>
            </div>
            <div class="gc-header-actions">
                <button
                    v-if="isDev"
                    type="button"
                    class="gc-dev-btn"
                    @click="cycleDevState"
                >
                    {{ devStateLabel }}
                </button>
                <button
                    type="button"
                    class="gc-help-btn"
                    aria-label="튜토리얼 다시보기"
                    @click="reopenTutorial"
                >
                    ?
                </button>
            </div>
        </header>

        <!-- ===== 본문 (이벤트 카드 영역) ===== -->
        <main class="gc-body">
            <!-- 이벤트 카드 (기소·투표) 또는 평온 카드 -->
            <div class="gc-events">
                <template v-if="hasAnyEvent">
                    <GroupTrialEventCard
                        v-if="hasIndictments"
                        type="indictment"
                        :data="indictmentSummary"
                        @action="onEventAction"
                    />
                    <GroupTrialEventCard
                        v-if="hasTrials"
                        type="trial"
                        :data="trialSummary"
                        @action="onEventAction"
                    />
                </template>
                <GroupPeacefulCard v-else />
            </div>

            <!-- 마스코트 씬 (한쪽 타입만 있거나 평온일 때만) -->
            <GroupMascotScene
                v-if="!hasBothTypes"
                :scene="mascotScene"
            />

            <!-- 진행 중인 챌린지 -->
            <div class="gc-section">
                <div class="gc-section-top">
                    <span class="gc-section-title">진행 중인 챌린지</span>
                    <button type="button" class="gc-view-all" @click="goToAllChallenges">
                        전체보기 ›
                    </button>
                </div>

                <div
                    v-for="ch in activeChallenges"
                    :key="ch.id"
                    class="gc-challenge-card"
                >
                    <div class="gc-challenge-card__top">
                        <span class="gc-challenge-card__name">{{ ch.groupName }}</span>
                        <span class="gc-challenge-card__info">
                            {{ ch.evalType === 'DAILY' ? '일일결산' : '기간평가' }} · {{ ch.currentDay }}일차
                        </span>
                    </div>
                    <div class="gc-challenge-card__progress">
                        <div class="gc-progress-track">
                            <div
                                class="gc-progress-fill"
                                :style="{ width: progressPercent(ch) + '%' }"
                            />
                        </div>
                        <span
                            class="gc-challenge-card__lives"
                            :style="{ color: livesColor(ch) }"
                        >
                            {{ ch.livesCount }}/{{ ch.maxLives }}
                        </span>
                    </div>
                </div>
            </div>
        </main>

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
    padding: var(--tt-space-12) var(--tt-screen-padding) 44px;
    position: relative;
}

.gc-header-content {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
}

.gc-header-text {
    max-width: 238px;
    padding-top: var(--tt-space-2);
}

.gc-title-row {
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

.gc-title {
    font-size: 24px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
    letter-spacing: -0.01em;
}

/* 상태 뱃지 */
.gc-badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}
.gc-badge--green {
    background: rgba(46, 158, 107, 0.22);
    color: #7FD9AE;
}
.gc-badge--gold {
    background: rgba(245, 185, 33, 0.16);
    color: var(--tt-gold);
}
.gc-badge--red {
    background: rgba(224, 102, 75, 0.22);
    color: #FF9E86;
}

.gc-subtitle {
    font-size: 12.5px;
    color: var(--tt-tab-inactive);
    line-height: 1.5;
    margin-top: 6px;
}

/* 헤더 마스코트 */
.gc-header-mascot {
    width: 88px;
    height: 88px;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    flex: none;
}

.gc-header-mascot__img {
    width: 64px;
    height: 64px;
    object-fit: contain;
    filter: drop-shadow(0 8px 12px rgba(0, 0, 0, 0.28));
}

/* 헤더 우측 버튼 그룹 */
.gc-header-actions {
    position: absolute;
    top: var(--tt-space-12);
    right: var(--tt-screen-padding);
    display: flex;
    align-items: center;
    gap: var(--tt-space-2);
}

/* DEV 상태 전환 버튼 */
.gc-dev-btn {
    height: 28px;
    padding: 0 10px;
    border-radius: var(--tt-radius-full);
    border: 1.5px solid rgba(245, 185, 33, 0.5);
    background: rgba(245, 185, 33, 0.15);
    color: var(--tt-gold);
    font-size: 11px;
    font-weight: var(--tt-fw-black);
    font-family: inherit;
    cursor: pointer;
    white-space: nowrap;
    transition: background 0.15s ease;
}
.gc-dev-btn:active {
    background: rgba(245, 185, 33, 0.3);
}

/* 도움말 버튼 */
.gc-help-btn {
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

/* ── 본문 ──────────────────────────────── */
.gc-body {
    padding: 0 var(--tt-screen-padding);
    margin-top: -30px;
    position: relative;
    z-index: 3;
}

/* ── 이벤트 카드 목록 ──────────────────── */
.gc-events {
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

/* ── 진행 중인 챌린지 섹션 ─────────────── */
.gc-section {
    margin-top: var(--tt-space-4);
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
    background: none;
    border: none;
    cursor: pointer;
    font-family: inherit;
    padding: 0;
}

/* ── 챌린지 카드 ──────────────────────── */
.gc-challenge-card {
    margin-top: var(--tt-space-3);
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-2);
    padding: 14px 16px;
}

.gc-challenge-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-challenge-card__name {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-challenge-card__info {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-hint);
    font-weight: var(--tt-fw-bold);
}

.gc-challenge-card__progress {
    margin-top: 10px;
    display: flex;
    align-items: center;
    gap: 9px;
}

.gc-progress-track {
    flex: 1;
    height: 8px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-border-track);
    overflow: hidden;
}

.gc-progress-fill {
    height: 100%;
    border-radius: var(--tt-radius-full);
    background: var(--tt-gold);
}

.gc-challenge-card__lives {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
}
</style>
