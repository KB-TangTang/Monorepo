<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import GroupTutorialOverlay from '@/components/challenge/group/GroupTutorialOverlay.vue';
import GroupJoinCodeSheet from '@/components/challenge/group/GroupJoinCodeSheet.vue';
import GroupTodoCard from '@/components/challenge/group/GroupTodoCard.vue';
import GroupTodoDoneCard from '@/components/challenge/group/GroupTodoDoneCard.vue';
import GroupTodoSheet from '@/components/challenge/group/GroupTodoSheet.vue';
import { hasSeenGroupTutorial, markGroupTutorialSeen } from '@/services/groupTutorialGuide';
import { useCountdown } from '@/utils/useCountdown';
import {
    MOCK_TODO_ITEMS,
    MOCK_ACTIVE_CHALLENGES,
} from '@/fixtures/groupChallenge';
import { BellIcon } from '@heroicons/vue/24/outline';
import courtDistrictImg from '@/assets/images/court/court_district.png';

const router = useRouter();

/* ── 날짜 ──────────────────────────────── */
const todayLabel = computed(() => {
    const d = new Date();
    const dayNames = ['일', '월', '화', '수', '목', '금', '토'];
    const yyyy = d.getFullYear();
    const mm = String(d.getMonth() + 1).padStart(2, '0');
    const dd = String(d.getDate()).padStart(2, '0');
    return `${yyyy}. ${mm}. ${dd} (${dayNames[d.getDay()]})`;
});

/* ── 목데이터 (추후 API 교체) ──────────── */
const doneIds = ref([]);
const activeChallenges = ref(MOCK_ACTIVE_CHALLENGES);

const todoItems = computed(() =>
    MOCK_TODO_ITEMS
        .filter(i => !doneIds.value.includes(i.id))
        .sort((a, b) => a.deadlineMinutes - b.deadlineMinutes)
);

const hasTodo = computed(() => todoItems.value.length > 0);
const allDone = computed(() => todoItems.value.length === 0 && doneIds.value.length > 0);

/* ── 카운트다운 ────────────────────────── */
const { countdowns } = useCountdown(todoItems);

/* ── 바텀시트 ──────────────────────────── */
const showSheet = ref(false);

/* ── 토스트 ────────────────────────────── */
const toast = ref(null);
let toastTimer = null;

function flash(msg) {
    clearTimeout(toastTimer);
    toast.value = msg;
    toastTimer = setTimeout(() => { toast.value = null; }, 1800);
}

/* ── 알림 빨간 점 ─────────────────────── */
const hasNotificationDot = computed(() => hasTodo.value);

/* ── 튜토리얼 ─────────────────────────── */
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

/* ── DEV 상태 전환 ─────────────────────── */
const isDev = import.meta.env.DEV;
const DEV_STATES = ['할 일 있음', '전부 완료', '초기화'];
const devStateIndex = ref(0);
const devStateLabel = computed(() => DEV_STATES[devStateIndex.value]);

function cycleDevState() {
    devStateIndex.value = (devStateIndex.value + 1) % DEV_STATES.length;
    if (devStateIndex.value === 1) {
        doneIds.value = MOCK_TODO_ITEMS.map(i => i.id);
    } else if (devStateIndex.value === 2) {
        doneIds.value = [];
        devStateIndex.value = 0;
    }
}

/* ── 이벤트 핸들러 ─────────────────────── */
function onOpenTodo(item) {
    const msg = item.type === 'accuse' ? '변론 화면으로 이동했어요' : '투표 화면으로 이동했어요';
    flash(msg);
    doneIds.value = [...doneIds.value, item.id];
    /* 남은 건이 없으면 시트 자동 닫기 */
    if (todoItems.value.length === 0) {
        showSheet.value = false;
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
        <!-- ===== 다크 헤더 (법원 현판) ===== -->
        <header class="gc-header">
            <div class="gc-header__bg" />
            <div class="gc-header__glow" />

            <!-- 상단 바: DEV 버튼 + 알림 아이콘 -->
            <div class="gc-header__topbar">
                <button
                    v-if="isDev"
                    type="button"
                    class="gc-dev-btn"
                    @click="cycleDevState"
                >
                    {{ devStateLabel }}
                </button>
                <div style="flex:1" />
                <div class="gc-header__bell">
                    <BellIcon class="gc-header__bell-icon" />
                    <span v-if="hasNotificationDot" class="gc-header__bell-dot" />
                </div>
            </div>

            <!-- 현판 이미지 + 날짜 칩 -->
            <div class="gc-header__court">
                <img :src="courtDistrictImg" alt="탕탕 지방법원" class="gc-header__court-img" />
                <div class="gc-header__date">{{ todayLabel }}</div>
            </div>
        </header>

        <!-- ===== 본문 ===== -->
        <main class="gc-body">
            <!-- TO-DO 인박스 또는 완료 카드 -->
            <GroupTodoCard
                v-if="hasTodo"
                :items="todoItems"
                :countdowns="countdowns"
                @open="onOpenTodo"
                @open-sheet="showSheet = true"
            />
            <GroupTodoDoneCard v-else-if="allDone" />

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

        <!-- ===== 토스트 ===== -->
        <Transition name="tt-toast">
            <div v-if="toast" class="gc-toast">
                <span class="gc-toast__text">{{ toast }}</span>
            </div>
        </Transition>

        <!-- ===== 개인/그룹 세그먼트 (팀 공용) ===== -->
        <ChallengeModeTabBar active-mode="group" />

        <!-- ===== TO-DO 바텀시트 ===== -->
        <GroupTodoSheet
            v-model="showSheet"
            :items="todoItems"
            :countdowns="countdowns"
            @open="onOpenTodo"
        />

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

/* ── 다크 헤더 ─────────────────────────── */
.gc-header {
    background: var(--tt-surface-inverse);
    border-radius: 0 0 30px 30px;
    padding: 0 20px 14px;
    position: relative;
    overflow: hidden;
}

.gc-header__bg {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background: linear-gradient(180deg, #1E2338 0%, #232842 60%, #283050 100%);
}
.gc-header__glow {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background: radial-gradient(200px 130px at 50% 60px, rgba(245, 185, 33, 0.1), transparent 70%);
}

/* 상단 바 */
.gc-header__topbar {
    height: 42px;
    display: flex;
    align-items: center;
    position: relative;
    z-index: 2;
    padding-top: env(safe-area-inset-top);
}

/* DEV 버튼 */
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

/* 알림 아이콘 */
.gc-header__bell {
    width: 36px;
    height: 36px;
    border-radius: 12px;
    background: rgba(255, 255, 255, 0.1);
    display: flex;
    align-items: center;
    justify-content: center;
    position: relative;
    cursor: pointer;
}
.gc-header__bell-icon {
    width: 21px;
    height: 21px;
    color: var(--tt-text-inverse);
}
.gc-header__bell-dot {
    position: absolute;
    top: 7px;
    right: 8px;
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--tt-red);
    border: 1.5px solid var(--tt-surface-inverse);
}

/* 현판 + 날짜 */
.gc-header__court {
    position: relative;
    z-index: 2;
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 6px;
}
.gc-header__court-img {
    width: 150px;
    height: auto;
    object-fit: contain;
    filter: drop-shadow(0 10px 18px rgba(0, 0, 0, 0.35));
}
.gc-header__date {
    margin-top: 6px;
    background: rgba(245, 185, 33, 0.15);
    border: 1px solid rgba(245, 185, 33, 0.3);
    border-radius: var(--tt-radius-full);
    padding: 3px 12px;
    font-size: 10.5px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-gold);
    font-family: ui-monospace, Menlo, monospace;
    letter-spacing: 0.06em;
}

/* ── 본문 ──────────────────────────────── */
.gc-body {
    padding: 10px 22px 0;
    position: relative;
    z-index: 3;
}

/* ── 진행 중인 챌린지 섹션 ─────────────── */
.gc-section {
    margin-top: 10px;
}

.gc-section-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-section-title {
    font-size: 15px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-view-all {
    font-size: 12px;
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
    margin-top: 11px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 18px;
    box-shadow: 0 8px 22px rgba(35, 40, 66, 0.05);
    padding: 14px 16px;
}

.gc-challenge-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-challenge-card__name {
    font-size: 14px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-challenge-card__info {
    font-size: 11px;
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
    background: var(--tt-accent);
}

.gc-challenge-card__lives {
    font-size: 12px;
    font-weight: var(--tt-fw-black);
}

/* ── 토스트 ────────────────────────────── */
.gc-toast {
    position: fixed;
    left: 22px;
    right: 22px;
    bottom: calc(var(--tt-tabbar-height) + 80px);
    z-index: var(--tt-z-toast, 50);
    display: flex;
    justify-content: center;
    pointer-events: none;
}
.gc-toast__text {
    background: rgba(35, 40, 66, 0.94);
    color: var(--tt-text-inverse);
    font-size: 12px;
    font-weight: var(--tt-fw-bold);
    padding: 10px 16px;
    border-radius: var(--tt-radius-full);
    box-shadow: 0 12px 26px rgba(21, 24, 40, 0.32);
}

.tt-toast-enter-active {
    animation: tt-toastin 0.22s ease-out both;
}
.tt-toast-leave-active {
    animation: tt-toastin 0.18s ease-in reverse both;
}

@keyframes tt-toastin {
    0% { transform: translateY(10px); opacity: 0; }
    100% { transform: none; opacity: 1; }
}
</style>
