<script setup>
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import ChallengePageHeader from '@/components/challenge/ChallengePageHeader.vue';
import GroupListSegment from '@/components/challenge/group/GroupListSegment.vue';
import GroupPreStartCard from '@/components/challenge/group/GroupPreStartCard.vue';
import GroupActiveCard from '@/components/challenge/group/GroupActiveCard.vue';
import GroupEndedCard from '@/components/challenge/group/GroupEndedCard.vue';
import GroupJoinCodeSheet from '@/components/challenge/group/GroupJoinCodeSheet.vue';
import {
    MOCK_PRE_START_CHALLENGES,
    MOCK_ACTIVE_LIST_CHALLENGES,
    MOCK_ENDED_CHALLENGES,
} from '@/fixtures/groupChallenge';
import mascotCheering from '@/assets/images/emotions/09_cheering.png';

const router = useRouter();
const activeTab = ref('active');
const showJoinSheet = ref(false);

/* ── 목데이터 (추후 API 교체) ──────────── */
const preStartList = ref(MOCK_PRE_START_CHALLENGES);
const activeList = ref(MOCK_ACTIVE_LIST_CHALLENGES);
const endedList = ref(MOCK_ENDED_CHALLENGES);

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
            <!-- 시작 전 -->
            <template v-if="activeTab === 'pre-start'">
                <GroupPreStartCard
                    v-for="ch in preStartList"
                    :key="ch.id"
                    :challenge="ch"
                    @invite="handleInvite"
                />
            </template>

            <!-- 진행 중 -->
            <template v-else-if="activeTab === 'active'">
                <GroupActiveCard
                    v-for="ch in activeList"
                    :key="ch.id"
                    :challenge="ch"
                />
            </template>

            <!-- 종료됨 -->
            <template v-else>
                <GroupEndedCard
                    v-for="ch in endedList"
                    :key="ch.id"
                    :challenge="ch"
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
