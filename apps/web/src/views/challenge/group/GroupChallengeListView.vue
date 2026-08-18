<script setup>
import { ref, computed, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ChallengePageHeader from '@/components/challenge/ChallengePageHeader.vue';
import GroupListSegment from '@/components/challenge/group/GroupListSegment.vue';
import GroupPreStartCard from '@/components/challenge/group/GroupPreStartCard.vue';
import GroupActiveCard from '@/components/challenge/group/GroupActiveCard.vue';
import GroupEndedCard from '@/components/challenge/group/GroupEndedCard.vue';
import GroupJoinCodeSheet from '@/components/challenge/group/GroupJoinCodeSheet.vue';
import DevDataSourceFab from '@/components/dev/DevDataSourceFab.vue';
import DevBatchTriggerFab from '@/components/dev/DevBatchTriggerFab.vue';
import { fetchMyGroupChallenges } from '@/api/groupChallenge';
import { dataSource } from '@/services/devDataSource';
import mascotCheering from '@/assets/images/emotions/09_cheering.png';

const route = useRoute();
const router = useRouter();
const showJoinSheet = ref(false);

const preStartList = ref([]);
const activeList = ref([]);
const endedList = ref([]);
const loadError = ref('');

/*
 * 「종료됨」 탭은 JUDGING·CLOSED 두 상태를 함께 본다.
 * 판정이 끝나기를 기다리는 챌린지도 사용자에게는 이미 끝난 것이라서다.
 */
const STATUS_BY_TAB = {
    'pre-start': ['RECRUITING'],
    active: ['ACTIVE'],
    ended: ['JUDGING', 'CLOSED'],
};

/* 참여·생성 직후처럼 특정 탭을 열어야 하는 이동이 있다. 그때 ?tab= 으로 넘긴다. */
const activeTab = ref(
    Object.hasOwn(STATUS_BY_TAB, route.query.tab) ? route.query.tab : 'active',
);

const LIST_BY_TAB = {
    'pre-start': preStartList,
    active: activeList,
    ended: endedList,
};

async function loadAll() {
    loadError.value = '';
    try {
        const results = await Promise.all(
            Object.values(STATUS_BY_TAB).map((statuses) => fetchMyGroupChallenges(statuses)),
        );
        Object.keys(STATUS_BY_TAB).forEach((tab, i) => {
            LIST_BY_TAB[tab].value = results[i];
        });
    } catch (e) {
        loadError.value = e.message ?? '목록을 불러오지 못했습니다.';
    }
}

onMounted(loadAll);
watch(dataSource, loadAll);

const tabCounts = computed(() => ({
    'pre-start': preStartList.value.length,
    active: activeList.value.length,
    ended: endedList.value.length,
}));

function goBack() {
    router.push({ name: 'groupChallenge' });
}

function handleInvite(challenge) {
    router.push({ name: 'groupChallengeInvite', params: { groupId: challenge.id } });
}

function goToDetail(challenge) {
    router.push({ name: 'groupChallengeDetail', params: { id: challenge.id } });
}

/*
 * 카드 하단의 채팅 영역을 누르면 상세를 거치지 않고 바로 방으로 들어간다.
 * 상세 화면이 아직 실서버에 붙지 않아, 지금은 이 경로가 채팅방으로 가는 유일한 UI 다(이슈 #271).
 */
function goToChat(challenge) {
    router.push({ name: 'groupChallengeChat', params: { id: challenge.id } });
}
</script>

<template>
    <main class="gcl-page">
        <!-- ===== 스티키 헤더 ===== -->
        <header class="gcl-header">
            <ChallengePageHeader title="재판 전체보기" @back="goBack" />
            <div class="gcl-segment-wrap">
                <GroupListSegment v-model="activeTab" :counts="tabCounts" />
            </div>
        </header>

        <!-- ===== 본문 (스크롤 영역) ===== -->
        <section class="gcl-body">
            <p v-if="loadError" class="gcl-error">{{ loadError }}</p>

            <!-- 시작 전 -->
            <template v-if="activeTab === 'pre-start'">
                <GroupPreStartCard
                    v-for="ch in preStartList"
                    :key="ch.id"
                    :challenge="ch"
                    @invite="handleInvite"
                    @click="goToDetail(ch)"
                />
            </template>

            <!-- 진행 중 -->
            <template v-else-if="activeTab === 'active'">
                <GroupActiveCard
                    v-for="ch in activeList"
                    :key="ch.id"
                    :challenge="ch"
                    @click="goToDetail(ch)"
                    @open-chat="goToChat(ch)"
                />
            </template>

            <!-- 종료됨 -->
            <template v-else>
                <GroupEndedCard
                    v-for="ch in endedList"
                    :key="ch.id"
                    :challenge="ch"
                    @click="goToDetail(ch)"
                />
            </template>
        </section>

        <!-- ===== 시작 전 탭: 참여코드 CTA ===== -->
        <div v-if="activeTab === 'pre-start'" class="gcl-cta" @click="showJoinSheet = true">
            <img :src="mascotCheering" alt="탕이" class="gcl-cta__mascot" />
            <div class="gcl-cta__text">
                <span class="gcl-cta__title">참여코드가 있나요?</span>
                <span class="gcl-cta__desc">코드를 입력하면 바로 합류할 수 있어요</span>
            </div>
            <span class="gcl-cta__arrow">›</span>
        </div>

        <!-- ===== 참여코드 입장 바텀시트 ===== -->
        <GroupJoinCodeSheet v-model="showJoinSheet" />

        <DevDataSourceFab />

        <!--
          DEV: 시작 배치 즉시 실행. 세 탭이 한 화면에 있어 전이 결과(시작 전 → 진행 중)를
          바로 확인할 수 있는 자리라 홈보다 여기가 본거지다. 실행 후 세 목록을 함께 갱신한다.
        -->
        <DevBatchTriggerFab :offset="56" @done="loadAll" />
    </main>
</template>

<style scoped>
.gcl-page {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    padding-bottom: calc(var(--tt-tabbar-height) + var(--tt-space-10));
}

/* ── 스티키 헤더 ─────────────── */
.gcl-header {
    position: sticky;
    top: 0;
    z-index: var(--tt-z-sticky);
    background: var(--tt-bg);
    padding: var(--tt-space-3) var(--tt-screen-padding) 0;
    border-bottom: 1px solid var(--tt-border);
}

.gcl-segment-wrap {
    padding: var(--tt-space-3) 0 var(--tt-space-3);
}

/* ── 본문 ────────────────────── */
.gcl-body {
    padding: 14px var(--tt-screen-padding);
    display: flex;
    flex-direction: column;
    gap: var(--tt-space-3);
}

.gcl-body > * {
    cursor: pointer;
}

.gcl-error {
    padding: 10px 14px;
    background: var(--tt-danger-subtle);
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-danger-deep);
    text-align: center;
    line-height: 1.5;
    cursor: default;
}

/* ── 참여코드 CTA 배너 ────────── */
.gcl-cta {
    margin: var(--tt-space-1) var(--tt-screen-padding) 0;
    display: flex;
    align-items: center;
    gap: var(--tt-space-3);
    padding: 12px 16px;
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-lg);
    box-shadow: var(--tt-elevation-1);
    cursor: pointer;
    transition: box-shadow 0.15s ease;
}

.gcl-cta:active {
    box-shadow: var(--tt-elevation-3);
}

.gcl-cta__mascot {
    width: 44px;
    height: 44px;
    object-fit: contain;
    flex: none;
}

.gcl-cta__text {
    flex: 1;
    min-width: 0;
    display: flex;
    flex-direction: column;
    gap: 2px;
}

.gcl-cta__title {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gcl-cta__desc {
    font-size: var(--tt-fs-overline);
    color: var(--tt-text-muted);
}

.gcl-cta__arrow {
    font-size: var(--tt-fs-label);
    color: var(--tt-text-hint);
    flex: none;
}
</style>
