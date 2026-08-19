<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import ChallengeModeTabBar from '@/components/challenge/ChallengeModeTabBar.vue';
import GroupTutorialOverlay from '@/components/challenge/group/GroupTutorialOverlay.vue';
import GroupJoinCodeSheet from '@/components/challenge/group/GroupJoinCodeSheet.vue';
import GroupTodoCard from '@/components/challenge/group/GroupTodoCard.vue';
import GroupTodoDoneCard from '@/components/challenge/group/GroupTodoDoneCard.vue';
import GroupPeacefulCard from '@/components/challenge/group/GroupPeacefulCard.vue';
import GroupMascotScene from '@/components/challenge/group/GroupMascotScene.vue';
import GroupTodoSheet from '@/components/challenge/group/GroupTodoSheet.vue';
import DevDataSourceFab from '@/components/dev/DevDataSourceFab.vue';
import DevBatchTriggerFab from '@/components/dev/DevBatchTriggerFab.vue';
import { hasSeenGroupTutorial, markGroupTutorialSeen } from '@/services/tutorialGuide';
import { fetchMyGroupChallenges, fetchMyTrials } from '@/api/groupChallenge';
import { dataSource } from '@/services/devDataSource';
import { useCountdown } from '@/utils/useCountdown';
import { ArrowPathIcon } from '@heroicons/vue/24/outline';
import TheNotificationBell from '@/components/common/TheNotificationBell.vue';
import courtDistrictImg from '@/assets/images/court/court_district.png';
import judgeImg from '@/assets/images/emotions/48_judging.png';
import { entryState } from '@/utils/groupChallengeNavigation';

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

/* ── 진행 중인 챌린지 ──────────────────── */
const activeChallenges = ref([]);

async function loadActiveChallenges() {
    try {
        activeChallenges.value = await fetchMyGroupChallenges(['ACTIVE']);
    } catch {
        /* 위젯 하나 때문에 홈 전체를 막지 않는다. 비어 있으면 판사 탕이가
         * "새로운 챌린지에 참여해보세요" 로 안내한다. */
        activeChallenges.value = [];
    }
}

/* ── TO-DO (내가 변론·투표해야 하는 재판) ── */
const myTrials = ref([]);

/* 처리한 건을 화면에서만 지운다. DEV 토글이 「전부 완료」를 흉내낼 때도 쓴다. */
const doneIds = ref([]);

async function loadMyTrials() {
    try {
        /* 서버가 마감 임박순으로 정렬해 준다. 여기서 다시 정렬하면 기준이 갈린다. */
        myTrials.value = await fetchMyTrials();
    } catch {
        /* 위젯 하나 때문에 홈 전체를 막지 않는다. 비면 판사 탕이가 다른 말을 한다. */
        myTrials.value = [];
    }
}

const todoItems = computed(() => myTrials.value.filter((item) => !doneIds.value.includes(item.id)));

const hasTodo = computed(() => todoItems.value.length > 0);
const allDone = computed(() => todoItems.value.length === 0 && doneIds.value.length > 0);

/* ── 카운트다운 ────────────────────────── */
const { countdowns } = useCountdown(todoItems);

/* ── 바텀시트 ──────────────────────────── */
const showSheet = ref(false);
const todoSheetRef = ref(null);

/* ── 토스트 ────────────────────────────── */
const toast = ref(null);
let toastTimer = null;

function flash(msg) {
    clearTimeout(toastTimer);
    toast.value = msg;
    toastTimer = setTimeout(() => {
        toast.value = null;
    }, 1800);
}

/* ── 판사 탕이 말풍선 ─────────────────── */
const judgeQuote = computed(() => {
    const todoCount = todoItems.value.length;
    const activeCount = activeChallenges.value.length;
    if (todoCount > 0) return `밀린 할 일이 ${todoCount}건 있어요,\n서두르세요!`;
    if (allDone.value) return '모든 할 일을 처리했군요,\n훌륭해요!';
    if (activeCount > 0) return `진행 중인 챌린지가\n${activeCount}건 있어요`;
    return '새로운 챌린지에\n참여해보세요!';
});

/* ── 튜토리얼 ─────────────────────────── */
const showTutorial = ref(false);
const showJoinSheet = ref(false);

onMounted(() => {
    if (!hasSeenGroupTutorial()) {
        showTutorial.value = true;
    }
    loadActiveChallenges();
    loadMyTrials();
});

watch(dataSource, () => {
    loadActiveChallenges();
    loadMyTrials();
});

/*
 * 완료 저장은 서버(tbl_user.group_tutorial_seen_at)로 나간다 — 비동기다.
 * 저장 실패는 markGroupTutorialSeen() 안에서 삼킨다(다음에 한 번 더 뜨는 게 전부).
 */
async function onTutorialComplete() {
    await markGroupTutorialSeen();
}

/* ── DEV 상태 전환 ─────────────────────── */
const isDev = import.meta.env.DEV;
const DEV_STATES = ['할 일 있음', '전부 완료', '초기화'];
const devStateIndex = ref(0);
const devStateLabel = computed(() => DEV_STATES[devStateIndex.value]);

function cycleDevState() {
    devStateIndex.value = (devStateIndex.value + 1) % DEV_STATES.length;
    if (devStateIndex.value === 1) {
        doneIds.value = myTrials.value.map((i) => i.id);
    } else if (devStateIndex.value === 2) {
        doneIds.value = [];
        devStateIndex.value = 0;
    }
}

/* ── 이벤트 핸들러 ─────────────────────── */
function onOpenTodo(item) {
    if (item.type === 'accuse') {
        /* 바텀시트 안에서 이동할 때는 시트의 history 항목을 먼저 양도한 뒤
         * router.replace 를 써야 한다. 그렇지 않으면 시트가 닫히면서
         * history.back() 이 라우터 이동을 되감는다 (useOverlay 주석 참고). */
        if (showSheet.value) {
            todoSheetRef.value?.releaseHistory?.();
            showSheet.value = false;
        }
        router.replace({
            name: 'defenseViolation',
            params: { id: item.challengeId, indictmentId: item.indictmentId },
        });
        return;
    }
    /* vote — 투표 플로우 */
    if (item.type === 'vote') {
        if (showSheet.value) {
            todoSheetRef.value?.releaseHistory?.();
            showSheet.value = false;
        }
        router.replace({
            name: 'voteVerdict',
            params: { id: item.challengeId, indictmentId: item.indictmentId },
        });
        return;
    }
}

function goToAllChallenges() {
    router.push({ name: 'groupChallengeList' });
}

/*
 * 그룹챌린지 생성 진입로. 만들기 화면(`/group-challenges/create`)은 라우트만 있고
 * 홈·목록 어느 쪽에도 버튼이 없어 주소를 직접 쳐야만 갈 수 있었다(이슈 #172).
 */
function goToCreate() {
    router.push({ name: 'groupChallengeCreate' });
}

/*
 * 홈의 진행 중 카드에는 클릭이 아예 없었다. 목록의 GroupActiveCard 와 달리 그 자리에서 만든
 * 별도 마크업이라 진입로가 함께 붙지 않았다 — 눌러도 반응이 없어 「전체보기 ›」로 목록까지
 * 들어가야만 상세에 갈 수 있었다.
 *
 * 목록 카드처럼 채팅 영역을 따로 두지는 않는다. 홈 카드에는 대화 미리보기가 없어 나눌 자리가 없다.
 */
function goToDetail(challenge) {
    router.push({
        name: 'groupChallengeDetail',
        params: { id: challenge.id },
        /* 상세가 스크롤 위치까지 되살리며 여기로 돌아오게 한다(이슈 #303) */
        state: entryState('groupChallenge'),
    });
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

            <div class="gc-header__inner">
                <!-- 상단 바: 알림 아이콘 -->
                <div class="gc-header__topbar">
                    <TheNotificationBell />
                </div>

                <!-- 현판 이미지 + 날짜 칩 -->
                <div class="gc-header__court">
                    <img :src="courtDistrictImg" alt="탕탕 지방법원" class="gc-header__court-img" />
                    <div class="gc-header__date">{{ todayLabel }}</div>
                </div>

                <!-- 판사 탕이 + 말풍선 -->
                <div class="gc-header__judge">
                    <div class="gc-header__judge-wrap">
                        <img :src="judgeImg" alt="판사 탕이" class="gc-header__judge-img" />
                        <div class="gc-header__nameplate">판사 탕이</div>
                    </div>
                    <div class="gc-header__speech">
                        <div class="gc-header__speech-arrow" />
                        <div class="gc-header__speech-text">{{ judgeQuote }}</div>
                    </div>
                </div>
            </div>
        </header>

        <!-- ===== 본문 ===== -->
        <main class="gc-body">
            <!-- TO-DO 인박스 / 방금 다 처리함 / 애초에 할 일이 없음 -->
            <GroupTodoCard
                v-if="hasTodo"
                :items="todoItems"
                :countdowns="countdowns"
                @open="onOpenTodo"
                @open-sheet="showSheet = true"
            />
            <GroupTodoDoneCard v-else-if="allDone" />
            <!-- 기소·투표가 아예 없는 평온 상태. 이 분기가 없으면 자리 전체가 빈 화면이 된다.
                 doneIds 는 DEV 토글로만 채워지므로 allDone 은 실사용에서 거의 오지 않는다. -->
            <template v-else>
                <GroupPeacefulCard />
                <GroupMascotScene scene="peaceful" />
            </template>

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
                    @click="goToDetail(ch)"
                >
                    <div class="gc-challenge-card__top">
                        <span class="gc-challenge-card__name">{{ ch.groupName }}</span>
                        <span class="gc-challenge-card__info">
                            {{ ch.evalType === 'DAILY' ? '일일결산' : '기간평가' }} ·
                            {{ ch.currentDay }}일차
                        </span>
                    </div>
                    <div class="gc-challenge-card__progress">
                        <div class="gc-progress-track">
                            <div
                                class="gc-progress-fill"
                                :style="{ width: progressPercent(ch) + '%' }"
                            />
                        </div>
                        <span class="gc-challenge-card__lives" :style="{ color: livesColor(ch) }">
                            {{ ch.livesCount }}/{{ ch.maxLives }}
                        </span>
                    </div>
                </div>

                <!-- 생성 진입로. 만들기 화면으로 가는 유일한 버튼이다 -->
                <button type="button" class="gc-create-cta" @click="goToCreate">
                    <span class="gc-create-cta__title">새 그룹챌린지 만들기</span>
                    <span class="gc-create-cta__plus">+</span>
                </button>

                <!-- 참여코드 진입로. 목록 「시작 전」 탭까지 들어가지 않아도 코드를 넣을 수 있다 -->
                <button type="button" class="gc-join-cta" @click="showJoinSheet = true">
                    <span class="gc-join-cta__title">참여코드가 있나요?</span>
                    <span class="gc-join-cta__arrow">›</span>
                </button>
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
            ref="todoSheetRef"
            v-model="showSheet"
            :items="todoItems"
            :countdowns="countdowns"
            @open="onOpenTodo"
        />

        <!-- ===== 참여코드 입장 바텀시트 ===== -->
        <GroupJoinCodeSheet v-model="showJoinSheet" />

        <!-- ===== 튜토리얼 오버레이 ===== -->
        <GroupTutorialOverlay v-model="showTutorial" @complete="onTutorialComplete" />

        <!-- DEV: 데이터 출처 전환 -->
        <DevDataSourceFab />

        <!-- DEV: TO-DO 상태 전환 (데이터 출처 버튼 위에 쌓는다) -->
        <button v-if="isDev" type="button" class="gc-dev-fab" @click="cycleDevState">
            <ArrowPathIcon class="gc-dev-fab__icon" />
            {{ devStateLabel }}
        </button>

        <!-- DEV: 시작 상태 전이 배치 즉시 실행 (자정을 기다리지 않기 위한 문) -->
        <DevBatchTriggerFab :offset="96" @done="loadActiveChallenges" />
    </div>
</template>

<style scoped>
.gc-page {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    padding-bottom: calc(var(--tt-tabbar-height) + var(--tt-space-10));
}

.gc-dev-fab {
    position: fixed;
    right: 16px;
    bottom: calc(var(--tt-tabbar-height) + 56px); /* DevDataSourceFab 위에 쌓는다 */
    height: 32px;
    padding: 0 12px;
    border-radius: var(--tt-radius-full);
    border: 1.5px solid rgba(245, 185, 33, 0.5);
    background: rgba(35, 40, 66, 0.9);
    color: var(--tt-gold);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    font-family: inherit;
    cursor: pointer;
    z-index: var(--tt-z-tabbar);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
    display: flex;
    align-items: center;
    gap: 4px;
}

.gc-dev-fab__icon {
    width: 14px;
    height: 14px;
}

/* ── 다크 헤더 ─────────────────────────── */
.gc-header {
    background: var(--tt-surface-inverse);
    border-radius: 0 0 var(--tt-radius-2xl) var(--tt-radius-2xl);
    padding-bottom: var(--tt-space-5);
    position: relative;
    overflow: hidden;
}
.gc-header__inner {
    position: relative;
    z-index: 2;
    padding: 0 var(--tt-screen-padding);
}

.gc-header__bg {
    position: absolute;
    inset: 0;
    pointer-events: none;
    background: linear-gradient(180deg, #1e2338 0%, #232842 60%, #283050 100%);
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
    justify-content: flex-end;
    padding-top: calc(env(safe-area-inset-top) + var(--tt-space-2));
}

/* TheNotificationBell 다크 헤더 오버라이드 */
.gc-header__topbar :deep(.tt-bell) {
    width: 36px;
    height: 36px;
    padding: 0;
    border-radius: var(--tt-radius-sm);
    background: rgba(255, 255, 255, 0.1);
    color: var(--tt-text-inverse);
    display: flex;
    align-items: center;
    justify-content: center;
}
.gc-header__topbar :deep(.tt-bell svg) {
    width: 21px;
    height: 21px;
}
.gc-header__topbar :deep(.tt-bell__badge) {
    top: -4px;
    right: -4px;
    min-width: 16px;
    font-size: 10px;
    line-height: 16px;
}

/* 현판 + 날짜 */
.gc-header__court {
    display: flex;
    flex-direction: column;
    align-items: center;
}
.gc-header__court-img {
    height: 120px;
    width: auto;
    object-fit: contain;
    filter: drop-shadow(0 10px 18px rgba(0, 0, 0, 0.35));
}
.gc-header__date {
    margin-top: var(--tt-space-2);
    background: rgba(245, 185, 33, 0.15);
    border: 1px solid rgba(245, 185, 33, 0.3);
    border-radius: var(--tt-radius-full);
    padding: 3px var(--tt-space-3);
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    color: var(--tt-accent-bright);
    font-family: var(--tt-font-mono);
    letter-spacing: 0.06em;
}

/* 판사 탕이 + 말풍선 */
.gc-header__judge {
    margin-top: var(--tt-space-3);
    display: flex;
    align-items: center;
    gap: 3%;
    padding: 0 var(--tt-space-3);
}
.gc-header__judge-wrap {
    width: 23%;
    flex: none;
    display: flex;
    flex-direction: column;
    align-items: center;
}
.gc-header__judge-img {
    width: 100%;
    aspect-ratio: 1;
    object-fit: contain;
    animation: gc-float 3.4s ease-in-out infinite;
}
.gc-header__nameplate {
    margin-top: var(--tt-space-1);
    background: var(--tt-kraft);
    border: 1.5px solid var(--tt-wood);
    border-radius: 6px;
    padding: 2px 8px;
    font-size: 9px;
    font-weight: var(--tt-fw-black);
    color: var(--tt-wood);
    white-space: nowrap;
    letter-spacing: 0.03em;
    box-shadow: 0 2px 6px rgba(156, 123, 84, 0.15);
}
@keyframes gc-float {
    0%,
    100% {
        transform: translateY(0);
    }
    50% {
        transform: translateY(-5px);
    }
}
.gc-header__speech {
    flex: 1;
    min-width: 0;
    background: var(--tt-white);
    border-radius: var(--tt-space-4);
    padding: 13px 14px;
    box-shadow: 0 12px 26px -12px rgba(0, 0, 0, 0.45);
    position: relative;
}
.gc-header__speech-arrow {
    position: absolute;
    left: -10px;
    top: calc(50% - 7px);
    width: 0;
    height: 0;
    border-bottom: 14px solid var(--tt-white);
    border-left: 11px solid transparent;
}
.gc-header__speech-text {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
    letter-spacing: -0.01em;
    line-height: 1.45;
    word-break: keep-all;
    white-space: pre-line;
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
    margin-top: 11px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 18px;
    box-shadow: 0 8px 22px rgba(35, 40, 66, 0.05);
    padding: 14px 16px;
    cursor: pointer;
    transition: box-shadow 0.15s ease;
}

/* 누를 수 있다는 신호. 목록의 카드도 같은 방식으로 눌린 느낌을 준다 */
.gc-challenge-card:active {
    box-shadow: 0 4px 12px rgba(35, 40, 66, 0.12);
}

.gc-challenge-card__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-challenge-card__name {
    font-size: var(--tt-fs-body);
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
    background: var(--tt-accent);
}

.gc-challenge-card__lives {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
}

/* ── 생성 진입로 ──────────────────────── */
.gc-create-cta {
    width: 100%;
    margin-top: 11px;
    padding: 14px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: var(--tt-primary);
    border: none;
    border-radius: 18px;
    cursor: pointer;
    font-family: inherit;
    box-shadow: var(--tt-elevation-btn);
}

.gc-create-cta:active {
    background: var(--tt-primary-hover);
}

.gc-create-cta__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
}

.gc-create-cta__plus {
    font-size: var(--tt-fs-subtitle);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text-inverse);
    line-height: 1;
}

/* ── 참여코드 진입로 ──────────────────── */
.gc-join-cta {
    width: 100%;
    margin-top: 11px;
    padding: 14px 16px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: var(--tt-bg);
    border: 1.5px dashed var(--tt-border-strong);
    border-radius: 18px;
    cursor: pointer;
    font-family: inherit;
}

.gc-join-cta__title {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-join-cta__arrow {
    font-size: var(--tt-fs-subtitle);
    color: var(--tt-text-hint);
    line-height: 1;
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
    font-size: var(--tt-fs-caption);
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
    0% {
        transform: translateY(10px);
        opacity: 0;
    }
    100% {
        transform: none;
        opacity: 1;
    }
}

@media (max-width: 390px) {
    .gc-header__court-img {
        height: 100px;
    }
    .gc-header__speech-text {
        font-size: var(--tt-fs-label);
    }
}
</style>
