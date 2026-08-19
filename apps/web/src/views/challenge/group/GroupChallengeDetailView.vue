<!--
  그룹 챌린지 상세 — 시작 전(S5/S6) + 진행 중(S1/S2) 통합 뷰.
  status 와 evalType 에 따라 조건부로 다른 섹션을 렌더링한다.
  /group-challenges/:id 로 라우팅된다.
-->
<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { fetchGroupChallengeDetail } from '@/api/groupChallenge';

import ChallengePageHeader from '@/components/challenge/ChallengePageHeader.vue';
import GroupDetailInkCard from '@/components/challenge/group/GroupDetailInkCard.vue';
import GroupDetailLivesBar from '@/components/challenge/group/GroupDetailLivesBar.vue';
import GroupDetailPeriodBar from '@/components/challenge/group/GroupDetailPeriodBar.vue';
import GroupDetailPromise from '@/components/challenge/group/GroupDetailPromise.vue';
import GroupDetailMemberGrid from '@/components/challenge/group/GroupDetailMemberGrid.vue';
import GroupDetailMemberTable from '@/components/challenge/group/GroupDetailMemberTable.vue';
import GroupDetailTrialCarousel from '@/components/challenge/group/GroupDetailTrialCarousel.vue';
import GroupDetailPodium from '@/components/challenge/group/GroupDetailPodium.vue';
import GroupDetailRankingTable from '@/components/challenge/group/GroupDetailRankingTable.vue';

import { ChevronRightIcon } from '@heroicons/vue/24/solid';
import mascotChat from '@/assets/images/emotions/57_chat.png';
import { entryState, resolveBack } from '@/utils/groupChallengeNavigation';

const route = useRoute();
const router = useRouter();

const challenge = ref(null);
const loading = ref(true);

onMounted(async () => {
    try {
        challenge.value = await fetchGroupChallengeDetail(route.params.id);
    } catch (e) {
        /*
         * 이 catch 가 조용하면 화면이 「클릭이 씹힌 것」처럼 보인다 — replace 라 히스토리도 안 남아
         * 목록에서 카드를 눌러도 아무 일이 없는 것과 구분되지 않는다.
         * 실제로 이것 때문에 원인 추적이 오래 걸렸다(이슈 #271). 되돌리기 전에 반드시 남긴다.
         */
        console.error('[GroupChallengeDetail] 상세를 불러오지 못해 목록으로 되돌린다.', e);
        router.replace({ name: 'groupChallengeList' });
    } finally {
        loading.value = false;
    }
});

const ch = computed(() => challenge.value);
const isRecruiting = computed(() => ch.value?.status === 'RECRUITING');
const isActive = computed(() => ch.value?.status === 'ACTIVE');
const isClosed = computed(() => ch.value?.status === 'CLOSED');
const isDaily = computed(() => ch.value?.evalType === 'DAILY');

/* 종료 결과 텍스트 */
const outcomeLabel = computed(() => {
    if (!isClosed.value) return '';
    return ch.value.finalOutcome === 'SURVIVED' ? '승소' : '패소';
});

const outcomeStyle = computed(() => {
    if (!isClosed.value) return {};
    if (ch.value.finalOutcome === 'SURVIVED') {
        return { bg: 'var(--tt-green-soft)', color: 'var(--tt-green)' };
    }
    return { bg: 'var(--tt-red-soft)', color: 'var(--tt-red-deep)' };
});

/* 상태 뱃지 */
const statusBadges = computed(() => {
    if (!ch.value) return [];
    if (isRecruiting.value) {
        const dBadge = isDaily.value
            ? { bg: 'var(--tt-gold-soft)', color: 'var(--tt-gold-deep)' }
            : { bg: '#EAF0FF', color: '#3E63D6' };
        return [
            { label: '시작 전', bg: 'var(--tt-bg-fill)', color: 'var(--tt-text-body)' },
            { label: `D-${ch.value.daysUntilStart}`, ...dBadge },
        ];
    }
    if (isClosed.value) {
        return [
            { label: '종료됨', bg: 'var(--tt-bg-fill)', color: 'var(--tt-text-muted)' },
            { label: outcomeLabel.value, ...outcomeStyle.value },
        ];
    }
    return [
        { label: '진행 중', bg: 'var(--tt-green-soft)', color: 'var(--tt-green)' },
        {
            label: `DAY ${ch.value.currentDay} / ${ch.value.totalDays}`,
            bg: 'var(--tt-ink)',
            color: 'var(--tt-gold)',
        },
    ];
});

/* 기간 설명 */
const periodText = computed(() => {
    if (!ch.value) return '';
    const s = formatDate(ch.value.startDate);
    const e = formatDate(ch.value.endDate);
    if (isRecruiting.value) {
        return `${s} ~ ${e} · ${ch.value.totalDays}일간`;
    }
    if (isClosed.value) {
        return `${s} ~ ${e} · ${ch.value.totalDays}일간 완료`;
    }
    return `${s} ~ ${e} · ${ch.value.settleTime || '매일 23:30 결산'}`;
});

function formatDate(iso) {
    const d = new Date(iso);
    return `${d.getMonth() + 1}월 ${d.getDate()}일`;
}

/*
 * 네비게이션
 *
 * 뒤로가기는 **진입한 화면이 바로 앞에 있다고 확인된 경우에만** 히스토리를 되돌린다(이슈 #303).
 * 확인은 홈·목록이 push 할 때 남긴 history.state 로 한다 — 규칙은 utils/groupChallengeNavigation.
 *
 * 무조건 `router.back()` 으로 바꾸면 안 된다. 판결 플로우가 `replace` 로 이 화면에 돌아오므로
 * 그때 앞 항목은 방금 본 재판 화면이다(이슈 #172 가 홈 하드코딩을 넣은 이유). 그 경우는
 * 표시가 없어 종전대로 홈으로 나간다.
 */
function goBack() {
    const target = resolveBack(window.history.state, 'groupChallenge');

    if (target.type === 'back') {
        router.back();
        return;
    }
    router.push({ name: target.name });
}

function goToList() {
    router.push({ name: 'groupChallengeList' });
}

function goToRanking() {
    router.push({ name: 'groupChallengeRanking', params: { id: ch.value.id } });
}

function goToInvite() {
    router.push({
        name: 'groupChallengeInvite',
        params: { groupId: ch.value.id },
        /* 초대 화면의 뒤로가기가 홈이 아니라 이 상세로 돌아오게 한다(이슈 #303) */
        state: entryState('groupChallengeDetail'),
    });
}

function goToChat() {
    router.push({ name: 'groupChallengeChat', params: { id: ch.value.id } });
}

function goToDefense(item) {
    router.push({
        name: 'defenseViolation',
        params: { id: ch.value.id, indictmentId: item.id },
    });
}

function goToVote(item) {
    router.push({
        name: 'voteVerdict',
        params: { id: ch.value.id, indictmentId: item.id },
    });
}

function goToTrialProgress(item) {
    router.push({
        name: 'trialProgress',
        params: { id: ch.value.id, indictmentId: item.id },
    });
}

/*
 * 목록 카드와 같은 평평한 필드를 본다. 서버 DTO 에 중첩 chat 객체는 없다 —
 * 예전 목데이터가 쓰던 `chat.unreadCount` 를 보고 있어서 배지가 항상 0 이었다(이슈 #271).
 */
const unreadCount = computed(() => ch.value?.unreadChatCount ?? 0);
</script>

<template>
    <div v-if="loading" class="gc-detail gc-detail--loading">
        <div class="gc-detail__spinner"></div>
    </div>

    <div v-else-if="ch" class="gc-detail">
        <!-- ── 히어로 영역 (장식 원 + 헤더 + 상태) ── -->
        <div class="gc-detail__hero">
            <div class="gc-detail__deco gc-detail__deco--lg"></div>

            <!-- 내비게이션 헤더 -->
            <ChallengePageHeader title="그룹 챌린지" class="gc-detail__nav" @back="goBack">
                <template v-if="isClosed" #action>
                    <span class="gc-detail__nav-right" @click="goToRanking">최종 순위</span>
                </template>
            </ChallengePageHeader>

            <!-- 상태 뱃지 + 챌린지 이름 -->
            <div class="gc-detail__status">
                <div class="gc-detail__badges">
                    <span
                        v-for="(b, i) in statusBadges"
                        :key="i"
                        class="gc-detail__badge"
                        :style="{ background: b.bg, color: b.color }"
                    >
                        {{ b.label }}
                    </span>
                </div>
                <h2 class="gc-detail__name">{{ ch.groupName }}</h2>
                <p class="gc-detail__period">{{ periodText }}</p>
            </div>
        </div>

        <!-- ── 콘텐츠 영역 ── -->
        <div class="gc-detail__content">
            <!-- 종료: 시상대 + 결과 + 약속 + 순위 + 재판 기록 -->
            <template v-if="isClosed">
                <!-- 시상대 (3명 이상일 때) -->
                <GroupDetailPodium v-if="ch.finalMembers?.length >= 3" :members="ch.finalMembers" />

                <!-- 최종 결과 요약 -->
                <div class="gc-detail__result-card">
                    <div class="gc-detail__result-header">
                        <span class="gc-detail__result-title">최종 결과</span>
                        <span
                            class="gc-detail__result-badge"
                            :style="{
                                background: outcomeStyle.bg,
                                color: outcomeStyle.color,
                            }"
                        >
                            {{ outcomeLabel }}
                        </span>
                    </div>
                    <div class="gc-detail__result-stats">
                        <div class="gc-detail__result-stat">
                            <span class="gc-detail__result-label">최종 순위</span>
                            <span class="gc-detail__result-value">
                                {{ ch.finalRank }}위
                                <span class="gc-detail__result-total"
                                    >/ {{ ch.memberCount }}명</span
                                >
                            </span>
                        </div>
                        <div v-if="ch.savingsAmount" class="gc-detail__result-stat">
                            <span class="gc-detail__result-label">절약 금액</span>
                            <span class="gc-detail__result-value gc-detail__result-value--green">
                                {{ ch.savingsAmount.toLocaleString() }}원
                            </span>
                        </div>
                        <div v-if="ch.finalChargeAmount > 0" class="gc-detail__result-stat">
                            <span class="gc-detail__result-label">누적 혐의액</span>
                            <span class="gc-detail__result-value gc-detail__result-value--red">
                                {{ ch.finalChargeAmount.toLocaleString() }}원
                            </span>
                        </div>
                    </div>
                </div>

                <!-- 약속 아코디언 -->
                <GroupDetailPromise
                    v-if="ch.memo"
                    :memo="ch.memo"
                    :memo-author="ch.memoAuthor"
                    :memo-date="ch.memoDate"
                />

                <!-- 전체 피고인 현황 -->
                <GroupDetailRankingTable
                    v-if="ch.finalMembers"
                    :members="ch.finalMembers"
                    :eval-type="ch.evalType"
                    :max-lives="ch.maxLives"
                />

                <!-- 재판 기록 보기 -->
                <div
                    v-if="ch.trialStats && ch.trialStats.totalTrials > 0"
                    class="gc-detail__trial-link"
                    @click="goToRanking"
                >
                    <div class="gc-detail__trial-link-icon">
                        <svg viewBox="0 0 24 24" fill="currentColor" width="19" height="19">
                            <path
                                d="M17.29 5.71a1 1 0 0 0-1.41 0L13.5 8.09l-1.8-1.8a1 1 0 0 0-1.41 1.42l.38.38-5.3 5.3a1 1 0 0 0 0 1.41l2.83 2.83a1 1 0 0 0 1.41 0l5.3-5.3.38.38a1 1 0 0 0 1.42-1.42l-1.8-1.8 2.38-2.37a1 1 0 0 0 0-1.41ZM9.7 15.22 7.78 13.3l4.6-4.6 1.91 1.92ZM18 19H3v2h15Z"
                            />
                        </svg>
                    </div>
                    <div class="gc-detail__trial-link-body">
                        <span class="gc-detail__trial-link-title">재판 기록 보기</span>
                        <span class="gc-detail__trial-link-sub">
                            이 챌린지 재판 {{ ch.trialStats.totalTrials }}건 · 무죄
                            {{ ch.trialStats.innocentCount }} · 유죄 {{ ch.trialStats.guiltyCount }}
                        </span>
                    </div>
                    <ChevronRightIcon class="gc-detail__trial-link-arrow" />
                </div>
            </template>

            <!-- 시작 전 / 진행 중 -->
            <template v-else>
                <!-- Ink 스탯 카드 -->
                <GroupDetailInkCard
                    :eval-type="ch.evalType"
                    :status="ch.status"
                    :limit-amount="ch.limitAmount"
                    :category-name="ch.categoryName"
                    :current-amount="ch.myDailyAmount"
                    :usage-percent="ch.myUsagePercent"
                    :remaining-amount="ch.myRemainingAmount"
                />

                <!-- 목숨 바 (일일결산) / 기간 카드 (기간결산) -->
                <GroupDetailLivesBar
                    v-if="isDaily"
                    :lives-count="ch.livesCount || ch.maxLives"
                    :max-lives="ch.maxLives"
                    :mode="isRecruiting ? 'upcoming' : 'active'"
                    :size="isRecruiting ? 21 : 19"
                />
                <GroupDetailPeriodBar
                    v-else
                    :start-date="ch.startDate"
                    :end-date="ch.endDate"
                    :total-days="ch.totalDays"
                    :mode="isRecruiting ? 'upcoming' : 'active'"
                    :current-day="ch.currentDay || 0"
                />

                <!-- 재판 캐러셀 (일일결산 진행 중 + 기소 건이 있을 때) -->
                <GroupDetailTrialCarousel
                    v-if="isActive && isDaily && ch.indictments?.length"
                    :indictments="ch.indictments"
                    @defend="goToDefense"
                    @vote="goToVote"
                    @trial="goToTrialProgress"
                />

                <!-- 약속 아코디언 -->
                <GroupDetailPromise
                    :memo="ch.memo"
                    :memo-author="ch.memoAuthor"
                    :memo-date="ch.memoDate"
                    :show-subtitle="isRecruiting"
                />

                <!-- 시작 전: 참여 멤버 그리드 -->
                <GroupDetailMemberGrid
                    v-if="isRecruiting"
                    :members="ch.members"
                    :member-count="ch.memberCount"
                    :max-members="ch.maxMembers"
                    @invite="goToInvite"
                />

                <!-- 진행 중: 멤버 소비 상태 테이블 -->
                <GroupDetailMemberTable
                    v-if="isActive && ch.dailyMembers"
                    :members="ch.dailyMembers"
                    :eval-type="ch.evalType"
                />
            </template>
        </div>

        <!-- ── 하단 CTA ── -->
        <div class="gc-detail__footer">
            <div class="gc-detail__actions">
                <button
                    v-if="isActive"
                    class="gc-detail__btn gc-detail__btn--secondary"
                    @click="goToInvite"
                >
                    소환
                </button>
                <button
                    v-if="isActive"
                    class="gc-detail__btn gc-detail__btn--primary"
                    @click="goToRanking"
                >
                    재판 기록
                </button>
                <button
                    v-if="!isActive"
                    class="gc-detail__btn gc-detail__btn--outline"
                    @click="goToList"
                >
                    그룹 목록으로
                </button>
            </div>
        </div>

        <!-- 채팅 플로팅 버튼 (종료 제외) -->
        <div v-if="!isClosed" class="gc-detail__chat-fab" @click="goToChat">
            <div class="gc-detail__chat-fab-circle">
                <img :src="mascotChat" alt="채팅" class="gc-detail__chat-fab-img" />
            </div>
            <span
                class="gc-detail__chat-fab-badge"
                :class="{ 'gc-detail__chat-fab-badge--zero': !unreadCount }"
                >{{ unreadCount }}</span
            >
        </div>
    </div>
</template>

<style scoped>
.gc-detail {
    min-height: 100vh;
    background: var(--tt-bg-subtle);
    display: flex;
    flex-direction: column;
}

.gc-detail--loading {
    align-items: center;
    justify-content: center;
}

.gc-detail__spinner {
    width: 32px;
    height: 32px;
    border: 3px solid var(--tt-border);
    border-top-color: var(--tt-ink);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
}

@keyframes spin {
    to {
        transform: rotate(360deg);
    }
}

/* ── 히어로 영역 ── */
.gc-detail__hero {
    position: relative;
    flex: none;
    overflow: hidden;
    padding-bottom: 6px;
}

.gc-detail__deco {
    position: absolute;
    border-radius: 50%;
}

.gc-detail__deco--lg {
    top: -34px;
    right: -26px;
    width: 132px;
    height: 132px;
    background: #e9eefb;
}

/* ── 네비게이션 (ChallengePageHeader) ── */
.gc-detail__nav {
    padding: var(--tt-space-3) var(--tt-screen-padding) 0;
    position: relative;
    z-index: 2;
}

.gc-detail__nav-right {
    font-size: var(--tt-fs-caption);
    font-weight: var(--tt-fw-black);
    color: var(--tt-blue);
    cursor: pointer;
}

/* ── 채팅 플로팅 버튼 (좌측 하단 고정) ── */
.gc-detail__chat-fab {
    position: fixed;
    right: var(--tt-screen-padding);
    bottom: 90px;
    z-index: 10;
    cursor: pointer;
}

.gc-detail__chat-fab-circle {
    width: 52px;
    height: 52px;
    border-radius: 50%;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    box-shadow: 0 4px 12px rgba(35, 40, 66, 0.15);
    display: flex;
    align-items: center;
    justify-content: center;
}

.gc-detail__chat-fab-img {
    width: 34px;
    height: auto;
}

.gc-detail__chat-fab-badge {
    position: absolute;
    top: -3px;
    right: -5px;
    min-width: 18px;
    height: 18px;
    padding: 0 5px;
    border-radius: var(--tt-radius-full);
    background: var(--tt-red);
    color: var(--tt-white);
    font-size: 10px;
    font-weight: var(--tt-fw-black);
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
}

.gc-detail__chat-fab-badge--zero {
    background: var(--tt-border-strong);
}

/* ── 상태 + 제목 ── */
.gc-detail__status {
    padding: var(--tt-space-3) var(--tt-screen-padding) 0;
    position: relative;
    z-index: 2;
}

.gc-detail__badges {
    display: flex;
    gap: 6px;
}

.gc-detail__badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 3px 9px;
    border-radius: var(--tt-radius-full);
}

.gc-detail__name {
    font-size: var(--tt-fs-title);
    font-weight: var(--tt-fw-black);
    letter-spacing: -0.01em;
    margin-top: 9px;
    color: var(--tt-text);
}

.gc-detail__period {
    font-size: var(--tt-fs-caption);
    color: var(--tt-text-muted);
    margin-top: 4px;
}

/* ── 콘텐츠 ── */
.gc-detail__content {
    flex: 1;
    overflow-y: auto;
    padding: 6px var(--tt-screen-padding) 0;
    display: flex;
    flex-direction: column;
    gap: 7px;
}

/* ── 하단 ── */
.gc-detail__footer {
    flex: none;
    padding: 10px var(--tt-screen-padding) 24px;
    background: var(--tt-bg-subtle);
}

.gc-detail__actions {
    display: flex;
    gap: 10px;
}

.gc-detail__btn {
    padding: 14px;
    border-radius: var(--tt-radius-md);
    font-size: var(--tt-fs-label);
    font-weight: var(--tt-fw-black);
    text-align: center;
    border: none;
    cursor: pointer;
}

.gc-detail__btn--primary {
    flex: 1;
    background: var(--tt-ink);
    color: var(--tt-white);
}

.gc-detail__btn--outline {
    flex: 1;
    background: transparent;
    border: 1.5px solid var(--tt-ink);
    color: var(--tt-ink);
}

.gc-detail__btn--secondary {
    flex: none;
    width: 64px;
    background: var(--tt-bg);
    border: 1.5px solid var(--tt-border);
    color: var(--tt-text-body);
    font-size: var(--tt-fs-body);
}

/* ── 종료 결과 카드 ── */
.gc-detail__result-card {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: var(--tt-radius-xl);
    box-shadow: var(--tt-elevation-2);
    padding: 18px 16px;
}

.gc-detail__result-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-detail__result-title {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__result-badge {
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-black);
    padding: 4px 10px;
    border-radius: var(--tt-radius-full);
}

.gc-detail__result-stats {
    margin-top: 14px;
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.gc-detail__result-stat {
    display: flex;
    align-items: center;
    justify-content: space-between;
}

.gc-detail__result-label {
    font-size: var(--tt-fs-body);
    color: var(--tt-text-muted);
}

.gc-detail__result-value {
    font-size: var(--tt-fs-button);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__result-total {
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-medium);
    color: var(--tt-text-muted);
}

.gc-detail__result-value--green {
    color: var(--tt-green);
}

.gc-detail__result-value--red {
    color: var(--tt-red-deep);
}

/* ── 재판 기록 링크 카드 ── */
.gc-detail__trial-link {
    background: var(--tt-bg);
    border: 1px solid var(--tt-border);
    border-radius: 16px;
    padding: 12px 14px;
    display: flex;
    align-items: center;
    gap: 11px;
    cursor: pointer;
    box-shadow: var(--tt-elevation-2);
}

.gc-detail__trial-link-icon {
    width: 34px;
    height: 34px;
    border-radius: 11px;
    background: var(--tt-gold-soft);
    display: flex;
    align-items: center;
    justify-content: center;
    flex: none;
}

.gc-detail__trial-link-icon svg {
    color: var(--tt-gold-deep);
}

.gc-detail__trial-link-body {
    flex: 1;
    min-width: 0;
}

.gc-detail__trial-link-title {
    display: block;
    font-size: var(--tt-fs-body);
    font-weight: var(--tt-fw-black);
    color: var(--tt-text);
}

.gc-detail__trial-link-sub {
    display: block;
    font-size: var(--tt-fs-overline);
    font-weight: var(--tt-fw-bold);
    color: var(--tt-text-hint);
    margin-top: 2px;
}

.gc-detail__trial-link-arrow {
    width: 20px;
    height: 20px;
    color: var(--tt-border-strong);
    flex: none;
}
</style>
